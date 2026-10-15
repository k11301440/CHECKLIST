package com.example.checklist

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PhaseNavKey : NavKey

@Serializable
data object HomeNavKey : NavKey

@Serializable
data object SettingsNavKey : NavKey

@Serializable
data class TemplateEditorNavKey(val templateId: String? = null) : NavKey
