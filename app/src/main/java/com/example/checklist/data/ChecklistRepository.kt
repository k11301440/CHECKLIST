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
import java.util.concurrent.atomic.AtomicLong
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

    // ファイル書き込みは単一並列度のディスパッチャで直列化 (書き込み順序の逆転・tmpファイル競合を防止)
    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val writeVersion = AtomicLong(0)

    // 初期読み込み時にマイグレーション等で保存が必要になったか (_appState 初期化前に persist を呼ばないためのフラグ)
    private var initialStateNeedsSave = false

    private val _appState = MutableStateFlow(loadInitialState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        if (initialStateNeedsSave) {
            persist(_appState.value)
        }
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

        val migratedSettings = run {
            val base = if (loaded.settings.darkThemeConfig == DarkThemeConfig.DARK &&
                loaded.settings.accentTheme == AccentTheme.MONOCHROME &&
                loaded.settings.themeMode != ThemeMode.SPACE_BLACK
            ) {
                when (loaded.settings.themeMode) {
                    ThemeMode.SPACE_WHITE -> loaded.settings.copy(darkThemeConfig = DarkThemeConfig.LIGHT, accentTheme = AccentTheme.MONOCHROME)
                    ThemeMode.TRON_RED -> loaded.settings.copy(darkThemeConfig = DarkThemeConfig.DARK, accentTheme = AccentTheme.TRON)
                    ThemeMode.MONSTER_GREEN -> loaded.settings.copy(darkThemeConfig = DarkThemeConfig.DARK, accentTheme = AccentTheme.MONSTER)
                    ThemeMode.ANA_BLUE -> loaded.settings.copy(darkThemeConfig = DarkThemeConfig.DARK, accentTheme = AccentTheme.ANA)
                    ThemeMode.JAL_RED -> loaded.settings.copy(darkThemeConfig = DarkThemeConfig.DARK, accentTheme = AccentTheme.JAL)
                    ThemeMode.SYSTEM -> loaded.settings.copy(darkThemeConfig = DarkThemeConfig.SYSTEM, accentTheme = AccentTheme.MONOCHROME)
                }
            } else {
                loaded.settings
            }
            if (base.useLargeButtons && base.uiSize == UiSize.MEDIUM) {
                base.copy(uiSize = UiSize.LARGE)
            } else {
                base
            }
        }

        val stateWithDate = (if (loaded.currentDate.isEmpty()) {
            loaded.copy(currentDate = today)
        } else {
            loaded
        }).copy(settings = migratedSettings)

        // フェーズが空、または旧来の「日中」が含まれる場合のみマイグレーション (3フェーズや5フェーズのカスタムは保護)
        val base = if (stateWithDate.phases.isEmpty() || stateWithDate.phases.any { it.name == "日中" }) {
            initialStateNeedsSave = true
            createDefaultState().copy(
                currentDate = today,
                templates = if (stateWithDate.templates.isNotEmpty()) stateWithDate.templates else createDefaultState().templates,
                reviewLogs = stateWithDate.reviewLogs,
                dailyRecords = stateWithDate.dailyRecords,
                settings = stateWithDate.settings,
                scheduledItems = stateWithDate.scheduledItems
            )
        } else {
            stateWithDate
        }

        // 読み込み時にも整合エンジンを適用 (旧バージョンで保存された未来日付タスクは予約リストへ移行)
        val migrated = TaskSync.extractFutureScheduled(base)
        if (migrated !== base) initialStateNeedsSave = true
        return TaskSync.reconcile(TaskSync.releaseDueScheduled(migrated, migrated.currentDate))
    }

    companion object {
        @Volatile
        private var INSTANCE: ChecklistRepository? = null

        fun getInstance(context: Context): ChecklistRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChecklistRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

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

    // 全フェーズを横断して同一タスクの状態を自動整合するエンジン (Single Source of Truth 保証)
    // 同一タスクの判定は ID のみ (詳細は TaskSync.reconcile を参照)
    private fun reconcileTaskStates(state: AppState): AppState = TaskSync.reconcile(state)

    private fun persist(newState: AppState) {
        val reconciled = reconcileTaskStates(newState)
        _appState.value = reconciled
        val version = writeVersion.incrementAndGet()
        scope.launch(writeDispatcher) {
            // より新しい状態の書き込みが既にキューにある場合は古いスナップショットを書かない
            if (version != writeVersion.get()) return@launch
            try {
                val jsonString = json.encodeToString(reconciled)
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
                ReqNotificationHelper.updateNotification(context, reconciled)
                ReqNotificationHelper.schedulePhaseReminder(context, reconciled)
                com.example.checklist.widget.WidgetUpdateHelper.updateAllWidgetsSync(context, reconciled)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectPhase(index: Int) {
        val current = _appState.value
        if (index in current.phases.indices) {
            val updatedPhases = TaskSync.propagateCarryoverUpTo(current.phases, index, current.settings)
            persist(current.copy(phases = updatedPhases, selectedPhaseIndex = index))
        }
    }

    // 同一タスク判定ヘルパー。キャリーオーバーは ID を保持してコピーされるため ID 一致のみで判定する。
    // (旧実装はタイトル一致でも同期しており、意図的に作った同名の独立タスクが連動するバグがあった)
    private fun isSameOrCarryoverTask(item: ChecklistItem, targetItem: ChecklistItem): Boolean =
        item.id == targetItem.id

    private fun findItemAcrossPhases(itemId: String): ChecklistItem? {
        val current = _appState.value
        for (phase in current.phases) {
            val item = phase.items.find { it.id == itemId }
            if (item != null) return item
        }
        return null
    }

    fun completeItem(phaseId: String, itemId: String) {
        val current = _appState.value
        findItemAcrossPhases(itemId) ?: return
        val now = System.currentTimeMillis()
        val updatedPhases = TaskSync.completeItemInPhase(current.phases, phaseId, itemId, now)
        persist(current.copy(phases = updatedPhases))
    }

    fun undoItem(phaseId: String, itemId: String) {
        val current = _appState.value
        val targetItem = findItemAcrossPhases(itemId) ?: return

        // キャリーオーバー元・先含め、同一実体のタスクを全フェーズで未完了に戻す
        val undonePhases = current.phases.map { phase ->
            val updatedItems = phase.items.map { item ->
                if (isSameOrCarryoverTask(item, targetItem)) {
                    item.copy(isCompleted = false, completedAt = null)
                } else item
            }
            phase.copy(items = updatedItems)
        }
        val updatedPhases = TaskSync.propagateCarryoverUpTo(undonePhases, current.selectedPhaseIndex, current.settings)
        persist(current.copy(phases = updatedPhases))
    }

    fun addItem(phaseId: String, title: String, priority: Priority, scheduledDate: String? = null) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val current = _appState.value
        val phaseIdx = current.phases.indexOfFirst { it.id == phaseId }.coerceAtLeast(0)
        val newItem = ChecklistItem(
            title = trimmed,
            priority = priority,
            originPhaseIndex = phaseIdx,
            scheduledDate = scheduledDate
        )
        // 未来日付のタスクは指定日まで予約リストに保持 (今日のフェーズには表示しない)
        val today = current.currentDate.ifEmpty { getTodayDateString() }
        if (scheduledDate != null && scheduledDate > today) {
            val targetPhaseId = current.phases.getOrNull(phaseIdx)?.id ?: phaseId
            persist(current.copy(scheduledItems = current.scheduledItems + ScheduledTask(targetPhaseId, newItem)))
            return
        }
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val currentItems = phase.items.toMutableList()
                val insertIndex = if (priority == Priority.REQUIRED) {
                    val firstActiveNormIndex = currentItems.indexOfFirst {
                        !it.isCompleted && !it.isDeleted && it.priority == Priority.NORMAL
                    }
                    if (firstActiveNormIndex != -1) {
                        firstActiveNormIndex
                    } else {
                        val firstInactiveIndex = currentItems.indexOfFirst { it.isCompleted || it.isDeleted }
                        if (firstInactiveIndex != -1) firstInactiveIndex else currentItems.size
                    }
                } else {
                    val firstInactiveIndex = currentItems.indexOfFirst { it.isCompleted || it.isDeleted }
                    if (firstInactiveIndex != -1) firstInactiveIndex else currentItems.size
                }
                currentItems.add(insertIndex, newItem)
                phase.copy(items = currentItems)
            } else phase
        }
        val propagated = TaskSync.propagateCarryoverUpTo(updatedPhases, current.selectedPhaseIndex, current.settings)
        persist(current.copy(phases = propagated))
    }

    fun updateItem(phaseId: String, itemId: String, newTitle: String, newPriority: Priority) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        val current = _appState.value
        val targetItem = findItemAcrossPhases(itemId) ?: return

        val updatedPhases = current.phases.map { phase ->
            val updatedItems = phase.items.map { item ->
                if (isSameOrCarryoverTask(item, targetItem)) {
                    item.copy(title = trimmed, priority = newPriority)
                } else item
            }
            phase.copy(items = updatedItems)
        }
        val propagated = TaskSync.propagateCarryoverUpTo(updatedPhases, current.selectedPhaseIndex, current.settings)
        persist(current.copy(phases = propagated))
    }

    // 誤破棄防止のため即時削除ではなく保管（isDeleted = true）- 全フェーズで同期
    fun removeItem(phaseId: String, itemId: String) {
        val current = _appState.value
        findItemAcrossPhases(itemId) ?: return
        val updatedPhases = TaskSync.removeItemInPhase(current.phases, phaseId, itemId)
        persist(current.copy(phases = updatedPhases))
    }

    // 削除されたアイテムを復元 - 全フェーズで同期
    fun restoreItem(phaseId: String, itemId: String) {
        val current = _appState.value
        val targetItem = findItemAcrossPhases(itemId) ?: return

        val restoredPhases = current.phases.map { phase ->
            val updatedItems = phase.items.map { item ->
                if (isSameOrCarryoverTask(item, targetItem)) {
                    item.copy(isDeleted = false)
                } else item
            }
            phase.copy(items = updatedItems)
        }
        val updatedPhases = TaskSync.propagateCarryoverUpTo(restoredPhases, current.selectedPhaseIndex, current.settings)
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

    // 完了タスクの消去 (対象フェーズおよびキャリーオーバー関係にあるタスクを全フェーズから完全に整理)
    fun clearCompletedItems(phaseId: String) {
        val current = _appState.value
        val targetPhase = current.phases.find { it.id == phaseId } ?: return
        val clearedIds = targetPhase.items.filter { it.isCompleted || it.isDeleted }.map { it.id }.toSet()
        if (clearedIds.isEmpty()) return

        // ID 一致のインスタンス (キャリーオーバー元・先) のみ除去。同名の独立タスクには影響させない
        val updatedPhases = current.phases.map { phase ->
            phase.copy(items = phase.items.filterNot { it.id in clearedIds })
        }
        persist(current.copy(phases = updatedPhases))
    }

    // 破棄されたタスクの完全抹消 (ゴミ箱からの完全削除・全フェーズから永久除去)
    fun permanentlyDeleteItem(phaseId: String, itemId: String) {
        val current = _appState.value
        val updatedPhases = current.phases.map { phase ->
            phase.copy(items = phase.items.filterNot { it.id == itemId })
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
        val newIndex = when {
            targetIndex < current.selectedPhaseIndex -> (current.selectedPhaseIndex - 1).coerceAtLeast(0)
            else -> current.selectedPhaseIndex.coerceAtMost(updatedPhases.size - 1)
        }

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
        val targetIndex = current.phases.indexOfFirst { it.id == phaseId }
        if (targetIndex < 0) return
        val oldName = current.phases[targetIndex].name

        val updatedPhases = current.phases.map { phase ->
            val renamedPhase = if (phase.id == phaseId) phase.copy(name = trimmed) else phase
            val updatedItems = renamedPhase.items.map { item ->
                if (item.isCarryover && item.sourcePhaseName == oldName) {
                    item.copy(sourcePhaseName = trimmed)
                } else item
            }
            renamedPhase.copy(items = updatedItems)
        }
        val updatedSchedules = current.settings.phaseSchedules.map { s ->
            if (s.phaseIndex == targetIndex) s.copy(phaseName = trimmed) else s
        }
        persist(
            current.copy(
                phases = updatedPhases,
                settings = current.settings.copy(phaseSchedules = updatedSchedules)
            )
        )
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

        // 同一日・同一フェーズの既存ログを重複排除して最新1件に更新 (上限件数でGC保護)
        val cleanedLogs = current.reviewLogs.filterNot {
            it.dateString == today && it.phaseName == targetPhase.name
        }
        val updatedReviewLogs = (listOf(newLog) + cleanedLogs).take(TaskSync.MAX_REVIEW_LOGS)

        // 最後のフェーズ（夜）かどうか
        val isLastPhase = targetPhaseIndex >= current.phases.size - 1

        if (isLastPhase) {
            // 夜セクション完了 -> 全フェーズの未完了持越対象を集約し、本日の日別レコードを保存して翌日の朝へ進む
            val dayCarryoverItems = TaskSync.collectDayCarryover(phasesAfterTargetCompletion, current.settings)
            val nextDayState = buildNextDayState(
                current = current,
                basePhases = phasesAfterTargetCompletion,
                uncompletedItems = dayCarryoverItems,
                today = today,
                updatedReviewLogs = updatedReviewLogs
            )
            persist(nextDayState)
        } else {
            // 通常のフェーズ完了 -> 次のフェーズへ進み、未完了タスクを次のフェーズへキャリーオーバー（優先度高いため上部に表示）
            val nextIndex = targetPhaseIndex + 1
            val propagatedPhases = TaskSync.propagateCarryoverUpTo(
                phases = phasesAfterTargetCompletion,
                targetIndex = nextIndex,
                settings = current.settings
            )

            persist(
                current.copy(
                    phases = propagatedPhases,
                    selectedPhaseIndex = nextIndex,
                    reviewLogs = updatedReviewLogs
                )
            )
        }
    }

    fun isTemplateApplicable(template: PhaseTemplate, date: LocalDate): Boolean =
        TaskSync.isTemplateApplicable(template, date)

    private fun buildNextDayState(
        current: AppState,
        basePhases: List<Phase>,
        uncompletedItems: List<ChecklistItem>,
        today: String,
        updatedReviewLogs: List<ReviewLog>? = null
    ): AppState = TaskSync.buildNextDayState(current, basePhases, uncompletedItems, today, updatedReviewLogs)

    // 直近の時刻判定で観測した時間帯フェーズIndex (-1 = 未初期化/初回起動時)
    // 手動で過去・未来フェーズを閲覧中に定期チェックやON_RESUMEが走っても、
    // 実時刻が次のフェーズ境界を跨ぐまでは勝手に引き戻さないために保持する
    @Volatile
    private var lastObservedTimePhaseIndex: Int = -1

    fun computeCurrentPhaseIndexByTime(
        state: AppState = _appState.value,
        now: LocalTime = LocalTime.now()
    ): Int = TaskSync.computeCurrentPhaseIndexByTime(state, now)

    // 時間ベースのフェーズ判定・自動進行
    fun checkAndAdvanceByTime(forceSync: Boolean = false) {
        val current = _appState.value
        if (current.phases.isEmpty()) return

        val today = getTodayDateString()
        val matchedIndex = computeCurrentPhaseIndexByTime(current)
        val previousObservedIndex = lastObservedTimePhaseIndex
        lastObservedTimePhaseIndex = matchedIndex

        if (current.currentDate.isNotEmpty() && current.currentDate < today) {
            // 日付をまたいだ場合は、自動進行設定のON/OFFに関わらず新しい日へロールオーバーする
            // (自動進行ONなら現在時刻のフェーズへ、OFFなら朝(Phase 0)へ進める)
            val targetIndex = if (current.settings.autoAdvanceByTime) matchedIndex else 0
            persist(TaskSync.advanceToDateAndPhase(current, today, targetIndex))
            return
        }

        if (!current.settings.autoAdvanceByTime) return

        // すでに翌日以降へ手動で進んでいる場合は、当日の時刻で勝手にフェーズを変更しない
        if (current.currentDate.isNotEmpty() && current.currentDate > today) return

        // 初回起動時(previousObservedIndex == -1)、明示的な強制同期(forceSync)、
        // または実時刻がフェーズ境界を跨いで時間帯が変化した場合のみ自動遷移する。
        // (ユーザーが手動で別フェーズを見ている最中に同じ時間帯の定期チェックで引き戻されるのを防ぐ)
        val timeBoundaryCrossed = forceSync || previousObservedIndex == -1 || matchedIndex != previousObservedIndex
        if (!timeBoundaryCrossed) return

        // 現在時刻のフェーズ〜選択中フェーズの直前までが全て完了済み（先取り進行中）の場合は引き戻さない
        val isAhead = TaskSync.isAheadBecauseCompleted(
            phases = current.phases,
            selectedPhaseIndex = current.selectedPhaseIndex,
            currentPhaseIndexByTime = matchedIndex
        )
        if (matchedIndex != current.selectedPhaseIndex && !isAhead && matchedIndex in current.phases.indices) {
            selectPhase(matchedIndex)
        }
    }

    /** 「現在のフェーズに戻る」: 日付(今日)と時間帯(現在時刻のフェーズ)の両方を現在に合わせ、未完了持越も伝播する */
    fun returnToCurrentPhase() {
        val current = _appState.value
        if (current.phases.isEmpty()) return
        val today = getTodayDateString()
        val matchedIndex = computeCurrentPhaseIndexByTime(current)
        persist(TaskSync.advanceToDateAndPhase(current, today, matchedIndex))
    }

    fun advanceToNextDay() {
        val current = _appState.value
        val today = current.currentDate.ifEmpty { getTodayDateString() }

        // 未完了タスクを取得（REQ / NORM）。キャリーオーバーで複数フェーズにある同一タスクは1件に集約
        val uncompletedItems = TaskSync.collectDayCarryover(current.phases, current.settings)

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
        val target = current.phases.find { it.id == phaseId } ?: return
        // 対象フェーズのタスクと同一IDのインスタンス (キャリーオーバー元・先) も未完了に戻す。
        // 片側だけ戻すと整合エンジンの完了OR合成で即座に完了へ巻き戻るため。
        val resetIds = target.items.filterNot { it.isDeleted }.map { it.id }.toSet()
        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                phase.copy(
                    isCompleted = false,
                    completedAt = null,
                    reviewNote = null,
                    items = phase.items.filterNot { it.isDeleted }.map { it.copy(isCompleted = false, completedAt = null) }
                )
            } else if (phase.items.any { it.id in resetIds }) {
                phase.copy(items = phase.items.map {
                    if (it.id in resetIds) it.copy(isCompleted = false, completedAt = null) else it
                })
            } else phase
        }
        val propagated = TaskSync.propagateCarryoverUpTo(updatedPhases, current.selectedPhaseIndex, current.settings)
        persist(current.copy(phases = propagated))
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
        val phaseIdx = current.phases.indexOfFirst { it.id == phaseId }.coerceAtLeast(0)
        val newItems = template.items.map {
            ChecklistItem(title = it.title, priority = it.priority, originPhaseIndex = phaseIdx)
        }

        val updatedPhases = current.phases.map { phase ->
            if (phase.id == phaseId) {
                val combined = if (replace) {
                    newItems
                } else {
                    val existingTitles = phase.items.filterNot { it.isDeleted }.map { it.title }.toSet()
                    phase.items + newItems.filter { it.title !in existingTitles }
                }
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
        val activeDate = runCatching { LocalDate.parse(current.currentDate) }.getOrElse { LocalDate.now() }
        val applicableToday = isTemplateApplicable(template, activeDate)

        // テンプレートを編集したら、当日の適用条件に合致する場合のみ現在いるフェーズ以降に反映 (既存アイテムのIDは維持)
        val updatedPhases = current.phases.mapIndexed { pIdx, phase ->
            val isMatching = (pIdx == templateIndex) ||
                    (phase.name.contains("朝") && template.name.contains("朝")) ||
                    (phase.name.contains("午前") && template.name.contains("午前")) ||
                    (phase.name.contains("午後") && template.name.contains("午後")) ||
                    (phase.name.contains("夜") && template.name.contains("夜"))

            if (applicableToday && isMatching && pIdx >= current.selectedPhaseIndex && !phase.isCompleted) {
                val existingValid = phase.items.filterNot { it.isDeleted }
                val newTemplateItems = template.items.map { tItem ->
                    val existing = existingValid.find { it.title == tItem.title }
                    existing?.copy(priority = tItem.priority) ?: ChecklistItem(
                        title = tItem.title,
                        priority = tItem.priority,
                        originPhaseIndex = pIdx
                    )
                }
                val templateTitles = template.items.map { it.title }.toSet()
                val extraItems = phase.items.filter {
                    it.title !in templateTitles && (it.isCompleted || it.isCarryover || it.scheduledDate != null)
                }
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
            val targetIndex = if (current.settings.autoAdvanceByTime) {
                computeCurrentPhaseIndexByTime(current)
            } else {
                current.selectedPhaseIndex
            }
            persist(TaskSync.advanceToDateAndPhase(current, today, targetIndex))
        }
    }

    fun setDate(dateString: String) {
        val current = _appState.value
        if (current.currentDate != dateString) {
            persist(TaskSync.advanceToDateAndPhase(current, dateString, current.selectedPhaseIndex))
        }
    }

    fun returnToPreviousDay(dateString: String) {
        val current = _appState.value
        if (current.phases.isEmpty()) return
        persist(TaskSync.advanceToDateAndPhase(current, dateString, current.phases.lastIndex))
    }

    fun setShowReqNotification(enabled: Boolean) {
        val current = _appState.value
        val mode = if (enabled) NotificationDisplayMode.COUNT_ONLY else NotificationDisplayMode.OFF
        persist(current.copy(settings = current.settings.copy(showReqNotification = enabled, notificationMode = mode)))
    }

    fun setNotificationMode(mode: NotificationDisplayMode) {
        val current = _appState.value
        val enabled = mode != NotificationDisplayMode.OFF
        persist(current.copy(settings = current.settings.copy(notificationMode = mode, showReqNotification = enabled)))
    }

    fun setUiSize(size: UiSize) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(uiSize = size, useLargeButtons = (size == UiSize.LARGE))))
    }

    fun setUseLargeButtons(enabled: Boolean) {
        setUiSize(if (enabled) UiSize.LARGE else UiSize.MEDIUM)
    }

    // 設定更新
    fun setDarkThemeConfig(config: DarkThemeConfig) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(darkThemeConfig = config)))
    }

    fun setAccentTheme(theme: AccentTheme) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(accentTheme = theme)))
    }

    fun setEnableSwipePhase(enabled: Boolean) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(enableSwipePhase = enabled)))
    }

    fun setThemeMode(mode: ThemeMode) {
        val current = _appState.value
        val config = when (mode) {
            ThemeMode.SPACE_WHITE -> DarkThemeConfig.LIGHT
            ThemeMode.SYSTEM -> DarkThemeConfig.SYSTEM
            else -> DarkThemeConfig.DARK
        }
        val accent = when (mode) {
            ThemeMode.SPACE_BLACK, ThemeMode.SPACE_WHITE, ThemeMode.SYSTEM -> AccentTheme.MONOCHROME
            ThemeMode.TRON_RED -> AccentTheme.TRON
            ThemeMode.MONSTER_GREEN -> AccentTheme.MONSTER
            ThemeMode.ANA_BLUE -> AccentTheme.ANA
            ThemeMode.JAL_RED -> AccentTheme.JAL
        }
        persist(current.copy(settings = current.settings.copy(
            themeMode = mode,
            darkThemeConfig = config,
            accentTheme = accent
        )))
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
        if (enabled) {
            checkAndAdvanceByTime(forceSync = true)
        }
    }

    fun setCarryoverUnfinishedReq(enabled: Boolean) {
        val current = _appState.value
        val newSettings = current.settings.copy(carryoverUnfinishedReq = enabled)
        val updatedPhases = TaskSync.propagateCarryoverUpTo(current.phases, current.selectedPhaseIndex, newSettings)
        persist(current.copy(phases = updatedPhases, settings = newSettings))
    }

    fun setCarryoverUnfinishedNorm(enabled: Boolean) {
        val current = _appState.value
        val newSettings = current.settings.copy(carryoverUnfinishedNorm = enabled)
        val updatedPhases = TaskSync.propagateCarryoverUpTo(current.phases, current.selectedPhaseIndex, newSettings)
        persist(current.copy(phases = updatedPhases, settings = newSettings))
    }

    fun setConfirmReviewOnAdvance(enabled: Boolean) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(confirmReviewOnAdvance = enabled)))
    }

    fun updatePhaseSchedules(schedules: List<PhaseSchedule>) {
        val current = _appState.value
        persist(current.copy(settings = current.settings.copy(phaseSchedules = schedules)))
        checkAndAdvanceByTime(forceSync = true)
    }

    fun updatePhaseScheduleTime(phaseIndex: Int, startHour: Int, startMinute: Int) {
        val current = _appState.value
        val updated = current.settings.phaseSchedules.map { s ->
            if (s.phaseIndex == phaseIndex) s.copy(startHour = startHour, startMinute = startMinute) else s
        }
        persist(current.copy(settings = current.settings.copy(phaseSchedules = updated)))
        checkAndAdvanceByTime(forceSync = true)
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
