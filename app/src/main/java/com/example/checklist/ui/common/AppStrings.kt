package com.example.checklist.ui.common

import com.example.checklist.data.AppLanguage

object AppStrings {
    // Navigation & Headers
    fun checklist(lang: AppLanguage) = if (lang == AppLanguage.EN) "CHECKLIST" else "チェックリスト"
    fun home(lang: AppLanguage) = if (lang == AppLanguage.EN) "HOME" else "ホーム"
    fun settings(lang: AppLanguage) = if (lang == AppLanguage.EN) "SETTINGS" else "設定"
    fun calendar(lang: AppLanguage) = if (lang == AppLanguage.EN) "CALENDAR" else "カレンダー"
    fun tutorial(lang: AppLanguage) = if (lang == AppLanguage.EN) "TUTORIAL" else "チュートリアル"
    fun privacyPolicy(lang: AppLanguage) = if (lang == AppLanguage.EN) "PRIVACY POLICY" else "プライバシーポリシー"
    fun templates(lang: AppLanguage) = if (lang == AppLanguage.EN) "TEMPLATES" else "テンプレート"
    fun aviationPhilosophy(lang: AppLanguage) = if (lang == AppLanguage.EN) "AVIATION CHECKLIST PHILOSOPHY" else "航空チェックリストの思想"
    fun releaseNotes(lang: AppLanguage) = if (lang == AppLanguage.EN) "RELEASE NOTES" else "リリースノート"
    fun backToToday(lang: AppLanguage) = if (lang == AppLanguage.EN) "BACK TO TODAY" else "今日に戻る"

    // Priority Terms (REQ/NORM -> 重要/標準, CRIT/STD)
    fun priorityStandard(lang: AppLanguage) = if (lang == AppLanguage.EN) "STD" else "標準"
    fun priorityCritical(lang: AppLanguage) = if (lang == AppLanguage.EN) "CRIT" else "重要"
    fun priorityStandardFull(lang: AppLanguage) = if (lang == AppLanguage.EN) "STANDARD" else "標準タスク"
    fun priorityCriticalFull(lang: AppLanguage) = if (lang == AppLanguage.EN) "CRITICAL" else "重要タスク"

    // Task Screen
    fun activeTasks(lang: AppLanguage, count: Int) = if (lang == AppLanguage.EN) "ACTIVE TASKS" else "進行中タスク"
    fun completed(lang: AppLanguage, count: Int) = if (lang == AppLanguage.EN) "COMPLETED" else "完了済み"
    fun discarded(lang: AppLanguage, count: Int) = if (lang == AppLanguage.EN) "ARCHIVED / DISCARDED" else "破棄されたタスク"
    fun completedAndDiscarded(lang: AppLanguage) = if (lang == AppLanguage.EN) "COMPLETED & ARCHIVED" else "完了・破棄されたタスク"
    fun inputHint(lang: AppLanguage) = if (lang == AppLanguage.EN) "Enter new task..." else "新しいタスクを入力..."
    fun scheduleSelect(lang: AppLanguage) = if (lang == AppLanguage.EN) "SCHEDULE / PHASE SELECTION" else "開始日・フェーズ選択"
    fun today(lang: AppLanguage) = if (lang == AppLanguage.EN) "TODAY" else "今日"
    fun tomorrow(lang: AppLanguage) = if (lang == AppLanguage.EN) "TOMORROW" else "明日"
    fun dayAfterTomorrow(lang: AppLanguage) = if (lang == AppLanguage.EN) "IN 2 DAYS" else "明後日"
    fun completeDay(lang: AppLanguage) = if (lang == AppLanguage.EN) "COMPLETE DAY & RESTART" else "1日の全フェーズを完了"
    fun completePhase(lang: AppLanguage) = if (lang == AppLanguage.EN) "ADVANCE TO NEXT PHASE" else "次のフェーズへ"
    fun phaseCompletedReevaluate(lang: AppLanguage) = if (lang == AppLanguage.EN) "PHASE COMPLETED // RE-EVALUATE" else "フェーズ完了済み // 再確認"
    fun carryoverFrom(lang: AppLanguage, phase: String) = if (lang == AppLanguage.EN) "CARRYOVER // $phase" else "持越 // $phase"
    fun restore(lang: AppLanguage) = if (lang == AppLanguage.EN) "RESTORE" else "復元"
    fun noTasks(lang: AppLanguage) = if (lang == AppLanguage.EN) "NO TASKS IN THIS PHASE" else "このフェーズにはタスクがありません"
    fun noTasksHint(lang: AppLanguage) = if (lang == AppLanguage.EN) 
        "Add a new task using the input bar below, or load presets from Settings > Templates."
    else 
        "下部の入力バーから新しいタスクを追加するか、右上の設定 > テンプレートからプリセットを読み込んでください。"

