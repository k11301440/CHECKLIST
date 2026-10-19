package com.example.checklist.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * タスク状態遷移の純粋ロジック (Android 非依存・JUnit でテスト可能)。
 *
 * 同一タスクの判定は「ID 一致」のみで行う。
 * キャリーオーバー時は ID を保持したままコピーするため、ID だけで
 * 発生元 ⇔ 持越先の全インスタンスを追跡できる。
 * タイトル一致による同期は、意図的に作成された同名の独立タスク
 * (例: 朝と午後の「メール確認」) を誤って連動させるため使用しない。
 */
object TaskSync {

    private fun normalizeTitle(title: String): String = title.trim().lowercase()

    /** 全フェーズに存在する同一 ID のタスクへ transform を一括適用する */
    fun updateLinked(
        phases: List<Phase>,
        itemId: String,
        transform: (ChecklistItem) -> ChecklistItem
    ): List<Phase> = phases.map { phase ->
        if (phase.items.none { it.id == itemId }) phase
        else phase.copy(items = phase.items.map { if (it.id == itemId) transform(it) else it })
    }

    fun containsItem(phases: List<Phase>, itemId: String): Boolean =
        phases.any { p -> p.items.any { it.id == itemId } }

    private data class Status(
        val title: String,
        val priority: Priority,
        val isCompleted: Boolean,
        val completedAt: Long?,
        val isDeleted: Boolean
    )

    /**
     * 自動整合エンジン (保存直前・読み込み時に実行)。
     * - 同一フェーズ内の同一 ID 重複を 1 件に統合
     * - 同一 ID の全インスタンスで 完了 / 破棄 を OR 同期 (古い状態による巻き戻り防止)
     * - タイトル・優先度は発生元 (非キャリーオーバー) インスタンスの内容に統一
     *
     * 注意: 完了/破棄は OR 合成のため、「戻す」系の操作 (undo / restore / reset) は
     * 必ず [updateLinked] で全インスタンスを同時に更新すること。
     */
    fun reconcile(state: AppState): AppState {
        val byId = LinkedHashMap<String, Status>()
        val originSeen = HashSet<String>()

        for (phase in state.phases) {
            for (item in phase.items) {
                val prev = byId[item.id]
                val takeContent = prev == null || (!item.isCarryover && item.id !in originSeen)
                if (!item.isCarryover) originSeen.add(item.id)

                val completed = (prev?.isCompleted == true) || item.isCompleted
                val completedAt = if (completed) {
                    listOfNotNull(prev?.completedAt, item.completedAt).maxOrNull()
                } else null

                byId[item.id] = Status(
                    title = if (takeContent) item.title else prev.title,
                    priority = if (takeContent) item.priority else prev.priority,
                    isCompleted = completed,
                    completedAt = completedAt,
                    isDeleted = (prev?.isDeleted == true) || item.isDeleted
                )
            }
        }

        val phases = state.phases.map { phase ->
            val seen = HashSet<String>()
            val items = phase.items.mapNotNull { item ->
                if (!seen.add(item.id)) return@mapNotNull null
                val s = byId.getValue(item.id)
                if (item.title == s.title && item.priority == s.priority &&
                    item.isCompleted == s.isCompleted && item.completedAt == s.completedAt &&
                    item.isDeleted == s.isDeleted
                ) item else item.copy(
                    title = s.title,
                    priority = s.priority,
                    isCompleted = s.isCompleted,
                    completedAt = s.completedAt,
                    isDeleted = s.isDeleted
                )
            }
            phase.copy(items = items)
        }
        return state.copy(phases = phases)
    }

    const val MAX_DAILY_RECORDS = 400
    const val MAX_REVIEW_LOGS = 1000

    /**
     * 引き継ぎ対象の未完了タスクを抽出 (REQ → NORM の順)。
     * 既にキャリーオーバー済みのタスクは発生元フェーズ名を維持する (多段持越で発生元が上書きされない)。
     */
    fun selectCarryover(
        items: List<ChecklistItem>,
        sourcePhaseName: String,
        settings: AppSettings
    ): List<ChecklistItem> {
        fun pick(priority: Priority) = items
            .filter { it.priority == priority && !it.isCompleted && !it.isDeleted }
            .map { if (it.isCarryover) it else it.copy(isCarryover = true, sourcePhaseName = sourcePhaseName) }

        val result = mutableListOf<ChecklistItem>()
        if (settings.carryoverUnfinishedReq) result += pick(Priority.REQUIRED)
        if (settings.carryoverUnfinishedNorm) result += pick(Priority.NORMAL)
        return result
    }

