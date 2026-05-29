package com.example.japanesegrammarapp.domain.usecase

import android.net.Uri
import com.example.japanesegrammarapp.data.repository.HistoryRepository
import com.example.japanesegrammarapp.data.repository.SettingsRepository
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
        historyRepository.updateRecord(record.copy(status = "PENDING", errorMessage = null))

        val providerAndModel = record.modelUsed.split(": ")
        val provider = providerAndModel.getOrNull(0) ?: "Gemini"
        val modelName = providerAndModel.getOrNull(1) ?: "default"

        val key = settingsRepository.getApiKey(provider)
        val url = settingsRepository.getApiUrl(provider)
        val imageUri = record.imageUri?.let { Uri.parse(it) }

        analyzeTextUseCase.executeRetry(recordId, record.originalText, imageUri, provider, modelName, url, key)
        return recordId
    }
}
