package com.example.japanesegrammarapp.data

sealed class AnalysisEvent {
    data class TaskCompleted(val recordId: Int, val message: String) : AnalysisEvent()
}
