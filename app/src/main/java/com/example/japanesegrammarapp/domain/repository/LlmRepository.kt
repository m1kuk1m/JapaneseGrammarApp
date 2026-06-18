package com.example.japanesegrammarapp.domain.repository

data class LlmResult(
    val text: String,
    val consumedTokens: Int,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val provider: String? = null,
    val modelName: String? = null,
    val endpointId: String? = null,
    val endpointName: String? = null
)

data class LlmApiConfig(
    val provider: String,
    val baseProvider: String,
    val modelName: String,
    val url: String,
    val key: String,
    val endpointId: String = "",
    val endpointName: String = ""
)

interface LlmRepository {
    suspend fun fetchModels(provider: String, baseUrl: String, apiKey: String): List<String>
    suspend fun callLlmApi(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        provider: String,
        baseProvider: String,
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
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        recordId: Int? = null,
        stepName: String? = null,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {}
    ): LlmResult

    suspend fun executeWithStreaming(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        apiTypeLabel: String,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        recordId: Int?,
        stepName: String?,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {}
    ): kotlinx.coroutines.flow.Flow<com.example.japanesegrammarapp.domain.model.LlmStreamEvent>
}
