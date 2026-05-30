package com.example.japanesegrammarapp.domain.model

sealed class AnalysisEvent {
    data class TaskCompleted(val recordId: Int, val message: String) : AnalysisEvent()
    data class OcrFallbackTriggered(val recordId: Int) : AnalysisEvent()
    data class SpellingCorrectedTriggered(val recordId: Int, val correctedText: String) : AnalysisEvent()
    data class LlmRetryTriggered(val recordId: Int, val apiTypeLabel: String, val attempt: Int) : AnalysisEvent()
    data class LlmBackupTriggered(val recordId: Int, val apiTypeLabel: String, val backupProvider: String) : AnalysisEvent()
}
