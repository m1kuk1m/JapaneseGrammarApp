package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzeTextUseCase @Inject constructor(
    private val taskManager: AnalysisTaskManager
) {
    val progressFlow: StateFlow<Map<Int, AnalysisProgress>> = taskManager.progressFlow

    suspend fun execute(
        text: String,
        imageUri: String?,
        provider: String,
        modelName: String,
        baseUrl: String,
        apiKey: String
    ): Int = taskManager.execute(text, imageUri, provider, modelName, baseUrl, apiKey)

    suspend fun executeRetry(recordId: Int, text: String, imageUri: String?) {
        taskManager.executeRetry(recordId, text, imageUri)
    }

    fun cancel(recordId: Int) {
        taskManager.cancel(recordId)
    }

    fun parseDetailedResult(originalText: String, jsonString: String?): DetailedAnalysisResult? {
        return taskManager.parseDetailedResult(originalText, jsonString)
    }
}