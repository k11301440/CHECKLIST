package com.example.checklist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TASK_ARCHITECTURE_AND_DEBUG_SPEC.txt の [4] 既知バグ と [7] 検証観点 に対応する回帰テスト。
 */
class TaskSyncTest {

    private val settings = AppSettings(carryoverUnfinishedReq = true, carryoverUnfinishedNorm = false)

    private fun fourPhases(vararg itemsPerPhase: List<ChecklistItem>): List<Phase> {
        val names = listOf("朝", "午前", "午後", "夜")
        return names.mapIndexed { i, n -> Phase(id = "p$i", name = n, items = itemsPerPhase.getOrElse(i) { emptyList() }) }
    }

    /** completePhase と同等の持越処理 */
    private fun carry(phases: List<Phase>, from: Int): List<Phase> {
        val carried = TaskSync.selectCarryover(phases[from].items, phases[from].name, settings)
        return phases.toMutableList().also {
            it[from + 1] = it[from + 1].copy(items = TaskSync.mergeCarryover(it[from + 1].items, carried))
        }
    }

    private fun state(phases: List<Phase>, date: String = "2026-10-19") =
        AppState(currentDate = date, phases = phases, settings = settings)

    private fun allInstances(s: AppState, id: String) = s.phases.flatMap { it.items }.filter { it.id == id }

    // [4-1] / [7-1] 多段キャリーオーバー後、途中フェーズで完了 → 全インスタンス同期
    @Test
    fun multiHopCarryover_completeInMiddle_syncsAll() {
        val a = ChecklistItem(id = "A", title = "TASK_A", priority = Priority.REQUIRED)
        var phases = fourPhases(listOf(a))
        phases = carry(phases, 0); phases = carry(phases, 1); phases = carry(phases, 2)
        assertEquals(4, allInstances(state(phases), "A").size)

        val completed = TaskSync.reconcile(state(TaskSync.updateLinked(phases, "A") { it.copy(isCompleted = true, completedAt = 1L) }))
        assertTrue(allInstances(completed, "A").all { it.isCompleted })

        // 多段でも発生元フェーズ名は「朝」のまま
        assertTrue(phases.drop(1).all { p -> p.items.single().sourcePhaseName == "朝" })
    }

    @Test
    fun multiHopCarryover_deleteInMiddle_syncsAll() {
        val a = ChecklistItem(id = "A", title = "TASK_A", priority = Priority.REQUIRED)
        var phases = fourPhases(listOf(a))
        phases = carry(phases, 0); phases = carry(phases, 1); phases = carry(phases, 2)
        val deleted = TaskSync.reconcile(state(TaskSync.updateLinked(phases, "A") { it.copy(isDeleted = true) }))
        assertTrue(allInstances(deleted, "A").all { it.isDeleted })
    }

    // [4-2] 完了後にフェーズ完了処理が再評価されても未完了で上書きされない
    @Test
    fun completePhaseRerun_doesNotResurrectCompletedTask() {
        val a = ChecklistItem(id = "A", title = "TASK_A", priority = Priority.REQUIRED)
        var phases = carry(fourPhases(listOf(a)), 0)
        // 午前側で完了
        phases = TaskSync.updateLinked(phases, "A") { it.copy(isCompleted = true) }
        // 朝フェーズの完了処理を再実行 (元フェーズは完了済みなので持越対象なし / 冪等)
        phases = carry(phases, 0)
        val s = TaskSync.reconcile(state(phases))
        assertEquals(1, s.phases[1].items.size)
        assertTrue(allInstances(s, "A").all { it.isCompleted })
    }

    // 完了解除は全インスタンスで戻り、OR 合成で再完了しない
    @Test
    fun undo_propagatesAndIsNotRevertedByReconcile() {
        val a = ChecklistItem(id = "A", title = "TASK_A", priority = Priority.REQUIRED)
        var phases = carry(fourPhases(listOf(a)), 0)
        phases = TaskSync.updateLinked(phases, "A") { it.copy(isCompleted = true) }
        phases = TaskSync.updateLinked(phases, "A") { it.copy(isCompleted = false, completedAt = null) }
        val s = TaskSync.reconcile(state(phases))
        assertTrue(allInstances(s, "A").none { it.isCompleted })
    }

    // [7-3] 同名の独立タスクは同期されない
    @Test
    fun sameTitleIndependentTasks_areNotLinked() {
        val morning = ChecklistItem(id = "M", title = "メール確認")
        val afternoon = ChecklistItem(id = "N", title = "メール確認")
        val phases = fourPhases(listOf(morning), emptyList(), listOf(afternoon))
        val s = TaskSync.reconcile(state(TaskSync.updateLinked(phases, "M") { it.copy(isCompleted = true) }))
        assertTrue(s.phases[0].items.single().isCompleted)
        assertFalse(s.phases[2].items.single().isCompleted)
    }

