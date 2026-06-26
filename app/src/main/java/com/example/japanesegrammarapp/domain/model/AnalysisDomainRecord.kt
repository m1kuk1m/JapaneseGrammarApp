package com.example.japanesegrammarapp.domain.model

data class AnalysisDomainRecord(
    val id: Int = 0,
    val originalText: String,
    val imageUri: String? = null,
    val analysisResult: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String,
    val status: AnalysisStatus = AnalysisStatus.PENDING,
    val errorMessage: String? = null,
    val consumedTokens: Int = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val isRead: Boolean = false
)