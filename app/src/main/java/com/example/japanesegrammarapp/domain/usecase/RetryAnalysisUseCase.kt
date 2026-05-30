package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryAnalysisUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val analyzeTextUseCase: AnalyzeTextUseCase
) {
    suspend fun execute(recordId: Int): Int {
        val record = historyRepository.getRecordById(recordId) ?: throw IllegalArgumentException("Record not found")
        historyRepository.updateRecord(record.copy(status = AnalysisStatus.PENDING, errorMessage = null))

        analyzeTextUseCase.executeRetry(
            recordId = recordId,
            text = record.originalText,
            imageUri = record.imageUri
        )
        return recordId
    }
}