    // [7-3] 持越コピーと同名の独立タスクも連動しない
    @Test
    fun sameTitleIndependentTask_notLinkedToCarryoverCopy() {
        val a = ChecklistItem(id = "A", title = "メール確認", priority = Priority.REQUIRED)
        val other = ChecklistItem(id = "B", title = "メール確認")
        var phases = fourPhases(listOf(a), emptyList(), listOf(other))
        phases = carry(phases, 0)
        val s = TaskSync.reconcile(state(TaskSync.updateLinked(phases, "B") { it.copy(isCompleted = true) }))
        assertTrue(allInstances(s, "A").none { it.isCompleted })
        assertTrue(allInstances(s, "B").all { it.isCompleted })
    }

    // 旧実装では同一フェーズ内の同名タスクが整合エンジンで黙って削除されていた
    @Test
    fun reconcile_keepsSameTitleItemsInSamePhase_butDedupsSameId() {
        val x1 = ChecklistItem(id = "X1", title = "水を飲む")
        val x2 = ChecklistItem(id = "X2", title = "水を飲む")
        val phases = fourPhases(listOf(x1, x2, x1))
        val s = TaskSync.reconcile(state(phases))
        assertEquals(listOf("X1", "X2"), s.phases[0].items.map { it.id })
    }

    // 次フェーズに同名の「完了済み」タスクがあっても持越はスキップされない
    @Test
    fun mergeCarryover_completedSameTitleDoesNotBlock() {
        val done = ChecklistItem(id = "D", title = "報告", isCompleted = true)
        val carried = ChecklistItem(id = "C", title = "報告", priority = Priority.REQUIRED, isCarryover = true)
        val merged = TaskSync.mergeCarryover(listOf(done), listOf(carried))
        assertEquals(listOf("C", "D"), merged.map { it.id })
        // 未完了の同名タスクがあればスキップ (仕様 5-3)
        val active = done.copy(isCompleted = false)
        assertEquals(listOf("D"), TaskSync.mergeCarryover(listOf(active), listOf(carried)).map { it.id })
    }

    // [7-2] 翌日: 前日の持越コピーがルーティンに残らず、ID 重複や二重持越が起きない
    @Test
    fun nextDay_carryoverCopiesDoNotBecomeRoutine() {
        val a = ChecklistItem(id = "A", title = "TASK_A", priority = Priority.REQUIRED)
        var phases = fourPhases(listOf(a))
        phases = carry(phases, 0); phases = carry(phases, 1)
        val current = state(phases)

        val carryover = TaskSync.collectDayCarryover(current.phases, settings)
        assertEquals(1, carryover.size)

        val next = TaskSync.reconcile(TaskSync.buildNextDayState(current, current.phases, carryover, current.currentDate))
        assertEquals("2026-10-20", next.currentDate)
        // 朝に1件のみ、午前・午後のコピーは消える
        assertEquals(listOf("A"), next.phases[0].items.map { it.id })
        assertTrue(next.phases[1].items.isEmpty())
        assertTrue(next.phases[2].items.isEmpty())
        assertFalse(next.phases[0].items.single().isCarryover)
    }

    @Test
    fun nextDay_dailyRecordDoesNotDoubleCountCarryoverCopies() {
        val a = ChecklistItem(id = "A", title = "TASK_A", priority = Priority.REQUIRED)
        var phases = fourPhases(listOf(a))
        phases = carry(phases, 0); phases = carry(phases, 1)
        val record = TaskSync.createDailyRecord("2026-10-19", phases, now = 0L)
        assertEquals(1, record.totalItems)
        assertEquals(1, record.requiredPendingItems)
    }

    // 未来日付タスクは当日表示されず、指定日に投入される
    @Test
    fun scheduledTask_releasedOnlyOnItsDate() {
        val tomorrowTask = ChecklistItem(id = "T", title = "明日の会議準備", scheduledDate = "2026-10-20", originPhaseIndex = 1)
        val s0 = state(fourPhases()).copy(scheduledItems = listOf(ScheduledTask("p1", tomorrowTask)))

        val sameDay = TaskSync.releaseDueScheduled(s0, "2026-10-19")
        assertTrue(sameDay.phases.all { it.items.isEmpty() })

        val next = TaskSync.buildNextDayState(s0, s0.phases, emptyList(), "2026-10-19")
        assertEquals(listOf("T"), next.phases[1].items.map { it.id })
        assertTrue(next.scheduledItems.isEmpty())
    }

