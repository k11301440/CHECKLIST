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

    private fun loadInitialState(): AppState {
        return try {
            if (dataFile.exists()) {
                val text = dataFile.readText()
                val loaded = json.decodeFromString<AppState>(text)
                // 旧仕様の3フェーズ（「日中」を含む等）からの自動マイグレーション
                if (loaded.phases.any { it.name == "日中" } || loaded.phases.size < 4) {
                    val migrated = createDefaultState().copy(
                        templates = if (loaded.templates.isNotEmpty()) loaded.templates else createDefaultState().templates,
                        reviewLogs = loaded.reviewLogs
                    )
                    persist(migrated)
                    migrated
                } else {
                    loaded
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
            phases = defaultPhases,
            selectedPhaseIndex = 0,
            templates = defaultTemplates,
            reviewLogs = emptyList()
        )
    }

    private fun persist(newState: AppState) {
        _appState.value = newState
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val encoded = json.encodeToString(newState)
                    dataFile.writeText(encoded)
                }
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

    // タップ1回で完了
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

    // 長押しで取り消し
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

    // タスク追加（常に実行可能）
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

    // タスク更新（常に編集可能）
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

    // タスク削除
    fun removeItem(phaseId: String, itemId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                phase.copy(items = phase.items.filterNot { it.id == itemId })
            } else phase
        }
        persist(current.copy(phases = updatedPhases))
    }

    // 完了済み単発タスクの一括クリーンアップ
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
        val current = _appState.value
        if (trimmed.isEmpty() || current.phases.size >= 6) {
            return false // 最大6フェーズまで許容
        }
        val newPhase = Phase(name = trimmed)
        val updatedPhases = current.phases + newPhase
        persist(current.copy(phases = updatedPhases))
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
    fun completePhase(phaseId: String, hadIssue: Boolean, note: String?) {
        val current = _appState.value
        val targetPhase = current.phases.find { it.id == phaseId } ?: return

        val newLog = ReviewLog(
            timestamp = System.currentTimeMillis(),
            phaseName = targetPhase.name,
            hadIssue = hadIssue,
            note = if (hadIssue) note?.trim()?.ifEmpty { null } else null
        )

        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                phase.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    reviewNote = newLog.note
                )
            } else phase
        }

        // 次の未完了フェーズへ自動遷移
        val nextIndex = (current.selectedPhaseIndex + 1).coerceAtMost(current.phases.size - 1)

        persist(
            current.copy(
                phases = updatedPhases,
                reviewLogs = listOf(newLog) + current.reviewLogs,
                selectedPhaseIndex = nextIndex
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

    // デフォルト（朝・午前・午後・夜）の4フェーズ構成にリセット
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
}
