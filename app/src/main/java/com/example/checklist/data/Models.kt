package com.example.checklist.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Priority {
    REQUIRED,
    NORMAL
}

@Serializable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val priority: Priority = Priority.NORMAL,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
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
    val note: String? = null
)

@Serializable
data class AppState(
    val phases: List<Phase> = emptyList(),
    val selectedPhaseIndex: Int = 0,
    val templates: List<PhaseTemplate> = emptyList(),
    val reviewLogs: List<ReviewLog> = emptyList()
)
