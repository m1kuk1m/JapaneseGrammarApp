package com.example.japanesegrammarapp.data

sealed class AnalysisEvent {
    data class TaskCompleted(val recordId: Int, val message: String) : AnalysisEvent()
    data class OcrFallbackTriggered(val recordId: Int) : AnalysisEvent()
    data class SpellingCorrectedTriggered(val recordId: Int, val correctedText: String) : AnalysisEvent()
}
