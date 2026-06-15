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
        historyRepository.updateRecord(record.copy(status = AnalysisStatus.PENDING, errorMessage = null))

        val providerAndModel = record.modelUsed.split(": ")
        val provider = providerAndModel.getOrNull(0) ?: "Gemini"
        val modelName = providerAndModel.getOrNull(1) ?: "default"

        val key = settingsRepository.getApiKey(provider)
        val url = settingsRepository.getApiUrl(provider)

        analyzeTextUseCase.executeRetry(
            recordId = recordId,
            text = record.originalText,
            imageUri = record.imageUri
        )
        return recordId
    }
}