    @Test
    fun legacyFutureItemsInPhase_areMovedToReservation() {
        val future = ChecklistItem(id = "F", title = "未来", scheduledDate = "2026-10-21")
        val todayItem = ChecklistItem(id = "N", title = "今日")
        val s = TaskSync.extractFutureScheduled(state(fourPhases(listOf(future, todayItem))))
        assertEquals(listOf("N"), s.phases[0].items.map { it.id })
        assertEquals(listOf("F"), s.scheduledItems.map { it.item.id })
        assertEquals("p0", s.scheduledItems.single().phaseId)
    }

    // [7-1] + [7-2] 複数日 × 複数フェーズの連続持越でも増殖せず、完了時は翌日に復活しない
    @Test
    fun multiDayMultiPhaseCarryover_doesNotDuplicateOrResurrect() {
        // Day 1: 午後(index=2)で重要タスク作成 → 未完了のまま Day 2 へ
        val pmTask = ChecklistItem(id = "PM", title = "午後タスク", priority = Priority.REQUIRED, originPhaseIndex = 2)
        val day1 = state(fourPhases(emptyList(), emptyList(), listOf(pmTask)), date = "2026-10-19")
        val day1Carry = TaskSync.collectDayCarryover(day1.phases, settings)
        var day2 = TaskSync.reconcile(TaskSync.buildNextDayState(day1, day1.phases, day1Carry, day1.currentDate))

        // Day 2 開始時点: 朝(index=0)にのみ存在し、午後(index=2)には重複して残らない
        assertEquals(listOf("PM"), day2.phases[0].items.map { it.id })
        assertTrue(day2.phases[0].items.single().isCarryover)
        assertEquals("午後", day2.phases[0].items.single().sourcePhaseName)
        assertTrue(day2.phases[2].items.isEmpty())

        // Day 2: 朝 → 午前 → 午後 → 夜 と全フェーズに持越（すべて isCarryover = true の状態）
        var day2Phases = day2.phases
        day2Phases = carry(day2Phases, 0)
        day2Phases = carry(day2Phases, 1)
        day2Phases = carry(day2Phases, 2)
        day2 = day2.copy(phases = day2Phases)

        // 未完了のまま Day 3 へ進んだ場合: Day 3 の朝に1件だけ引き継がれ、午前・午後・夜には残らない
        val day2Carry = TaskSync.collectDayCarryover(day2.phases, settings)
        val day3 = TaskSync.reconcile(TaskSync.buildNextDayState(day2, day2.phases, day2Carry, day2.currentDate))
        assertEquals(listOf("PM"), day3.phases[0].items.map { it.id })
        assertTrue(day3.phases.drop(1).all { it.items.isEmpty() })

        // Day 3 で完了にして Day 4 へ進んだ場合: 完了済みの持越タスクは Day 4 に復活しない
        val day3CompletedPhases = TaskSync.updateLinked(day3.phases, "PM") { it.copy(isCompleted = true, completedAt = 100L) }
        val day3Completed = day3.copy(phases = day3CompletedPhases)
        val day3Carry = TaskSync.collectDayCarryover(day3Completed.phases, settings)
        assertTrue(day3Carry.isEmpty())
        val day4 = TaskSync.reconcile(TaskSync.buildNextDayState(day3Completed, day3Completed.phases, day3Carry, day3Completed.currentDate))
        assertTrue(day4.phases.all { it.items.isEmpty() })
    }

    // [7-5] 日別レコードと振り返りログの上限ガベージコレクション検証
    @Test
    fun buildNextDayState_prunesOldRecordsAndLogs() {
        val records = (1..(TaskSync.MAX_DAILY_RECORDS + 10)).associate { i ->
            val d = String.format("2025-%02d-%02d", ((i - 1) / 28) + 1, ((i - 1) % 28) + 1)
            d to DailyRecord(dateString = d)
        }
        val logs = (1..(TaskSync.MAX_REVIEW_LOGS + 20)).map { i ->
            ReviewLog(id = "L$i", phaseName = "朝", hadIssue = false)
        }
        val s0 = state(fourPhases(), date = "2026-10-19").copy(dailyRecords = records, reviewLogs = logs)
        val next = TaskSync.buildNextDayState(s0, s0.phases, emptyList(), "2026-10-19", logs)
        assertEquals(TaskSync.MAX_DAILY_RECORDS, next.dailyRecords.size)
        assertTrue(next.dailyRecords.containsKey("2026-10-19"))
        assertEquals(TaskSync.MAX_REVIEW_LOGS, next.reviewLogs.size)
    }
}
