package com.example.japanesegrammarapp.domain.model

data class HistoryExportPreview(
    val id: Int,
    val originalText: String,
    val timestamp: Long,
    val modelUsed: String,
    val status: AnalysisStatus
)
