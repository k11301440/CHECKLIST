package com.example.checklist.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.example.checklist.notification.ReqNotificationHelper

class ChecklistRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val dataFile: File
        get() = File(context.filesDir, "checklist_state.json")

    private val tempFile: File
        get() = File(context.filesDir, "checklist_state.json.tmp")

    private val backupFile: File
        get() = File(context.filesDir, "checklist_state.json.bak")

    private val _appState = MutableStateFlow(loadInitialState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        try {
            ReqNotificationHelper.createNotificationChannel(context)
            ReqNotificationHelper.updateNotification(context, _appState.value)
            ReqNotificationHelper.schedulePhaseReminder(context, _appState.value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getTodayDateString(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun tryLoadFromFile(file: File): AppState? {
        return try {
            if (file.exists() && file.length() > 0) {
                val text = file.readText()
                json.decodeFromString<AppState>(text)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadInitialState(): AppState {
        val today = getTodayDateString()
        // 1. メインファイルからの読み込み、失敗時はバックアップファイルから自己修復
        val loaded = tryLoadFromFile(dataFile)
            ?: tryLoadFromFile(backupFile)
            ?: createDefaultState()

        val stateWithDate = if (loaded.currentDate.isEmpty()) {
            loaded.copy(currentDate = today)
        } else {
            loaded
        }

        // フェーズが空、または旧来の「日中」が含まれる場合のみマイグレーション (3フェーズや5フェーズのカスタムは保護)
        return if (stateWithDate.phases.isEmpty() || stateWithDate.phases.any { it.name == "日中" }) {
            val migrated = createDefaultState().copy(
                currentDate = today,
                templates = if (stateWithDate.templates.isNotEmpty()) stateWithDate.templates else createDefaultState().templates,
                reviewLogs = stateWithDate.reviewLogs,
                dailyRecords = stateWithDate.dailyRecords,
                settings = stateWithDate.settings
            )
            persist(migrated)
            migrated
        } else {
            stateWithDate
        }
    }

    companion object {
        fun getDefaultPresetTemplates(): List<PhaseTemplate> {
            return listOf(
                PhaseTemplate(
                    name = "朝のセットアップ",
                    items = listOf(
                        TemplateItem(title = "本日の最優先フォーカスタスク選定", priority = Priority.REQUIRED),
                        TemplateItem(title = "スケジュール & 会議予定の確認", priority = Priority.REQUIRED),
                        TemplateItem(title = "重要メール・未読チャット一次確認", priority = Priority.NORMAL),
                        TemplateItem(title = "作業環境・デスクの整備", priority = Priority.NORMAL)
                    )
                ),
                PhaseTemplate(
                    name = "午前集中ブロック",
                    items = listOf(
                        TemplateItem(title = "最重要タスク (Deep Work) の着手・完遂", priority = Priority.REQUIRED),
                        TemplateItem(title = "午前中のチーム連携・報連相", priority = Priority.NORMAL),
                        TemplateItem(title = "午前に向けた進捗メモ更新", priority = Priority.NORMAL)
                    )
                ),
                PhaseTemplate(
                    name = "午後実行ブロック",
                    items = listOf(
                        TemplateItem(title = "午後コア業務の推進・完了", priority = Priority.REQUIRED),
                        TemplateItem(title = "外部連絡・打ち合わせの実施", priority = Priority.NORMAL),
                        TemplateItem(title = "残タスクとブロッカーの特定", priority = Priority.NORMAL)
                    )
                ),
                PhaseTemplate(
                    name = "夜のクローズ & 翌日準備",
                    items = listOf(
                        TemplateItem(title = "翌日スケジュールの策定 & タスク整理", priority = Priority.REQUIRED),
                        TemplateItem(title = "本日の実績記録と日報・メモ作成", priority = Priority.NORMAL),
                        TemplateItem(title = "作業端末・環境のクローズ", priority = Priority.NORMAL)
                    )
                )
            )
        }
    }

    private fun createDefaultState(): AppState {
        val today = getTodayDateString()
        val defaultPhases = listOf(
            Phase(name = "朝", items = emptyList()),
            Phase(name = "午前", items = emptyList()),
            Phase(name = "午後", items = emptyList()),
            Phase(name = "夜", items = emptyList())
        )

        return AppState(
            currentDate = today,
            phases = defaultPhases,
            selectedPhaseIndex = 0,
            templates = emptyList(),
            reviewLogs = emptyList(),
            dailyRecords = emptyMap(),
            settings = AppSettings()
        )
    }

    private fun persist(newState: AppState) {
        _appState.value = newState
        scope.launch {
            try {
                val jsonString = json.encodeToString(newState)
                // 1. 一時ファイルに完全に書き込み
                tempFile.writeText(jsonString)
                // 2. メインファイルが存在する場合はバックアップを更新
                if (dataFile.exists()) {
                    dataFile.copyTo(backupFile, overwrite = true)
                }
                // 3. アトミック置換 (renameTo失敗時はcopyTo+deleteでフォールバック)
                if (!tempFile.renameTo(dataFile)) {
                    tempFile.copyTo(dataFile, overwrite = true)
                    tempFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                ReqNotificationHelper.updateNotification(context, newState)
                ReqNotificationHelper.schedulePhaseReminder(context, newState)
                com.example.checklist.widget.WidgetUpdateHelper.updateAllWidgets(context, newState)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectPhase(index: Int) {
        val current = _appState.value
        if (index in current.phases.indices) {
            persist(current.copy(selectedPhaseIndex = index))
        }
    }

    fun completeItem(phaseId: String, itemId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val updatedItems = phase.items.map { item ->
                    if (item.id == itemId) {
                        item.copy(isCompleted = true, completedAt = System.currentTimeMillis())
                    } else item
                }
                phase.copy(items = updatedItems)
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    fun undoItem(phaseId: String, itemId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val updatedItems = phase.items.map { item ->
                    if (item.id == itemId) {
                        item.copy(isCompleted = false, completedAt = null)
                    } else item
                }
                phase.copy(items = updatedItems)
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    fun addItem(phaseId: String, title: String, priority: Priority, scheduledDate: String? = null) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val current = _appState.value
        val newItem = ChecklistItem(
            title = trimmed,
            priority = priority,
            scheduledDate = scheduledDate
        )
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val currentItems = phase.items.toMutableList()
                val insertIndex = if (priority == Priority.REQUIRED) {
                    // 未完了のNORMタスクの先頭を探す
                    val firstActiveNormIndex = currentItems.indexOfFirst {
                        !it.isCompleted && !it.isDeleted && it.priority == Priority.NORMAL
                    }
                    if (firstActiveNormIndex != -1) {
                        firstActiveNormIndex
                    } else {
                        // 未完了NORMがなければ、最初の非アクティブ（完了または削除）アイテムの手前、なければ末尾
                        val firstInactiveIndex = currentItems.indexOfFirst { it.isCompleted || it.isDeleted }
                        if (firstInactiveIndex != -1) firstInactiveIndex else currentItems.size
                    }
                } else {
                    // NORMの場合：未完了タスクの末尾（＝最初の非アクティブアイテムの手前、なければ末尾）
                    val firstInactiveIndex = currentItems.indexOfFirst { it.isCompleted || it.isDeleted }
                    if (firstInactiveIndex != -1) firstInactiveIndex else currentItems.size
                }
                currentItems.add(insertIndex, newItem)
                phase.copy(items = currentItems)
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    fun updateItem(phaseId: String, itemId: String, newTitle: String, newPriority: Priority) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val updatedItems = phase.items.map { item ->
                    if (item.id == itemId) {
                        item.copy(title = trimmed, priority = newPriority)
                    } else item
                }
                phase.copy(items = updatedItems)
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    // 誤破棄防止のため即時削除ではなく保管（isDeleted = true）
    fun removeItem(phaseId: String, itemId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val updatedItems = phase.items.map { item ->
                    if (item.id == itemId) {
                        item.copy(isDeleted = true)
                    } else item
                }
                phase.copy(items = updatedItems)
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    // 削除されたアイテムを復元
    fun restoreItem(phaseId: String, itemId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val updatedItems = phase.items.map { item ->
                    if (item.id == itemId) {
                        item.copy(isDeleted = false)
                    } else item
                }
                phase.copy(items = updatedItems)
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    // アイテムの並び替え (隣接要素の安全な位置スワップ)
    fun reorderItems(phaseId: String, fromIndex: Int, toIndex: Int) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val list = phase.items.toMutableList()
                if (fromIndex in list.indices && toIndex in list.indices) {
                    java.util.Collections.swap(list, fromIndex, toIndex)
                }
                phase.copy(items = list)
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    fun clearCompletedItems(phaseId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                phase.copy(items = phase.items.filterNot { it.isCompleted || it.isDeleted })
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    fun addPhase(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val current = _appState.value
        if (current.phases.size >= 8) return false
        val newPhase = Phase(name = trimmed)
        val newIndex = current.phases.size

        val existingSchedules = current.settings.phaseSchedules
        val nextHour = if (existingSchedules.isNotEmpty()) {
            (existingSchedules.maxOf { it.startHour } + 3) % 24
        } else {
            (newIndex * 4 + 7) % 24
        }
        val newSchedule = PhaseSchedule(
            phaseIndex = newIndex,
            phaseName = trimmed,
            startHour = nextHour,
            startMinute = 0
        )
        val updatedSettings = current.settings.copy(phaseSchedules = existingSchedules + newSchedule)
        persist(current.copy(phases = current.phases + newPhase, settings = updatedSettings))
        return true
    }

    fun removePhase(phaseId: String): Boolean {
        val current = _appState.value
        if (current.phases.size <= 1) return false
        val targetIndex = current.phases.indexOfFirst { it.id == phaseId }
        if (targetIndex < 0) return false

        val updatedPhases = current.phases.filterNot { it.id == phaseId }
        val newIndex = current.selectedPhaseIndex.coerceAtMost(updatedPhases.size - 1)

        val updatedSchedules = updatedPhases.mapIndexed { idx, p ->
            val oldSchedule = current.settings.phaseSchedules.find { it.phaseIndex == (if (idx >= targetIndex) idx + 1 else idx) }
            oldSchedule?.copy(phaseIndex = idx, phaseName = p.name) ?: PhaseSchedule(
                phaseIndex = idx,
                phaseName = p.name,
                startHour = (idx * 4 + 7) % 24,
                startMinute = 0
            )
        }
        val updatedSettings = current.settings.copy(phaseSchedules = updatedSchedules)

        persist(current.copy(phases = updatedPhases, selectedPhaseIndex = newIndex, settings = updatedSettings))
        return true
    }

    fun updatePhaseName(phaseId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) phase.copy(name = trimmed) else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    // 振り返りとフェーズ完了
    // 夜フェーズの場合は翌朝へ進み、未完了タスク（REQ / NORM設定連動）は持ち越し
    fun completePhase(phaseId: String, hadIssue: Boolean, note: String?) {
        val current = _appState.value
        val targetPhaseIndex = current.phases.indexOfFirst { it.id == phaseId }
        if (targetPhaseIndex < 0) return
        val targetPhase = current.phases[targetPhaseIndex]

        val today = current.currentDate.ifEmpty { getTodayDateString() }
        val newLog = ReviewLog(
            timestamp = System.currentTimeMillis(),
            phaseName = targetPhase.name,
            hadIssue = hadIssue,
            note = if (hadIssue) note?.trim()?.ifEmpty { null } else null,
            dateString = today
        )

        // 対象フェーズを完了マーク（削除済みアイテムは除外・整理）
        val updatedPhase = targetPhase.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
            reviewNote = newLog.note,
            items = targetPhase.items.filterNot { it.isDeleted }
        )

        val phasesAfterTargetCompletion = current.phases.toMutableList().apply {
            set(targetPhaseIndex, updatedPhase)
        }

        // 未完了タスクの引き継ぎリスト（REQ / NORM）
        val uncompletedItems = mutableListOf<ChecklistItem>()
        if (current.settings.carryoverUnfinishedReq) {
            uncompletedItems.addAll(
                targetPhase.items.filter { it.priority == Priority.REQUIRED && !it.isCompleted && !it.isDeleted }.map {
                    it.copy(isCarryover = true, sourcePhaseName = targetPhase.name)
                }
            )
        }
        if (current.settings.carryoverUnfinishedNorm) {
            uncompletedItems.addAll(
                targetPhase.items.filter { it.priority == Priority.NORMAL && !it.isCompleted && !it.isDeleted }.map {
                    it.copy(isCarryover = true, sourcePhaseName = targetPhase.name)
                }
            )
        }

        // 同一日・同一フェーズの既存ログを重複排除して最新1件に更新
        val cleanedLogs = current.reviewLogs.filterNot {
            it.dateString == today && it.phaseName == targetPhase.name
        }
        val updatedReviewLogs = listOf(newLog) + cleanedLogs

        // 最後のフェーズ（夜）かどうか
        val isLastPhase = targetPhaseIndex >= current.phases.size - 1

        if (isLastPhase) {
            // 夜セクション完了 -> 本日の日別レコードを保存し、翌日の朝へ進む (統合ヘルパー呼び出し)
            val nextDayState = buildNextDayState(
                current = current,
                basePhases = phasesAfterTargetCompletion,
                uncompletedItems = uncompletedItems,
                today = today,
                updatedReviewLogs = updatedReviewLogs
            )
            persist(nextDayState)
        } else {
            // 通常のフェーズ完了 -> 次のフェーズへ進み、未完了タスクを次のフェーズへキャリーオーバー（優先度高いため上部に表示）
            val nextIndex = targetPhaseIndex + 1
            val nextPhase = phasesAfterTargetCompletion[nextIndex]

            val nextPhaseItems = if (uncompletedItems.isNotEmpty()) {
                val existingTitles = nextPhase.items.map { it.title }.toSet()
                val itemsToAdd = uncompletedItems.filter { it.title !in existingTitles }
                itemsToAdd + nextPhase.items
            } else {
                nextPhase.items
            }

            phasesAfterTargetCompletion[nextIndex] = nextPhase.copy(items = nextPhaseItems)

            persist(
                current.copy(
                    phases = phasesAfterTargetCompletion,
                    selectedPhaseIndex = nextIndex,
                    reviewLogs = updatedReviewLogs
                )
            )
        }
    }

    private fun buildNextDayState(
        current: AppState,
        basePhases: List<Phase>,
        uncompletedItems: List<ChecklistItem>,
        today: String,
        updatedReviewLogs: List<ReviewLog>? = null
    ): AppState {
        val dailyRecord = createDailyRecord(today, basePhases)
        val updatedDailyRecords = current.dailyRecords + (today to dailyRecord)

        val baseDate = runCatching { LocalDate.parse(today) }.getOrElse { LocalDate.now() }
        val tomorrow = baseDate.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val resetPhases = basePhases.mapIndexed { index, p ->
            val templateForPhase = current.templates.getOrNull(index)
            val baseItems = if (templateForPhase != null && templateForPhase.items.isNotEmpty()) {
                templateForPhase.items.map { ChecklistItem(title = it.title, priority = it.priority) }
            } else {
                p.items.filterNot { it.isDeleted }.map { it.copy(isCompleted = false, completedAt = null) }
            }

            // 翌日指定タスク
            val scheduledForTomorrow = p.items.filter { it.scheduledDate == tomorrow && !it.isDeleted }

            val mergedItems = if (index == 0 && uncompletedItems.isNotEmpty()) {
                val existingTitles = (scheduledForTomorrow + baseItems).map { it.title }.toSet()
                val toAdd = uncompletedItems.filter { it.title !in existingTitles }
                toAdd + scheduledForTomorrow + baseItems
            } else {
                scheduledForTomorrow + baseItems
            }
            p.copy(
                items = mergedItems,
                isCompleted = false,
                completedAt = null,
                reviewNote = null
            )
        }

        return current.copy(
            currentDate = tomorrow,
            phases = resetPhases,
            selectedPhaseIndex = 0,
            reviewLogs = updatedReviewLogs ?: current.reviewLogs,
            dailyRecords = updatedDailyRecords
        )
    }

    private fun createDailyRecord(date: String, phases: List<Phase>): DailyRecord {
        val totalItems = phases.sumOf { it.items.count { item -> !item.isDeleted } }
        val completedItems = phases.sumOf { p -> p.items.count { it.isCompleted && !it.isDeleted } }
        val reqPending = phases.sumOf { p -> p.items.count { it.priority == Priority.REQUIRED && !it.isCompleted && !it.isDeleted } }

        val summaries = phases.map { p ->
            val validItems = p.items.filterNot { it.isDeleted }
            PhaseDailySummary(
                phaseName = p.name,
                isCompleted = p.isCompleted,
                completedCount = validItems.count { it.isCompleted },
                totalCount = validItems.size,
                reviewNote = p.reviewNote,
                hadIssue = p.reviewNote != null
            )
        }

        return DailyRecord(
            dateString = date,
            completedAt = System.currentTimeMillis(),
            totalItems = totalItems,
            completedItems = completedItems,
            requiredPendingItems = reqPending,
            phaseSummaries = summaries
        )
    }

    // 時間ベースのフェーズ判定・自動進行
    fun checkAndAdvanceByTime() {
        val current = _appState.value
        if (!current.settings.autoAdvanceByTime) return

        val now = LocalTime.now()
        val schedules = current.settings.phaseSchedules.sortedBy { it.startHour * 60 + it.startMinute }
        if (schedules.isEmpty()) return

        val currentMinutes = now.hour * 60 + now.minute
        var matchedIndex = 0
        for (schedule in schedules) {
            val schedMinutes = schedule.startHour * 60 + schedule.startMinute
            if (currentMinutes >= schedMinutes && schedule.phaseIndex in current.phases.indices) {
                matchedIndex = schedule.phaseIndex
            }
        }

        if (matchedIndex != current.selectedPhaseIndex && matchedIndex in current.phases.indices) {
            selectPhase(matchedIndex)
        }
    }

    fun advanceToNextDay() {
        val current = _appState.value
        val today = current.currentDate.ifEmpty { getTodayDateString() }

        // 未完了タスクを取得（REQ / NORM）
        val uncompletedItems = mutableListOf<ChecklistItem>()
        if (current.settings.carryoverUnfinishedReq) {
            uncompletedItems.addAll(
                current.phases.flatMap { p ->
                    p.items.filter { it.priority == Priority.REQUIRED && !it.isCompleted && !it.isDeleted }.map {
                        it.copy(isCarryover = true, sourcePhaseName = p.name)
                    }
                }
            )
        }
        if (current.settings.carryoverUnfinishedNorm) {
            uncompletedItems.addAll(
                current.phases.flatMap { p ->
                    p.items.filter { it.priority == Priority.NORMAL && !it.isCompleted && !it.isDeleted }.map {
                        it.copy(isCarryover = true, sourcePhaseName = p.name)
                    }
                }
            )
        }

        val nextDayState = buildNextDayState(
            current = current,
            basePhases = current.phases,
            uncompletedItems = uncompletedItems,
            today = today
        )
        persist(nextDayState)
    }

    fun resetPhase(phaseId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                phase.copy(
                    isCompleted = false,
                    completedAt = null,
                    reviewNote = null,
                    items = phase.items.filterNot { it.isDeleted }.map { it.copy(isCompleted = false, completedAt = null) }
                )
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    fun resetAllPhases() {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            phase.copy(
                isCompleted = false,
                completedAt = null,
                reviewNote = null,
                items = phase.items.filterNot { it.isDeleted }.map { it.copy(isCompleted = false, completedAt = null) }
            )
        }
        persist(current.copy(phases = updatedPhases, selectedPhaseIndex = 0))
    }

    fun restoreDefaultFourPhases() {
        persist(createDefaultState())
    }

    fun applyTemplate(phaseId: String, templateId: String, replace: Boolean = false) {
        val current = _appState.value
        val template = current.templates.find { it.id == templateId } ?: return
        val newItems = template.items.map {
            ChecklistItem(title = it.title, priority = it.priority)
        }

        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val combined = if (replace) newItems else phase.items + newItems
                phase.copy(items = combined)
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    fun savePhaseAsTemplate(phaseId: String, templateName: String) {
        val trimmed = templateName.trim()
        if (trimmed.isEmpty()) return
        val current = _appState.value
        val phase = current.phases.find { it.id == phaseId } ?: return

        val templateItems = phase.items.filterNot { it.isDeleted }.map {
            TemplateItem(title = it.title, priority = it.priority)
        }
        val newTemplate = PhaseTemplate(name = trimmed, items = templateItems)
        persist(current.copy(templates = current.templates + newTemplate))
    }

    fun saveTemplate(template: PhaseTemplate) {
        val current = _appState.value
        val existingIndex = current.templates.indexOfFirst { it.id == template.id }
        val updatedTemplates = if (existingIndex >= 0) {
            current.templates.toMutableList().apply { set(existingIndex, template) }
        } else {
            current.templates + template
        }

        val templateIndex = updatedTemplates.indexOfFirst { it.id == template.id }

        // テンプレートを編集したら即座に現在いるフェーズ以降に反映
        val updatedPhases = current.phases.mapIndexed { pIdx, phase ->
            val isMatching = (pIdx == templateIndex) ||
                    (phase.name.contains("朝") && template.name.contains("朝")) ||
                    (phase.name.contains("午前") && template.name.contains("午前")) ||
                    (phase.name.contains("午後") && template.name.contains("午後")) ||
                    (phase.name.contains("夜") && template.name.contains("夜"))

            if (isMatching && pIdx >= current.selectedPhaseIndex && !phase.isCompleted) {
                val completedOrSpecial = phase.items.filter { it.isCompleted || it.isCarryover || it.scheduledDate != null }
                val newTemplateItems = template.items.map { tItem ->
                    completedOrSpecial.find { it.title == tItem.title } ?: ChecklistItem(
                        title = tItem.title,
                        priority = tItem.priority
                    )
                }
                val templateTitles = template.items.map { it.title }.toSet()
                val extraItems = phase.items.filter { it.title !in templateTitles && (it.isCompleted || it.isCarryover || it.scheduledDate != null) }
                phase.copy(items = (newTemplateItems + extraItems).distinctBy { it.id })
            } else {
                phase
            }
        }

        persist(current.copy(templates = updatedTemplates, phases = updatedPhases))
    }

    fun deleteTemplate(templateId: String) {
        val current = _appState.value
        persist(current.copy(templates = current.templates.filterNot { it.id == templateId }))
    }

    fun resetToToday() {
        val today = getTodayDateString()
        val current = _appState.value
        if (current.currentDate != today) {
            persist(current.copy(currentDate = today))
        }
    }

    fun setShowReqNotification(enabled: Boolean) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(showReqNotification = enabled)))
    }

    // 設定更新
    fun setThemeMode(mode: ThemeMode) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(themeMode = mode)))
    }

    fun setAppLanguage(language: AppLanguage) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(appLanguage = language)))
    }

    fun setPhaseEndReminderMinutes(minutes: Int) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(phaseEndReminderMinutes = minutes)))
    }

    fun setAutoAdvanceByTime(enabled: Boolean) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(autoAdvanceByTime = enabled)))
    }

    fun setCarryoverUnfinishedReq(enabled: Boolean) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(carryoverUnfinishedReq = enabled)))
    }

    fun setCarryoverUnfinishedNorm(enabled: Boolean) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(carryoverUnfinishedNorm = enabled)))
    }

    fun setConfirmReviewOnAdvance(enabled: Boolean) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(confirmReviewOnAdvance = enabled)))
    }

    fun updatePhaseSchedules(schedules: List<PhaseSchedule>) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(phaseSchedules = schedules)))
    }

    fun updatePhaseScheduleTime(phaseIndex: Int, startHour: Int, startMinute: Int) {
        val current = _appState.value
        val updated = current.settings.phaseSchedules.map { s ->
            if (s.phaseIndex == phaseIndex) s.copy(startHour = startHour, startMinute = startMinute) else s
        }
        persist(current.copy(settings = current.settings.copy(phaseSchedules = updated)))
    }

    fun applyPresetTemplates() {
        val presets = getDefaultPresetTemplates()
        val current = _appState.value
        persist(current.copy(templates = presets))
    }

    fun resetAllData() {
        val defaultState = createDefaultState()
        persist(defaultState)
    }
}
