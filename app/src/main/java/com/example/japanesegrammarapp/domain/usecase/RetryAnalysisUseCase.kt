package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryAnalysisUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val analyzeTextUseCase: AnalyzeTextUseCase
) {
    suspend fun execute(recordId: Int): Int {
        val record = historyRepository.getRecordById(recordId) ?: throw IllegalArgumentException("Record not found")
        val activeProvider = settingsRepository.getActiveProvider()
        val activeModel = settingsRepository.getActiveModel(activeProvider)
        historyRepository.updateRecord(
            record.copy(
                status = AnalysisStatus.PENDING,
                errorMessage = null,
                modelUsed = "$activeProvider: $activeModel"
            )
        )

        analyzeTextUseCase.executeRetry(
            recordId = recordId,
            text = record.originalText,
            imageUri = record.imageUri
        )
        return recordId
    }
}
