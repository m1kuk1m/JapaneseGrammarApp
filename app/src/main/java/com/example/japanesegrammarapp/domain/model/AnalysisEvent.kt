package com.example.japanesegrammarapp.domain.model

sealed class AnalysisEvent {
    data class TaskCompleted(val recordId: Int, val analyzedText: String, val isShortened: Boolean) : AnalysisEvent()
    data class OcrFallbackTriggered(val recordId: Int) : AnalysisEvent()
    data class SpellingCorrectedTriggered(val recordId: Int, val correctedText: String) : AnalysisEvent()
    data class LlmRetryTriggered(val recordId: Int, val step: AnalysisStep, val attempt: Int) : AnalysisEvent()
    data class LlmBackupTriggered(val recordId: Int, val step: AnalysisStep, val backupProvider: String) : AnalysisEvent()
    data class DuplicateFound(val currentRecordId: Int, val existingRecordId: Int) : AnalysisEvent()
    data class TaskFailed(val recordId: Int, val exception: Exception) : AnalysisEvent()
}