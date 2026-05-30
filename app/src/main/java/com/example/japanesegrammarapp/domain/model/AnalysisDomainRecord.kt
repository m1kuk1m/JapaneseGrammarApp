package com.example.japanesegrammarapp.domain.model

enum class AnalysisStatus {
    PENDING,
    COMPLETED,
    FAILED
}

data class AnalysisDomainRecord(
    val id: Int = 0,
    val originalText: String,
    val imageUri: String?,
    val analysisResult: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String,
    val status: AnalysisStatus = AnalysisStatus.COMPLETED,
    val errorMessage: String? = null,
    val consumedTokens: Int = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0
)
