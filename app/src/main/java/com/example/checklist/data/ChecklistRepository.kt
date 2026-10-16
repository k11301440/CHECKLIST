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

class ChecklistRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val dataFile: File
        get() = File(context.filesDir, "checklist_state.json")

    private val _appState = MutableStateFlow(loadInitialState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private fun getTodayDateString(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun loadInitialState(): AppState {
        return try {
            val today = getTodayDateString()
            if (dataFile.exists()) {
                val text = dataFile.readText()
                val loaded = json.decodeFromString<AppState>(text)
                // マイグレーション & 日付チェック
                val stateWithDate = if (loaded.currentDate.isEmpty()) {
                    loaded.copy(currentDate = today)
                } else {
                    loaded
                }

                // 4フェーズ未満の場合はリセット
                if (stateWithDate.phases.size < 4 || stateWithDate.phases.any { it.name == "日中" }) {
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
            } else {
                createDefaultState()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            createDefaultState()
        }
    }

    private fun createDefaultState(): AppState {
        val today = getTodayDateString()
        val defaultTemplates = listOf(
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

        val defaultPhases = listOf(
            Phase(
                name = "朝",
                items = listOf(
                    ChecklistItem(title = "本日の最優先フォーカスタスク選定", priority = Priority.REQUIRED),
                    ChecklistItem(title = "スケジュール & 会議予定の確認", priority = Priority.REQUIRED),
                    ChecklistItem(title = "重要メール・未読チャット一次確認", priority = Priority.NORMAL),
                    ChecklistItem(title = "作業環境・デスクの整備", priority = Priority.NORMAL)
                )
            ),
            Phase(
                name = "午前",
                items = listOf(
                    ChecklistItem(title = "最重要タスク (Deep Work) の着手・完遂", priority = Priority.REQUIRED),
                    ChecklistItem(title = "午前中のチーム連携・報連相", priority = Priority.NORMAL)
                )
            ),
            Phase(
                name = "午後",
                items = listOf(
                    ChecklistItem(title = "午後コア業務の推進・完了", priority = Priority.REQUIRED),
                    ChecklistItem(title = "外部連絡・打ち合わせの実施", priority = Priority.NORMAL),
                    ChecklistItem(title = "残タスクとブロッカーの特定", priority = Priority.NORMAL)
                )
            ),
            Phase(
                name = "夜",
                items = listOf(
                    ChecklistItem(title = "翌日スケジュールの策定 & タスク整理", priority = Priority.REQUIRED),
                    ChecklistItem(title = "本日の実績記録と日報・メモ作成", priority = Priority.NORMAL),
                    ChecklistItem(title = "作業端末・環境のクローズ", priority = Priority.NORMAL)
                )
            )
        )

        return AppState(
            currentDate = today,
            phases = defaultPhases,
            selectedPhaseIndex = 0,
            templates = defaultTemplates,
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
                dataFile.writeText(jsonString)
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

    fun addItem(phaseId: String, title: String, priority: Priority) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val current = _appState.value
        val newItem = ChecklistItem(title = trimmed, priority = priority)
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                phase.copy(items = phase.items + newItem)
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

    fun removeItem(phaseId: String, itemId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                phase.copy(items = phase.items.filterNot { it.id == itemId })
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    fun clearCompletedItems(phaseId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                phase.copy(items = phase.items.filterNot { it.isCompleted })
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
        persist(current.copy(phases = current.phases + newPhase))
        return true
    }

    fun removePhase(phaseId: String): Boolean {
        val current = _appState.value
        if (current.phases.size <= 1) return false
        val updatedPhases = current.phases.filterNot { it.id == phaseId }
        val newIndex = current.selectedPhaseIndex.coerceAtMost(updatedPhases.size - 1)
        persist(current.copy(phases = updatedPhases, selectedPhaseIndex = newIndex))
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
    // 夜フェーズの場合は翌朝へ進み、未完了REQタスクは持ち越し
    fun completePhase(phaseId: String, hadIssue: Boolean, note: String?) {
        val current = _appState.value
        val targetPhaseIndex = current.phases.indexOfFirst { it.id == phaseId }
        if (targetPhaseIndex < 0) return
        val targetPhase = current.phases[targetPhaseIndex]

        val today = getTodayDateString()
        val newLog = ReviewLog(
            timestamp = System.currentTimeMillis(),
            phaseName = targetPhase.name,
            hadIssue = hadIssue,
            note = if (hadIssue) note?.trim()?.ifEmpty { null } else null,
            dateString = today
        )

        // 対象フェーズを完了マーク
        val updatedPhase = targetPhase.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
            reviewNote = newLog.note
        )

        val phasesAfterTargetCompletion = current.phases.toMutableList().apply {
            set(targetPhaseIndex, updatedPhase)
        }

        // 未完了のREQUIREDタスクを取得
        val uncompletedReqItems = if (current.settings.carryoverUnfinishedReq) {
            targetPhase.items.filter { it.priority == Priority.REQUIRED && !it.isCompleted }.map {
                it.copy(
                    isCarryover = true,
                    sourcePhaseName = targetPhase.name
                )
            }
        } else {
            emptyList()
        }

        // 最後のフェーズ（夜）かどうか
        val isLastPhase = targetPhaseIndex >= current.phases.size - 1

        if (isLastPhase) {
            // 夜セクション完了 -> 本日の日別レコードを保存し、翌日の朝へ進む
            val dailyRecord = createDailyRecord(today, phasesAfterTargetCompletion)
            val updatedDailyRecords = current.dailyRecords + (today to dailyRecord)

            // 翌日（今日+1日）へ日付更新
            val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

            // 各フェーズを初期化し、朝フェーズ（インデックス0）に未完了REQタスクを引き継ぐ
            val resetPhases = phasesAfterTargetCompletion.mapIndexed { index, p ->
                val baseItems = p.items.map { it.copy(isCompleted = false, completedAt = null) }
                val newItems = if (index == 0 && uncompletedReqItems.isNotEmpty()) {
                    // 朝フェーズに引き継ぎアイテムを追加（重複防止）
                    val existingTitles = baseItems.map { it.title }.toSet()
                    val itemsToAdd = uncompletedReqItems.filter { it.title !in existingTitles }
                    baseItems + itemsToAdd
                } else {
                    baseItems
                }
                p.copy(
                    items = newItems,
                    isCompleted = false,
                    completedAt = null,
                    reviewNote = null
                )
            }

            persist(
                current.copy(
                    currentDate = tomorrow,
                    phases = resetPhases,
                    selectedPhaseIndex = 0,
                    reviewLogs = listOf(newLog) + current.reviewLogs,
                    dailyRecords = updatedDailyRecords
                )
            )
        } else {
            // 通常のフェーズ完了 -> 次のフェーズへ進み、未完了REQを次のフェーズへキャリーオーバー
            val nextIndex = targetPhaseIndex + 1
            val nextPhase = phasesAfterTargetCompletion[nextIndex]

            val nextPhaseItems = if (uncompletedReqItems.isNotEmpty()) {
                val existingTitles = nextPhase.items.map { it.title }.toSet()
                val itemsToAdd = uncompletedReqItems.filter { it.title !in existingTitles }
                nextPhase.items + itemsToAdd
            } else {
                nextPhase.items
            }

            phasesAfterTargetCompletion[nextIndex] = nextPhase.copy(items = nextPhaseItems)

            persist(
                current.copy(
                    phases = phasesAfterTargetCompletion,
                    selectedPhaseIndex = nextIndex,
                    reviewLogs = listOf(newLog) + current.reviewLogs
                )
            )
        }
    }

    private fun createDailyRecord(date: String, phases: List<Phase>): DailyRecord {
        val totalItems = phases.sumOf { it.items.size }
        val completedItems = phases.sumOf { p -> p.items.count { it.isCompleted } }
        val reqPending = phases.sumOf { p -> p.items.count { it.priority == Priority.REQUIRED && !it.isCompleted } }

        val summaries = phases.map { p ->
            PhaseDailySummary(
                phaseName = p.name,
                isCompleted = p.isCompleted,
                completedCount = p.items.count { it.isCompleted },
                totalCount = p.items.size,
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
        val dailyRecord = createDailyRecord(today, current.phases)
        val updatedDailyRecords = current.dailyRecords + (today to dailyRecord)

        val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        // 未完了REQタスクを取得
        val uncompletedReq = if (current.settings.carryoverUnfinishedReq) {
            current.phases.flatMap { p ->
                p.items.filter { it.priority == Priority.REQUIRED && !it.isCompleted }.map {
                    it.copy(isCarryover = true, sourcePhaseName = p.name)
                }
            }
        } else emptyList()

        val resetPhases = current.phases.mapIndexed { index, p ->
            val baseItems = p.items.map { it.copy(isCompleted = false, completedAt = null) }
            val newItems = if (index == 0 && uncompletedReq.isNotEmpty()) {
                val existingTitles = baseItems.map { it.title }.toSet()
                val toAdd = uncompletedReq.filter { it.title !in existingTitles }
                baseItems + toAdd
            } else baseItems
            p.copy(
                items = newItems,
                isCompleted = false,
                completedAt = null,
                reviewNote = null
            )
        }

        persist(
            current.copy(
                currentDate = tomorrow,
                phases = resetPhases,
                selectedPhaseIndex = 0,
                dailyRecords = updatedDailyRecords
            )
        )
    }

    fun resetPhase(phaseId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                phase.copy(
                    isCompleted = false,
                    completedAt = null,
                    reviewNote = null,
                    items = phase.items.map { it.copy(isCompleted = false, completedAt = null) }
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
                items = phase.items.map { it.copy(isCompleted = false, completedAt = null) }
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

        val templateItems = phase.items.map {
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
        persist(current.copy(templates = updatedTemplates))
    }

    fun deleteTemplate(templateId: String) {
        val current = _appState.value
        persist(current.copy(templates = current.templates.filterNot { it.id == templateId }))
    }

    // 設定更新
    fun setThemeMode(mode: ThemeMode) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(themeMode = mode)))
    }

    fun setAutoAdvanceByTime(enabled: Boolean) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(autoAdvanceByTime = enabled)))
    }

    fun setCarryoverUnfinishedReq(enabled: Boolean) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(carryoverUnfinishedReq = enabled)))
    }

    fun updatePhaseSchedules(schedules: List<PhaseSchedule>) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(phaseSchedules = schedules)))
    }
}
