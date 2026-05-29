package com.example.japanesegrammarapp.data.repository

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
    ): String
}