    // Settings Screen
    fun sectionLanguage(lang: AppLanguage) = if (lang == AppLanguage.EN) "LANGUAGE" else "言語設定"
    fun sectionTheme(lang: AppLanguage) = if (lang == AppLanguage.EN) "COLOR THEME" else "カラーテーマ"
    fun sectionAutomation(lang: AppLanguage) = if (lang == AppLanguage.EN) "AUTOMATION" else "運用自動化"
    fun sectionNotifications(lang: AppLanguage) = if (lang == AppLanguage.EN) "NOTIFICATIONS & REMINDERS" else "通知・リマインダー"
    fun sectionPhases(lang: AppLanguage) = if (lang == AppLanguage.EN) "PHASES & SCHEDULE" else "フェーズ・時間割管理"
    fun sectionTemplates(lang: AppLanguage) = if (lang == AppLanguage.EN) "TEMPLATES" else "テンプレート管理"
    fun sectionGuide(lang: AppLanguage) = if (lang == AppLanguage.EN) "GUIDE & PHILOSOPHY" else "ガイドと設計思想"
    fun sectionWidgets(lang: AppLanguage) = if (lang == AppLanguage.EN) "WIDGETS" else "ウィジェット"
    fun sectionInfo(lang: AppLanguage) = if (lang == AppLanguage.EN) "APPLICATION INFO" else "アプリ情報"
    fun sectionDanger(lang: AppLanguage) = if (lang == AppLanguage.EN) "DANGER ZONE" else "データ管理"

    // Widget Strings
    fun widgetAddDesc(lang: AppLanguage) = if (lang == AppLanguage.EN) "Tap to pin widgets to your home screen" else "ホーム画面にウィジェットを追加します"
    fun widgetCriticalTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "Critical Tasks" else "重要タスク"
    fun widgetCriticalSub(lang: AppLanguage) = if (lang == AppLanguage.EN) "Pending critical tasks or count" else "重要タスクの一覧・件数を表示"
    fun widgetAllTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "All Tasks" else "全タスク"
    fun widgetAllSub(lang: AppLanguage) = if (lang == AppLanguage.EN) "All pending tasks or count" else "全タスクの一覧・件数を表示"
    fun widgetFocusTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "Phase & Focus" else "フェーズ＆フォーカス"
    fun widgetFocusSub(lang: AppLanguage) = if (lang == AppLanguage.EN) "Remaining time and top priority target" else "フェーズ残り時間と最優先タスクを表示"
    fun widgetPinButton(lang: AppLanguage) = if (lang == AppLanguage.EN) "ADD TO HOME" else "ホーム画面に追加"

    // Settings items
    fun autoAdvance(lang: AppLanguage) = if (lang == AppLanguage.EN) "Auto-advance phase by time" else "時間割によるフェーズ自動進行"
    fun autoAdvanceDesc(lang: AppLanguage) = if (lang == AppLanguage.EN) 
        "Automatically switch to current phase based on schedule" 
    else 
        "指定時刻になると該当フェーズへ自動で切り替えます"

