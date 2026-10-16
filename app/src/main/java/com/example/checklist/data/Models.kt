package com.example.checklist.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Priority {
    REQUIRED,
    NORMAL
}

@Serializable
enum class ThemeMode {
    SPACE_BLACK,
    SPACE_WHITE,
    SYSTEM
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
    val items: List<TemplateItem> = emptyList()
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
data class PhaseSchedule(
    val phaseIndex: Int,
    val phaseName: String,
    val startHour: Int,
    val startMinute: Int
)

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SPACE_BLACK,
    val autoAdvanceByTime: Boolean = true,
    val carryoverUnfinishedReq: Boolean = true,
    val carryoverUnfinishedNorm: Boolean = false,
    val confirmReviewOnAdvance: Boolean = true,
    val phaseSchedules: List<PhaseSchedule> = listOf(
        PhaseSchedule(0, "朝", 6, 0),
        PhaseSchedule(1, "午前", 9, 0),
        PhaseSchedule(2, "午後", 12, 0),
        PhaseSchedule(3, "夜", 18, 0)
    )
)

@Serializable
data class AppState(
    val currentDate: String = "",
    val phases: List<Phase> = emptyList(),
    val selectedPhaseIndex: Int = 0,
    val templates: List<PhaseTemplate> = emptyList(),
    val reviewLogs: List<ReviewLog> = emptyList(),
    val dailyRecords: Map<String, DailyRecord> = emptyMap(),
    val settings: AppSettings = AppSettings()
)
