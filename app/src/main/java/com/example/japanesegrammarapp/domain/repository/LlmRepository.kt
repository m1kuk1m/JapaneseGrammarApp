package com.example.japanesegrammarapp.domain.repository

data class LlmResult(val text: String, val consumedTokens: Int, val inputTokens: Int = 0, val outputTokens: Int = 0)

data class LlmApiConfig(
    val provider: String,
    val modelName: String,
    val baseUrl: String,
    val apiKey: String
)

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
        apiTypeLabel: String,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {}
    ): LlmResult
}