    fun carryoverReq(lang: AppLanguage) = if (lang == AppLanguage.EN) "Carryover unfinished CRITICAL tasks" else "未完了の重要タスクを自動持越"
    fun carryoverReqDesc(lang: AppLanguage) = if (lang == AppLanguage.EN) 
        "Carry forward pending critical tasks to the next phase" 
    else 
        "フェーズ完了時、未完了の重要タスクを次のフェーズへ引き継ぎます"

    fun carryoverNorm(lang: AppLanguage) = if (lang == AppLanguage.EN) "Carryover unfinished STANDARD tasks" else "未完了の標準タスクを自動持越"
    fun carryoverNormDesc(lang: AppLanguage) = if (lang == AppLanguage.EN) 
        "Carry forward pending standard tasks to the next phase" 
    else 
        "フェーズ完了時、未完了の標準タスクも次のフェーズへ引き継ぎます"

    fun reqNotification(lang: AppLanguage) = if (lang == AppLanguage.EN) "Display unfinished CRITICAL in status bar" else "未完了の重要タスクを常駐通知"
    fun reqNotificationDesc(lang: AppLanguage) = if (lang == AppLanguage.EN) 
        "Show remaining critical tasks in Android notification bar" 
    else 
        "未完了の重要タスクがある場合、通知バーに常駐表示します"

    fun phaseEndReminder(lang: AppLanguage) = if (lang == AppLanguage.EN) "Phase end reminder" else "フェーズ終了前リマインダー"
    fun phaseEndReminderDesc(lang: AppLanguage) = if (lang == AppLanguage.EN) 
        "Notify before current phase ends to check remaining tasks" 
    else 
        "フェーズ終了の一定時間前に通知でリマインドします"

    fun resetAllData(lang: AppLanguage) = if (lang == AppLanguage.EN) "RESET ALL DATA" else "全データ初期化"
    fun resetAllDataConfirm(lang: AppLanguage) = if (lang == AppLanguage.EN) 
        "Are you sure you want to initialize all tasks, phases, and records? This cannot be undone."
    else 
        "すべてのタスク、フェーズ、履歴データを初期化しますか？この操作は取り消せません。"

    // Buttons
    fun save(lang: AppLanguage) = if (lang == AppLanguage.EN) "SAVE" else "保存"
    fun cancel(lang: AppLanguage) = if (lang == AppLanguage.EN) "CANCEL" else "キャンセル"
    fun delete(lang: AppLanguage) = if (lang == AppLanguage.EN) "DELETE" else "削除"
    fun close(lang: AppLanguage) = if (lang == AppLanguage.EN) "CLOSE" else "閉じる"
    fun edit(lang: AppLanguage) = if (lang == AppLanguage.EN) "EDIT" else "編集"
    fun apply(lang: AppLanguage) = if (lang == AppLanguage.EN) "APPLY" else "適用"

    // Dialogs & Review
    fun reviewTitle(lang: AppLanguage, phase: String) = if (lang == AppLanguage.EN) "PHASE REVIEW // ${phase.uppercase()}" else "振り返り // $phase"
    fun reviewQuestion(lang: AppLanguage) = if (lang == AppLanguage.EN) "WAS THE PHASE COMPLETED NORMALLY?" else "フェーズは正常に終了しましたか？"
    fun reviewYes(lang: AppLanguage) = if (lang == AppLanguage.EN) "YES" else "はい"
    fun reviewNo(lang: AppLanguage) = if (lang == AppLanguage.EN) "NO" else "いいえ"
    fun reviewNoteTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "REVIEW NOTE (OPTIONAL)" else "振り返りメモ (任意)"
    fun reviewNoteHint(lang: AppLanguage) = if (lang == AppLanguage.EN) "Enter blockers, learnings, or notes..." else "気づきや課題、ブロッカーを入力..."
    fun recordAndComplete(lang: AppLanguage) = if (lang == AppLanguage.EN) "RECORD & COMPLETE" else "記録して完了"

