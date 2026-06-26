package com.example.japanesegrammarapp.data.mapper

import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.data.DailyTokenUsageEntity
import com.example.japanesegrammarapp.data.HistoryExportPreviewEntity
import com.example.japanesegrammarapp.data.ModelTokenUsageEntity
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.domain.model.DailyTokenUsage
import com.example.japanesegrammarapp.domain.model.HistoryExportPreview
import com.example.japanesegrammarapp.domain.model.ModelTokenUsage

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
        outputTokens = outputTokens,
        isRead = isRead
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
        outputTokens = outputTokens,
        isRead = isRead
    )
}

fun HistoryExportPreviewEntity.toDomain(): HistoryExportPreview {
    val domainStatus = when (status) {
        "PENDING" -> AnalysisStatus.PENDING
        "FAILED" -> AnalysisStatus.FAILED
        else -> AnalysisStatus.COMPLETED
    }
    return HistoryExportPreview(
        id = id,
        originalText = originalText,
        timestamp = timestamp,
        modelUsed = modelUsed,
        status = domainStatus
    )
}

fun ModelTokenUsageEntity.toDomain(): ModelTokenUsage =
    ModelTokenUsage(
        modelUsed = modelUsed,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        totalTokens = totalTokens
    )

fun DailyTokenUsageEntity.toDomain(): DailyTokenUsage =
    DailyTokenUsage(
        date = date,
        modelUsed = modelUsed,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        totalTokens = totalTokens
    )
