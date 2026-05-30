package com.example.japanesegrammarapp.data.repository

data class LlmResult(val text: String, val consumedTokens: Int, val inputTokens: Int = 0, val outputTokens: Int = 0)

interface LlmRepository {
    suspend fun fetchModels(provider: String, baseUrl: String, apiKey: String): List<String>
    suspend fun callLlmApi(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        provider: String,
        modelName: String,
        effectiveUrl: String,
        apiKey: String
    ): LlmResult

    suspend fun executeWithFailover(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        apiTypeLabel: String
    ): LlmResult
}
