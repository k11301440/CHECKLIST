package com.example.checklist.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Priority {
    REQUIRED,
    NORMAL
}

@Serializable
enum class AppLanguage {
    EN,
    JA
}

@Serializable
enum class ThemeMode {
    SPACE_BLACK,
    SPACE_WHITE,
    TRON_RED,
    MONSTER_GREEN,
    ANA_BLUE,
    JAL_RED,
    SYSTEM
}

@Serializable
enum class DarkThemeConfig {
    DARK,
    LIGHT,
    SYSTEM
}

@Serializable
enum class AccentTheme {
    MONOCHROME,
    TRON,
    MONSTER,
    ANA,
    AIRDO,
    JAL
}

@Serializable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val priority: Priority = Priority.NORMAL,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isCarryover: Boolean = false,
    val sourcePhaseName: String? = null,
    val originPhaseIndex: Int = 0,
    val isDeleted: Boolean = false,
    val scheduledDate: String? = null
)

@Serializable
data class Phase(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val items: List<ChecklistItem> = emptyList(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val reviewNote: String? = null
)

@Serializable
data class TemplateItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val priority: Priority = Priority.NORMAL
)

@Serializable
data class PhaseTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val items: List<TemplateItem> = emptyList(),
    val targetDaysOfWeek: List<Int> = emptyList(), // 1: Mon .. 7: Sun. Empty means all days
    val startDate: String? = null, // "yyyy-MM-dd"
    val endDate: String? = null   // "yyyy-MM-dd"
)

@Serializable
data class ReviewLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val phaseName: String,
    val hadIssue: Boolean,
    val note: String? = null,
    val dateString: String? = null
)

@Serializable
data class DailyRecord(
    val dateString: String, // e.g. "2026-10-16"
    val completedAt: Long = System.currentTimeMillis(),
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val requiredPendingItems: Int = 0,
    val phaseSummaries: List<PhaseDailySummary> = emptyList()
)

@Serializable
data class PhaseDailySummary(
    val phaseName: String,
    val isCompleted: Boolean,
    val completedCount: Int,
    val totalCount: Int,
    val reviewNote: String? = null,
    val hadIssue: Boolean = false
)

@Serializable
enum class NotificationDisplayMode {
    OFF,
    COUNT_ONLY,
    COUNT_AND_TITLE
}

@Serializable
data class PhaseSchedule(
    val phaseIndex: Int,
    val phaseName: String,
    val startHour: Int,
    val startMinute: Int
)

@Serializable
data class AppSettings(
    val appLanguage: AppLanguage = AppLanguage.JA,
    val themeMode: ThemeMode = ThemeMode.SPACE_BLACK,
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.DARK,
    val accentTheme: AccentTheme = AccentTheme.MONOCHROME,
    val enableSwipePhase: Boolean = true,
    val autoAdvanceByTime: Boolean = true,
    val carryoverUnfinishedReq: Boolean = true,
    val carryoverUnfinishedNorm: Boolean = false,
    val confirmReviewOnAdvance: Boolean = false,
    val showReqNotification: Boolean = false,
    val notificationMode: NotificationDisplayMode = NotificationDisplayMode.OFF,
    val phaseEndReminderMinutes: Int = 0,
    val useLargeButtons: Boolean = false,
    val phaseSchedules: List<PhaseSchedule> = listOf(
        PhaseSchedule(0, "朝", 6, 0),
        PhaseSchedule(1, "午前", 9, 0),
        PhaseSchedule(2, "午後", 12, 0),
        PhaseSchedule(3, "夜", 18, 0)
    )
)

// 未来日付で予約されたタスク (指定日になるまでフェーズには表示しない)
@Serializable
data class ScheduledTask(
    val phaseId: String,
    val item: ChecklistItem
)

@Serializable
data class AppState(
    val currentDate: String = "",
    val phases: List<Phase> = emptyList(),
    val selectedPhaseIndex: Int = 0,
    val templates: List<PhaseTemplate> = emptyList(),
    val reviewLogs: List<ReviewLog> = emptyList(),
    val dailyRecords: Map<String, DailyRecord> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val scheduledItems: List<ScheduledTask> = emptyList()
)

val Phase.validItems: List<ChecklistItem>
    get() = items.filterNot { it.isDeleted }

val Phase.activeItems: List<ChecklistItem>
    get() = items.filter { !it.isCompleted && !it.isDeleted }

val Phase.completedItemsList: List<ChecklistItem>
    get() = items.filter { it.isCompleted && !it.isDeleted }

val Phase.discardedItemsList: List<ChecklistItem>
    get() = items.filter { it.isDeleted }

val Phase.requiredPendingCount: Int
    get() = items.count { it.priority == Priority.REQUIRED && !it.isCompleted && !it.isDeleted }

val Phase.requiredDoneCount: Int
    get() = items.count { it.priority == Priority.REQUIRED && it.isCompleted && !it.isDeleted }

val Phase.normalPendingCount: Int
    get() = items.count { it.priority == Priority.NORMAL && !it.isCompleted && !it.isDeleted }

val Phase.normalDoneCount: Int
    get() = items.count { it.priority == Priority.NORMAL && it.isCompleted && !it.isDeleted }

val Phase.totalValidCount: Int
    get() = items.count { !it.isDeleted }

val Phase.totalDoneCount: Int
    get() = items.count { it.isCompleted && !it.isDeleted }