    /**
     * 次フェーズへのキャリーオーバー合流 (冪等)。
     * 同一 ID が既に存在する (完了・破棄済み含む) 場合、または
     * 同一タイトルの「未完了」タスクが存在する場合はスキップ。
     */
    fun mergeCarryover(target: List<ChecklistItem>, carry: List<ChecklistItem>): List<ChecklistItem> {
        if (carry.isEmpty()) return target
        val ids = target.map { it.id }.toSet()
        val activeTitles = target.filter { !it.isCompleted && !it.isDeleted }
            .map { normalizeTitle(it.title) }.toSet()
        val toAdd = carry.distinctBy { it.id }
            .filter { it.id !in ids && normalizeTitle(it.title) !in activeTitles }
        return toAdd + target
    }

    fun isTemplateApplicable(template: PhaseTemplate, date: LocalDate): Boolean {
        // 曜日チェック (1:月曜 .. 7:日曜)。空なら全曜日
        if (template.targetDaysOfWeek.isNotEmpty() && date.dayOfWeek.value !in template.targetDaysOfWeek) {
            return false
        }
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (template.startDate != null && dateStr < template.startDate) return false
        if (template.endDate != null && dateStr > template.endDate) return false
        return true
    }

    /**
     * 日別レコード作成。キャリーオーバーで複数フェーズに存在する同一タスクは
     * 合計値では 1 件として数える (二重計上防止)。
     */
    fun createDailyRecord(date: String, phases: List<Phase>, now: Long = System.currentTimeMillis()): DailyRecord {
        val unique = phases.flatMap { it.items }.filterNot { it.isDeleted }.distinctBy { it.id }
        val summaries = phases.map { p ->
            PhaseDailySummary(
                phaseName = p.name,
                isCompleted = p.isCompleted,
                completedCount = p.totalDoneCount,
                totalCount = p.totalValidCount,
                reviewNote = p.reviewNote,
                hadIssue = p.reviewNote != null
            )
        }
        return DailyRecord(
            dateString = date,
            completedAt = now,
            totalItems = unique.size,
            completedItems = unique.count { it.isCompleted },
            requiredPendingItems = unique.count { it.priority == Priority.REQUIRED && !it.isCompleted },
            phaseSummaries = summaries
        )
    }

    /** 全フェーズから翌日への持越対象を ID 重複なしで抽出 (最初に現れたインスタンス = 発生元を優先) */
    fun collectDayCarryover(phases: List<Phase>, settings: AppSettings): List<ChecklistItem> =
        phases.flatMap { p -> selectCarryover(p.items, p.name, settings) }.distinctBy { it.id }