    // Edit Item Dialog
    fun taskConfig(lang: AppLanguage) = if (lang == AppLanguage.EN) "TASK CONFIG" else "タスク設定"
    fun taskTitleHint(lang: AppLanguage) = if (lang == AppLanguage.EN) "Task title..." else "タスク名..."

    // Templates
    fun applyTemplateTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "APPLY TEMPLATE" else "テンプレート適用"
    fun noTemplatesAvailable(lang: AppLanguage) = if (lang == AppLanguage.EN) "NO TEMPLATES CONFIGURED" else "利用可能なテンプレートがありません"
    fun itemsCount(lang: AppLanguage, count: Int) = if (lang == AppLanguage.EN) "$count ITEMS" else "$count 項目"
    fun presetSuggestion(lang: AppLanguage) = if (lang == AppLanguage.EN) "PRESET SUGGESTION" else "プリセット提案"
    fun addItem(lang: AppLanguage) = if (lang == AppLanguage.EN) "ADD ITEM" else "項目追加"

    // Home Screen
    fun dailyTimeline(lang: AppLanguage) = if (lang == AppLanguage.EN) "DAILY TIMELINE" else "タイムライン"
    fun selectPhaseToMonitor(lang: AppLanguage) = if (lang == AppLanguage.EN) "SELECT PHASE TO MONITOR & EXECUTE" else "フェーズを選択して実行・管理"
    fun advanceToNextDay(lang: AppLanguage) = if (lang == AppLanguage.EN) "ADVANCE TO NEXT DAY" else "翌日に進む"
    fun resetAllPhases(lang: AppLanguage) = if (lang == AppLanguage.EN) "RESET ALL PHASES" else "全フェーズをリセット"
    fun resetAllPhasesTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "CONFIRM RESET: ALL PHASES" else "全フェーズのリセット確認"
    fun resetAllPhasesMessage(lang: AppLanguage) = if (lang == AppLanguage.EN) 
        "Reset all task completion statuses across all phases? Logs for today will be preserved." 
    else 
        "すべてのフェーズのタスク完了状況をリセットしますか？この日のログは保持されます。"
    fun resetAll(lang: AppLanguage) = if (lang == AppLanguage.EN) "RESET ALL" else "全リセット"
    fun active(lang: AppLanguage) = if (lang == AppLanguage.EN) "ACTIVE" else "進行中"
    fun done(lang: AppLanguage) = if (lang == AppLanguage.EN) "DONE" else "完了"

    // Calendar Screen
    fun dateLog(lang: AppLanguage) = if (lang == AppLanguage.EN) "DATE LOG" else "日付ログ"
    fun completedItems(lang: AppLanguage) = if (lang == AppLanguage.EN) "COMPLETED ITEMS" else "完了タスク"
    fun reqPending(lang: AppLanguage) = if (lang == AppLanguage.EN) "CRIT PENDING" else "重要未完了"
    fun phaseBreakdown(lang: AppLanguage) = if (lang == AppLanguage.EN) "PHASE BREAKDOWN" else "フェーズ内訳"
    fun todaysLiveStatus(lang: AppLanguage) = if (lang == AppLanguage.EN) "TODAY'S LIVE STATUS" else "本日のリアルタイム状況"
    fun noMissionLog(lang: AppLanguage) = if (lang == AppLanguage.EN) "NO MISSION LOG RECORDED" else "記録されたログはありません"
    fun reviewLogs(lang: AppLanguage, count: Int) = if (lang == AppLanguage.EN) "REVIEW LOGS ($count)" else "振り返りログ ($count)"
    fun issueNoted(lang: AppLanguage) = if (lang == AppLanguage.EN) "ISSUE NOTED" else "課題あり"
    fun allClear(lang: AppLanguage) = if (lang == AppLanguage.EN) "ALL CLEAR" else "問題なし"
    fun currentSol(lang: AppLanguage) = if (lang == AppLanguage.EN) "CURRENT SOL" else "本日"
}

