package com.example.japanesegrammarapp.data.mapper

import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus

fun AnalysisRecord.toDomain(): AnalysisDomainRecord {
    val domainStatus = when (status) {
        "PENDING" -> AnalysisStatus.PENDING
        "FAILED" -> AnalysisStatus.FAILED
        else -> AnalysisStatus.COMPLETED
    }
    return AnalysisDomainRecord(
        id = id,
        originalText = originalText,
        imageUri = imageUri,
        analysisResult = analysisResult,
        timestamp = timestamp,
        modelUsed = modelUsed,
        status = domainStatus,
        errorMessage = errorMessage,
        consumedTokens = consumedTokens,
        inputTokens = inputTokens,
        outputTokens = outputTokens
    )
}

fun AnalysisDomainRecord.toEntity(): AnalysisRecord {
    return AnalysisRecord(
        id = id,
        originalText = originalText,
        imageUri = imageUri,
        analysisResult = analysisResult,
        timestamp = timestamp,
        modelUsed = modelUsed,
        status = status.name,
        errorMessage = errorMessage,
        consumedTokens = consumedTokens,
        inputTokens = inputTokens,
        outputTokens = outputTokens
    )
}