    fun buildNextDayState(
        current: AppState,
        basePhases: List<Phase>,
        uncompletedItems: List<ChecklistItem>,
        today: String,
        updatedReviewLogs: List<ReviewLog>? = null
    ): AppState {
        val dailyRecord = createDailyRecord(today, basePhases)
        val rawDailyRecords = current.dailyRecords + (today to dailyRecord)
        val updatedDailyRecords = if (rawDailyRecords.size > MAX_DAILY_RECORDS) {
            rawDailyRecords.entries
                .sortedByDescending { it.key }
                .take(MAX_DAILY_RECORDS)
                .associate { it.toPair() }
        } else {
            rawDailyRecords
        }

        val baseDate = runCatching { LocalDate.parse(today) }.getOrElse { LocalDate.now() }
        val tomorrowDate = baseDate.plusDays(1)
        val tomorrow = tomorrowDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val uniqueCarryover = uncompletedItems.distinctBy { it.id }
        val carryoverIds = uniqueCarryover.map { it.id }.toSet()

        val resetPhases = basePhases.mapIndexed { index, p ->
            // 明日の日付・曜日に合致するテンプレートを選定
            val templateForPhase = current.templates.find { t ->
                val isPhaseMatch = (current.templates.indexOf(t) == index) || (t.name.contains(p.name))
                isPhaseMatch && isTemplateApplicable(t, tomorrowDate)
            } ?: current.templates.getOrNull(index)?.takeIf { isTemplateApplicable(it, tomorrowDate) }

            val baseItems = if (templateForPhase != null && templateForPhase.items.isNotEmpty()) {
                templateForPhase.items.map { ChecklistItem(title = it.title, priority = it.priority, originPhaseIndex = index) }
            } else {
                p.items
                    .filterNot { it.isDeleted }
                    // キャリーオーバー個体 (isCarryover = true) は一時的な持越データのため、翌日の基本ルーティンには残さない
                    // (未完了のものは uniqueCarryover 経由で翌朝フェーズの先頭に引き継がれる)
                    .filterNot { it.isCarryover }
                    // 当日以前の単発日付予約タスクで完了済みのものは翌日に繰り返さない
                    .filterNot { it.scheduledDate != null && it.scheduledDate <= today && it.isCompleted }
                    // 翌朝(index=0)へキャリーオーバーされる未完了タスクが元フェーズ(index>0)にも残って二重化しないよう除外
                    .filterNot { index > 0 && it.id in carryoverIds }
                    .map {
                        it.copy(
                            isCompleted = false,
                            completedAt = null,
                            isCarryover = false,
                            sourcePhaseName = null
                        )
                    }
                    .distinctBy { it.id }
            }

            val mergedItems = if (index == 0 && uniqueCarryover.isNotEmpty()) {
                val existingIds = baseItems.map { it.id }.toSet()
                val existingTitles = baseItems.map { normalizeTitle(it.title) }.toSet()
                val toAdd = uniqueCarryover
                    .filter { it.id !in existingIds && normalizeTitle(it.title) !in existingTitles }
                toAdd + baseItems
            } else {
                baseItems
            }

            p.copy(
                items = mergedItems,
                isCompleted = false,
                completedAt = null,
                reviewNote = null
            )
        }

        val prunedLogs = (updatedReviewLogs ?: current.reviewLogs).take(MAX_REVIEW_LOGS)

        val nextState = current.copy(
            currentDate = tomorrow,
            phases = resetPhases,
            selectedPhaseIndex = 0,
            reviewLogs = prunedLogs,
            dailyRecords = updatedDailyRecords
        )
        return releaseDueScheduled(nextState, tomorrow)
    }

    /** 日付指定タスクのうち upToDate 以前のものを対象フェーズへ投入する */
    fun releaseDueScheduled(state: AppState, upToDate: String): AppState {
        if (state.scheduledItems.isEmpty() || state.phases.isEmpty()) return state
        val validScheduled = state.scheduledItems.filterNot { it.item.isDeleted }
        val (due, later) = validScheduled.partition { (it.item.scheduledDate ?: "") <= upToDate }
        if (due.isEmpty() && validScheduled.size == state.scheduledItems.size) return state

        val phases = state.phases.toMutableList()
        for (entry in due) {
            var idx = phases.indexOfFirst { it.id == entry.phaseId }
            if (idx < 0) idx = entry.item.originPhaseIndex.coerceIn(0, phases.lastIndex)
            val p = phases[idx]
            if (p.items.none { it.id == entry.item.id }) {
                phases[idx] = p.copy(items = listOf(entry.item.copy(originPhaseIndex = idx)) + p.items)
            }
        }
        return state.copy(phases = phases, scheduledItems = later)
    }

    /** 旧データ移行: フェーズ内に直接入っている未来日付タスクを予約リストへ移す */
    fun extractFutureScheduled(state: AppState): AppState {
        val today = state.currentDate
        if (today.isEmpty()) return state
        val moved = mutableListOf<ScheduledTask>()
        val phases = state.phases.map { p ->
            val (future, keep) = p.items.partition {
                val d = it.scheduledDate
                d != null && d > today && !it.isCompleted && !it.isDeleted && !it.isCarryover
            }
            future.forEach { moved += ScheduledTask(phaseId = p.id, item = it) }
            if (future.isEmpty()) p else p.copy(items = keep)
        }
        if (moved.isEmpty()) return state
        val existing = state.scheduledItems.map { it.item.id }.toSet()
        return state.copy(
            phases = phases,
            scheduledItems = state.scheduledItems + moved.filter { it.item.id !in existing }
        )
    }
}
