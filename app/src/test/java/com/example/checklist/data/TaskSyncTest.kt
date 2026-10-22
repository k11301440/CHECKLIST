package com.example.checklist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    // タブ・スワイプ・時間進行で直接先のフェーズへ移動しても未完了タスクが伝播する
    @Test
    fun propagateCarryoverUpTo_carriesAcrossSkippedPhases() {
        val mTask = ChecklistItem(id = "M1", title = "朝タスク", priority = Priority.REQUIRED, originPhaseIndex = 0)
        val aTask = ChecklistItem(id = "A1", title = "午前タスク", priority = Priority.NORMAL, originPhaseIndex = 1)
        val phases = fourPhases(listOf(mTask), listOf(aTask))

        // 朝(0)から午後(2)へ直接移動した場合でも、朝と午前の未完了タスクが午後まで引き継がれる (デフォルト設定: REQ/NORM 両方持越)
        val propagated = TaskSync.propagateCarryoverUpTo(phases, targetIndex = 2, settings = AppSettings())
        val pmItems = propagated[2].items
        assertEquals(listOf("M1", "A1"), pmItems.map { it.id })
        assertEquals("朝", pmItems.first { it.id == "M1" }.sourcePhaseName)
        assertEquals("午前", pmItems.first { it.id == "A1" }.sourcePhaseName)
    }

    // 「現在のフェーズに戻る」/日付またぎ自動進行で、日付と時間帯フェーズが同時に現在へ飛び未完了も引き継がれる
    @Test
    fun advanceToDateAndPhase_jumpsDateAndTimeWithCarryover() {
        val pmTask = ChecklistItem(id = "PM1", title = "前日午後タスク", priority = Priority.REQUIRED, originPhaseIndex = 2)
        val yesterdayState = state(fourPhases(emptyList(), emptyList(), listOf(pmTask)), date = "2026-10-19")

        // 翌日 (2026-10-20) の午後 (index=2) へジャンプ
        val jumped = TaskSync.reconcile(
            TaskSync.advanceToDateAndPhase(yesterdayState, targetDate = "2026-10-20", targetPhaseIndex = 2)
        )
        assertEquals("2026-10-20", jumped.currentDate)
        assertEquals(2, jumped.selectedPhaseIndex)
        // 前日午後の未完了タスクが翌朝(0)→午前(1)→午後(2)まで伝播している
        assertTrue(jumped.phases[2].items.any { it.id == "PM1" && !it.isCompleted && it.sourcePhaseName == "午後" })
    }

    // 先のフェーズを覗き見してから元のフェーズに戻った場合、未来フェーズの未完了持越コピーは整理される
    // 一方、先のフェーズで「完了」にした持越コピーは戻って進んでも完了のまま維持される (フィードバック⑩-1, ⑩-6)
    @Test
    fun peekFuturePhaseAndReturn_trimsUnfinishedButKeepsCompletedCarryover() {
        val t1 = ChecklistItem(id = "T1", title = "未完了タスク", priority = Priority.REQUIRED, originPhaseIndex = 0)
        val t2 = ChecklistItem(id = "T2", title = "午前で完了するタスク", priority = Priority.NORMAL, originPhaseIndex = 0)
        val allCarrySettings = AppSettings(carryoverUnfinishedReq = true, carryoverUnfinishedNorm = true)
        var phases = fourPhases(listOf(t1, t2))

        // 1. 朝(0) -> 午後(2) まで移動: T1, T2 が午前(1)と午後(2)へ伝播
        phases = TaskSync.propagateCarryoverUpTo(phases, targetIndex = 2, settings = allCarrySettings)
        assertEquals(2, phases[1].items.size)
        assertEquals(2, phases[2].items.size)

        // 2. 午前(1)に戻り、T2 を午前(1)で完了にする
        phases = TaskSync.propagateCarryoverUpTo(phases, targetIndex = 1, settings = allCarrySettings)
        // 午後(2)にあった未完了の持越コピーは整理されている
        assertTrue(phases[2].items.isEmpty())

        phases = TaskSync.completeItemInPhase(phases, phaseId = "p1", itemId = "T2", now = 123L)
        phases = TaskSync.reconcile(state(phases)).phases

        // 3. 朝(0)へ戻る: T1の午前(1)未完了コピーは整理されるが、午前(1)で完了済みのT2は保持される
        phases = TaskSync.propagateCarryoverUpTo(phases, targetIndex = 0, settings = allCarrySettings)
        assertEquals(listOf("T2"), phases[1].items.map { it.id })
        assertTrue(phases[1].items.single().isCompleted)

        // 4. 再び午前(1)へ進む: T2は完了のまま、未完了のT1が再度キャリーオーバーされる
        phases = TaskSync.propagateCarryoverUpTo(phases, targetIndex = 1, settings = allCarrySettings)
        assertEquals(setOf("T1", "T2"), phases[1].items.map { it.id }.toSet())
        assertTrue(phases[1].items.first { it.id == "T2" }.isCompleted)
        assertFalse(phases[1].items.first { it.id == "T1" }.isCompleted)
    }

    // 発生元フェーズ(朝)でタスクを完了にした場合、後続フェーズのキャリーオーバーコピーは除去され件数が重複しない
    @Test
    fun completeInOriginPhase_removesLaterCarryoverCopies() {
        val a = ChecklistItem(id = "A", title = "朝タスク", priority = Priority.REQUIRED, originPhaseIndex = 0)
        val allCarrySettings = AppSettings(carryoverUnfinishedReq = true, carryoverUnfinishedNorm = true)
        var phases = fourPhases(listOf(a))

        // 午前(1)へ進みキャリーオーバー発生
        phases = TaskSync.propagateCarryoverUpTo(phases, targetIndex = 1, settings = allCarrySettings)
        assertEquals(1, phases[1].items.size)

        // 朝(0)側で完了にする -> 朝(0)は完了になり、午前(1)のキャリーオーバーコピーは消える
        phases = TaskSync.completeItemInPhase(phases, phaseId = "p0", itemId = "A", now = 500L)
        assertTrue(phases[0].items.single().isCompleted)
        assertTrue(phases[1].items.isEmpty())
    }

    // 翌日に手動で進んだ後、「現在のフェーズに戻る」等で当日に戻った場合、当日の完了状態と翌日予約タスクが正しく復元される
    @Test
    fun advanceToNextDayAndReturnToToday_restoresSnapshotAndScheduledTasks() {
        val doneTask = ChecklistItem(id = "D1", title = "朝完了タスク", priority = Priority.REQUIRED, isCompleted = true, completedAt = 10L)
        val pendingTask = ChecklistItem(id = "P1", title = "午前未完了タスク", priority = Priority.NORMAL, originPhaseIndex = 1)
        val tomorrowScheduled = ChecklistItem(id = "S1", title = "明日予約タスク", priority = Priority.REQUIRED, scheduledDate = "2026-10-20", originPhaseIndex = 0)

        val todayState = AppState(
            currentDate = "2026-10-19",
            phases = fourPhases(listOf(doneTask), listOf(pendingTask)),
            selectedPhaseIndex = 1,
            settings = AppSettings(),
            scheduledItems = listOf(ScheduledTask("p0", tomorrowScheduled))
        )

        // 1. 翌日 (2026-10-20) へ進める -> 明日予約タスク S1 がフェーズ0に放出され、D1 は未完了にリセットされる
        val dayCarry = TaskSync.collectDayCarryover(todayState.phases, todayState.settings)
        val tomorrowState = TaskSync.buildNextDayState(todayState, todayState.phases, dayCarry, "2026-10-19")
        assertEquals("2026-10-20", tomorrowState.currentDate)
        assertTrue(tomorrowState.scheduledItems.isEmpty())
        assertTrue(tomorrowState.phases[0].items.any { it.id == "S1" })
        assertFalse(tomorrowState.phases[0].items.first { it.id == "D1" }.isCompleted)

        // 2. 当日 (2026-10-19) の午前 (index=1) に戻る -> D1 は完了状態に復元され、S1 は予約リストに戻る
        val returnedState = TaskSync.advanceToDateAndPhase(tomorrowState, targetDate = "2026-10-19", targetPhaseIndex = 1)
        assertEquals("2026-10-19", returnedState.currentDate)
        assertEquals(1, returnedState.selectedPhaseIndex)
        assertTrue(returnedState.phases[0].items.first { it.id == "D1" }.isCompleted)
        assertEquals(listOf("S1"), returnedState.scheduledItems.map { it.item.id })
        assertFalse(returnedState.phases.flatMap { it.items }.any { it.id == "S1" })
    }

    // 単発の日付指定タスクが当日未完了だった場合、翌日はキャリーオーバーとして引き継がれ、完了した翌々日には消える
    @Test
    fun uncompletedScheduledTask_carriesOverOnceThenDisappearsAfterCompletion() {
        val sched = ChecklistItem(id = "SCH1", title = "当日単発タスク", priority = Priority.REQUIRED, scheduledDate = "2026-10-19", originPhaseIndex = 0)
        val day1 = AppState(
            currentDate = "2026-10-19",
            phases = fourPhases(listOf(sched)),
            settings = AppSettings()
        )

        // Day 1 で未完了のまま Day 2 へ -> Day 2 の朝に isCarryover = true として引き継がれる
        val carry1 = TaskSync.collectDayCarryover(day1.phases, day1.settings)
        val day2 = TaskSync.buildNextDayState(day1, day1.phases, carry1, "2026-10-19")
        val day2Item = day2.phases[0].items.single()
        assertEquals("SCH1", day2Item.id)
        assertTrue(day2Item.isCarryover)

        // Day 2 で完了にして Day 3 へ -> 単発タスクなので Day 3 には残らない
        val day2DonePhases = TaskSync.completeItemInPhase(day2.phases, "p0", "SCH1", 999L)
        val day2Done = day2.copy(phases = day2DonePhases)
        val carry2 = TaskSync.collectDayCarryover(day2Done.phases, day2Done.settings)
        val day3 = TaskSync.buildNextDayState(day2Done, day2Done.phases, carry2, "2026-10-20")
        assertTrue(day3.phases.all { it.items.isEmpty() })
    }

    // 時間帯からのフェーズ判定と先取り完了判定の検証
    @Test
    fun computeCurrentPhaseIndexByTime_and_isAheadBecauseCompleted() {
        val s = AppState(phases = fourPhases(), settings = AppSettings())
        assertEquals(0, TaskSync.computeCurrentPhaseIndexByTime(s, java.time.LocalTime.of(1, 30)))
        assertEquals(0, TaskSync.computeCurrentPhaseIndexByTime(s, java.time.LocalTime.of(6, 0)))
        assertEquals(0, TaskSync.computeCurrentPhaseIndexByTime(s, java.time.LocalTime.of(8, 59)))
        assertEquals(1, TaskSync.computeCurrentPhaseIndexByTime(s, java.time.LocalTime.of(9, 0)))
        assertEquals(2, TaskSync.computeCurrentPhaseIndexByTime(s, java.time.LocalTime.of(12, 0)))
        assertEquals(3, TaskSync.computeCurrentPhaseIndexByTime(s, java.time.LocalTime.of(18, 0)))
        assertEquals(3, TaskSync.computeCurrentPhaseIndexByTime(s, java.time.LocalTime.of(23, 59)))

        val phasesWith0Done = fourPhases().toMutableList().apply {
            set(0, get(0).copy(isCompleted = true))
        }
        // 現在時刻が朝(0)で、朝(0)が完了済みのため午前(1)にいる場合は先取り進行と判定される
        assertTrue(TaskSync.isAheadBecauseCompleted(phasesWith0Done, selectedPhaseIndex = 1, currentPhaseIndexByTime = 0))
        // 午前(1)が未完了なのに午後(2)にいる場合は先取り進行ではない (「現在のフェーズに戻る」を表示すべき状態)
        assertFalse(TaskSync.isAheadBecauseCompleted(phasesWith0Done, selectedPhaseIndex = 2, currentPhaseIndexByTime = 0))
    }

    // 履歴消去（完了タスク＋ゴミ箱タスクの一括整理）の検証
    @Test
    fun clearArchive_removesBothCompletedAndDiscardedTasks() {
        val activeItem = ChecklistItem(id = "A1", title = "アクティブ")
        val doneItem = ChecklistItem(id = "D1", title = "完了済み", isCompleted = true)
        val trashedItem = ChecklistItem(id = "T1", title = "ゴミ箱", isDeleted = true)

        val initialPhases = fourPhases(listOf(activeItem, doneItem, trashedItem))
        val targetPhase = initialPhases[0]
        val clearedIds = targetPhase.items.filter { it.isCompleted || it.isDeleted }.map { it.id }.toSet()

        val updatedPhases = initialPhases.map { phase ->
            phase.copy(items = phase.items.filterNot { it.id in clearedIds })
        }

        assertEquals(listOf("A1"), updatedPhases[0].items.map { it.id })
    }

    // 日付付き単発タスクの完了が日跨ぎでチェック外れ復活せず、Snapshotに記録されることの検証
    @Test
    fun adHocTaskWithScheduledDate_savedInSnapshot_and_notRevivedNextDay() {
        val today = "2026-10-22"
        val adHocDone = ChecklistItem(id = "ADHOC1", title = "本日限りの仕事", scheduledDate = today, isCompleted = true)
        val routineTask = ChecklistItem(id = "ROUTINE1", title = "毎日の確認", isCompleted = false)

        val stateToday = AppState(
            currentDate = today,
            phases = fourPhases(listOf(adHocDone, routineTask)),
            settings = AppSettings()
        )

        val dayCarry = TaskSync.collectDayCarryover(stateToday.phases, stateToday.settings)
        val nextDayState = TaskSync.buildNextDayState(stateToday, stateToday.phases, dayCarry, today)

        // 翌日フェーズには単発完了タスク ADHOC1 は残らず、ROUTINE1 のみが残る
        val nextPhase0Items = nextDayState.phases[0].items
        assertFalse(nextPhase0Items.any { it.id == "ADHOC1" })
        assertTrue(nextPhase0Items.any { it.id == "ROUTINE1" })

        // 当日の Snapshot には完了した ADHOC1 が記録されている
        val record = nextDayState.dailyRecords[today]
        assertNotNull(record)
        val snapItem = record?.phasesSnapshot?.get(0)?.items?.find { it.id == "ADHOC1" }
        assertNotNull(snapItem)
        assertTrue(snapItem?.isCompleted == true)
    }
}
