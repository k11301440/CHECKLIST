package com.example.checklist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.checklist.data.AppState
import com.example.checklist.data.ChecklistRepository
import com.example.checklist.data.PhaseSchedule
import com.example.checklist.data.PhaseTemplate
import com.example.checklist.data.Priority
import com.example.checklist.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ChecklistViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChecklistRepository(application.applicationContext)

    val state: StateFlow<AppState> = repository.appState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.appState.value
    )

    init {
        // 起動時に時間ベースのフェーズチェック
        repository.checkAndAdvanceByTime()
    }

    fun selectPhase(index: Int) {
        repository.selectPhase(index)
    }

    // タップ1回で完了
    fun completeItem(phaseId: String, itemId: String) {
        repository.completeItem(phaseId, itemId)
    }

    // 長押しで取り消し
    fun undoItem(phaseId: String, itemId: String) {
        repository.undoItem(phaseId, itemId)
    }

    fun addItem(phaseId: String, title: String, priority: Priority) {
        repository.addItem(phaseId, title, priority)
    }

    fun removeItem(phaseId: String, itemId: String) {
        repository.removeItem(phaseId, itemId)
    }

    fun addPhase(name: String): Boolean {
        return repository.addPhase(name)
    }

    fun removePhase(phaseId: String): Boolean {
        return repository.removePhase(phaseId)
    }

    fun updatePhaseName(phaseId: String, newName: String) {
        repository.updatePhaseName(phaseId, newName)
    }

    fun completePhase(phaseId: String, hadIssue: Boolean, note: String?) {
        repository.completePhase(phaseId, hadIssue, note)
    }

    fun advanceToNextDay() {
        repository.advanceToNextDay()
    }

    fun checkAndAdvanceByTime() {
        repository.checkAndAdvanceByTime()
    }

    fun resetPhase(phaseId: String) {
        repository.resetPhase(phaseId)
    }

    fun resetAllPhases() {
        repository.resetAllPhases()
    }

    fun applyTemplate(phaseId: String, templateId: String, replace: Boolean = false) {
        repository.applyTemplate(phaseId, templateId, replace)
    }

    fun savePhaseAsTemplate(phaseId: String, templateName: String) {
        repository.savePhaseAsTemplate(phaseId, templateName)
    }

    fun saveTemplate(template: PhaseTemplate) {
        repository.saveTemplate(template)
    }

    fun updateItem(phaseId: String, itemId: String, newTitle: String, newPriority: Priority) {
        repository.updateItem(phaseId, itemId, newTitle, newPriority)
    }

    fun clearCompletedItems(phaseId: String) {
        repository.clearCompletedItems(phaseId)
    }

    fun restoreDefaultFourPhases() {
        repository.restoreDefaultFourPhases()
    }

    fun deleteTemplate(templateId: String) {
        repository.deleteTemplate(templateId)
    }

    // 設定関連
    fun setThemeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }

    fun setAutoAdvanceByTime(enabled: Boolean) {
        repository.setAutoAdvanceByTime(enabled)
    }

    fun setCarryoverUnfinishedReq(enabled: Boolean) {
        repository.setCarryoverUnfinishedReq(enabled)
    }

    fun updatePhaseSchedules(schedules: List<PhaseSchedule>) {
        repository.updatePhaseSchedules(schedules)
    }
}
