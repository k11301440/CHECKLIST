package com.example.checklist.data

import java.time.LocalDate
import java.time.LocalTime
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

    /**
     * 指定フェーズ (phaseId) でタスクを完了にした際の更新処理。
     * - 完了操作を行ったフェーズ以前 (または全フェーズ) に存在する同一 ID のインスタンスを完了にする
     * - 完了操作を行ったフェーズより後のフェーズにある未完了キャリーオーバーコピーは不要になるため除去する
     *   (先のフェーズを覗き見した後に元フェーズで完了にした場合、先のフェーズに完了済みコピーが残留して件数が膨らむのを防止)
     */
    fun completeItemInPhase(
        phases: List<Phase>,
        phaseId: String,
        itemId: String,
        now: Long = System.currentTimeMillis()
    ): List<Phase> {
        val completedPhaseIdx = phases.indexOfFirst { it.id == phaseId }
        return phases.mapIndexed { idx, phase ->
            if (phase.items.none { it.id == itemId }) {
                phase
            } else if (completedPhaseIdx >= 0 && idx > completedPhaseIdx) {
                phase.copy(
                    items = phase.items.mapNotNull { item ->
                        if (item.id == itemId) {
                            if (item.isCarryover) null else item.copy(isCompleted = true, completedAt = now)
                        } else item
                    }
                )
            } else {
                phase.copy(
                    items = phase.items.map { item ->
                        if (item.id == itemId) item.copy(isCompleted = true, completedAt = now) else item
                    }
                )
            }
        }
    }

    /**
     * 指定フェーズ (phaseId) でタスクを破棄 (isDeleted = true) にした際の更新処理。
     * - 操作フェーズおよび発生元 (非キャリーオーバー) インスタンスを isDeleted = true にし、
     *   他フェーズに散らばったキャリーオーバーコピーは除去してゴミ箱の重複表示を防ぐ
     */
    fun removeItemInPhase(
        phases: List<Phase>,
        phaseId: String,
        itemId: String
    ): List<Phase> {
        val removedPhaseIdx = phases.indexOfFirst { it.id == phaseId }
        return phases.mapIndexed { idx, phase ->
            if (phase.items.none { it.id == itemId }) {
                phase
            } else {
                phase.copy(
                    items = phase.items.mapNotNull { item ->
                        if (item.id == itemId) {
                            if (!item.isCarryover || idx == removedPhaseIdx || removedPhaseIdx < 0) {
                                item.copy(isDeleted = true)
                            } else {
                                null
                            }
                        } else item
                    }
                )
            }
        }
    }

    fun containsItem(phases: List<Phase>, itemId: String): Boolean =
        phases.any { p -> p.items.any { it.id == itemId } }

    /**
     * 現在時刻 (now) とスケジュール設定から、現在時刻に対応するフェーズインデックスを算出する。
     */
    fun computeCurrentPhaseIndexByTime(
        state: AppState,
        now: LocalTime = LocalTime.now()
    ): Int {
        if (state.phases.isEmpty()) return 0
        val validSchedules = state.settings.phaseSchedules
            .filter { it.phaseIndex in state.phases.indices }
            .sortedBy { it.startHour * 60 + it.startMinute }
        if (validSchedules.isEmpty()) return state.selectedPhaseIndex.coerceIn(0, state.phases.lastIndex)

        val currentMinutes = now.hour * 60 + now.minute
        var matchedIndex = validSchedules.first().phaseIndex
        for (schedule in validSchedules) {
            val schedMinutes = schedule.startHour * 60 + schedule.startMinute
            if (currentMinutes >= schedMinutes) {
                matchedIndex = schedule.phaseIndex
            }
        }
        return matchedIndex.coerceIn(0, state.phases.lastIndex)
    }

    fun isPhaseCleared(phase: Phase): Boolean {
        val valid = phase.items.filterNot { it.isDeleted }
        return phase.isCompleted || (valid.isNotEmpty() && valid.none { !it.isCompleted })
    }

    /**
     * 現在の選択フェーズ (selectedPhaseIndex) が現在時刻のフェーズ (currentPhaseIndexByTime) より先にある場合、
     * その間のフェーズがすべて完了（または全タスク消化済み）による先取り移行かどうかを判定する。
     */
    fun isAheadBecauseCompleted(
        phases: List<Phase>,
        selectedPhaseIndex: Int,
        currentPhaseIndexByTime: Int
    ): Boolean {
        if (selectedPhaseIndex <= currentPhaseIndexByTime) return false
        return (currentPhaseIndexByTime until selectedPhaseIndex).all { idx ->
            phases.getOrNull(idx)?.let(::isPhaseCleared) == true
        }
    }

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
    const val MAX_SNAPSHOT_RECORDS = 14
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
    fun createDailyRecord(
        date: String,
        phases: List<Phase>,
        now: Long = System.currentTimeMillis(),
        includeSnapshot: Boolean = true
    ): DailyRecord {
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
            phaseSummaries = summaries,
            phasesSnapshot = if (includeSnapshot) phases else emptyList()
        )
    }

    /**
     * 現在のフェーズ (currentPhaseIndex) より未来のフェーズにある「未完了のキャリーオーバーコピー」や、
     * キャリーオーバー設定OFFの優先度の未完了キャリーオーバーコピーを整理する。
     * これにより、未来のフェーズを覗き見してから元のフェーズに戻った際に未来フェーズへ未完了コピーが残留しない。
     * ※ 未来のフェーズで既に「完了済み (isCompleted = true)」にしたキャリーオーバータスクは保護する。
     */
    fun trimFutureUnfinishedCarryover(
        phases: List<Phase>,
        currentPhaseIndex: Int,
        settings: AppSettings? = null
    ): List<Phase> {
        if (phases.isEmpty()) return phases
        val clamped = currentPhaseIndex.coerceIn(0, phases.lastIndex)
        return phases.mapIndexed { idx, phase ->
            val earlierIds = if (idx > 0) {
                phases.subList(0, idx).flatMap { it.items }.map { it.id }.toSet()
            } else {
                emptySet()
            }
            val filtered = phase.items.filterNot { item ->
                val isSameDayCarryover = item.isCarryover && item.id in earlierIds
                val isUnfinished = !item.isCompleted && !item.isDeleted
                val isAfterCurrent = idx > clamped
                val isDisabledBySettings = settings != null && (
                    (item.priority == Priority.REQUIRED && !settings.carryoverUnfinishedReq) ||
                    (item.priority == Priority.NORMAL && !settings.carryoverUnfinishedNorm)
                )
                isSameDayCarryover && isUnfinished && (isAfterCurrent || isDisabledBySettings)
            }
            if (filtered.size == phase.items.size) phase else phase.copy(items = filtered)
        }
    }

    /**
     * フェーズ 0 から targetIndex まで順番に未完了の持越対象タスクを伝播させる。
     * 「次のフェーズへ」ボタンを押さずにタブ・スワイプ・時間自動進行・「現在のフェーズに戻る」等で
     * 次の時間帯へ移動した場合でも、前フェーズの未完了タスクが確実にキャリーオーバーされることを保証する。
     * 同時に targetIndex より後のフェーズにある未完了持越コピーは整理する。
     */
    fun propagateCarryoverUpTo(
        phases: List<Phase>,
        targetIndex: Int,
        settings: AppSettings
    ): List<Phase> {
        if (phases.isEmpty()) return phases
        val end = targetIndex.coerceIn(0, phases.lastIndex)
        val trimmed = trimFutureUnfinishedCarryover(phases, end, settings).toMutableList()
        for (i in 0 until end) {
            val src = trimmed[i]
            val dst = trimmed[i + 1]
            val carry = selectCarryover(src.items, src.name, settings)
            if (carry.isNotEmpty()) {
                trimmed[i + 1] = dst.copy(items = mergeCarryover(dst.items, carry))
            }
        }
        return trimmed
    }

    /**
     * 指定した日付 (targetDate) とフェーズ (targetPhaseIndex) へ状態を遷移させる。
     * - 現在の日付が過去日の場合は日次レコードを保存し、未完了タスクを翌朝(Phase 0)へ引き継いでから targetDate へ進める
     * - 現在の日付が未来日の場合 (手動で翌日へ進んだ後に当日へ戻る等) は、未来日付の予約タスクを予約リストへ戻し、
     *   保存されていた targetDate のフェーズスナップショットがあれば復元する
     * - その後、Phase 0 から targetPhaseIndex まで未完了タスクを伝播させる
     */
    fun advanceToDateAndPhase(
        state: AppState,
        targetDate: String,
        targetPhaseIndex: Int
    ): AppState {
        if (state.phases.isEmpty()) return state.copy(currentDate = targetDate)

        val dateAlignedState = when {
            state.currentDate.isNotEmpty() && state.currentDate < targetDate -> {
                val dayCarryover = collectDayCarryover(state.phases, state.settings)
                buildNextDayState(
                    current = state,
                    basePhases = state.phases,
                    uncompletedItems = dayCarryover,
                    today = state.currentDate,
                    targetDate = targetDate
                )
            }
            state.currentDate.isNotEmpty() && state.currentDate > targetDate -> {
                restoreOrRewindToDate(state, targetDate)
            }
            else -> {
                releaseDueScheduled(state.copy(currentDate = targetDate), targetDate)
            }
        }

        val clampedIndex = targetPhaseIndex.coerceIn(0, dateAlignedState.phases.lastIndex)
        val propagatedPhases = propagateCarryoverUpTo(
            phases = dateAlignedState.phases,
            targetIndex = clampedIndex,
            settings = dateAlignedState.settings
        )
        return reconcile(
            dateAlignedState.copy(
                phases = propagatedPhases,
                selectedPhaseIndex = clampedIndex
            )
        )
    }

    /**
     * 翌日以降 (state.currentDate > targetDate) から targetDate へ戻る際の復元処理。
     * 1. targetDate より後の予約タスク (scheduledDate > targetDate) を scheduledItems へ戻す
     * 2. dailyRecords[targetDate] に phasesSnapshot が保存されている場合はそれを復元し、
     *    翌日画面で新規追加されたタスクや完了状態の変更があればマージする
     */
    private fun restoreOrRewindToDate(state: AppState, targetDate: String): AppState {
        val rewound = extractFutureScheduled(state.copy(currentDate = targetDate))
        val snapshot = rewound.dailyRecords[targetDate]?.phasesSnapshot
        if (snapshot.isNullOrEmpty() || snapshot.size != rewound.phases.size) {
            return releaseDueScheduled(rewound, targetDate)
        }

        val snapshotIds = snapshot.flatMap { it.items }.map { it.id }.toSet()
        // 翌日画面で完了・破棄・タイトル変更されたタスクの状態を収集
        val currentItemsById = rewound.phases.flatMap { it.items }.associateBy { it.id }

        val restoredPhases = snapshot.mapIndexed { idx, snapPhase ->
            val currentPhase = rewound.phases.getOrNull(idx)
            // スナップショットに存在しなかった新規追加タスク（非キャリーオーバー）を保持
            val newlyAdded = currentPhase?.items?.filter { item ->
                item.id !in snapshotIds && !item.isCarryover &&
                    (item.scheduledDate == null || item.scheduledDate <= targetDate)
            } ?: emptyList()

            val syncedSnapItems = snapPhase.items.map { item ->
                val cur = currentItemsById[item.id]
                if (cur != null) {
                    // 翌日側で新たに完了または破棄された場合はその変更を反映、それ以外はスナップショットの完了状態を維持
                    val completed = item.isCompleted || cur.isCompleted
                    val completedAt = if (completed) (item.completedAt ?: cur.completedAt) else null
                    item.copy(
                        title = cur.title,
                        priority = cur.priority,
                        isCompleted = completed,
                        completedAt = completedAt,
                        isDeleted = item.isDeleted || cur.isDeleted
                    )
                } else item
            }

            snapPhase.copy(items = syncedSnapItems + newlyAdded)
        }

        return releaseDueScheduled(rewound.copy(phases = restoredPhases), targetDate)
    }

    /** 全フェーズから翌日への持越対象を ID 重複なしで抽出 (最初に現れたインスタンス = 発生元を優先) */
    fun collectDayCarryover(phases: List<Phase>, settings: AppSettings): List<ChecklistItem> =
        phases.flatMap { p -> selectCarryover(p.items, p.name, settings) }.distinctBy { it.id }

    fun buildNextDayState(
        current: AppState,
        basePhases: List<Phase>,
        uncompletedItems: List<ChecklistItem>,
        today: String,
        updatedReviewLogs: List<ReviewLog>? = null,
        targetDate: String? = null
    ): AppState {
        val dailyRecord = createDailyRecord(today, basePhases, includeSnapshot = true)
        val rawDailyRecords = current.dailyRecords + (today to dailyRecord)
        val sortedEntries = rawDailyRecords.entries.sortedByDescending { it.key }
        val updatedDailyRecords = sortedEntries
            .take(MAX_DAILY_RECORDS)
            .mapIndexed { idx, entry ->
                if (idx < MAX_SNAPSHOT_RECORDS) entry.key to entry.value
                else entry.key to entry.value.copy(phasesSnapshot = emptyList())
            }
            .toMap()

        val baseDate = runCatching { LocalDate.parse(today) }.getOrElse { LocalDate.now() }
        val tomorrowDate = targetDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: baseDate.plusDays(1)
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
                    // 当日以前の単発日付予約タスクは基本ルーティンには残さない (未完了かつ持越対象のものは uniqueCarryover で翌朝へ引き継がれる)
                    .filterNot { it.scheduledDate != null && it.scheduledDate <= today }
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
