package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.network.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmRepositoryImpl @Inject constructor(
    private val llmService: LlmApiService
) : LlmRepository {

    override suspend fun fetchModels(provider: String, baseUrl: String, apiKey: String): List<String> {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Please configure API Key in Settings first.")
        }
        val effectiveUrl = if (baseUrl.isBlank()) LlmConfig.defaultUrls[provider] ?: "" else baseUrl
        return when (provider) {
            "DeepSeek", "OpenAI Compatible" -> {
                val cleanBase = effectiveUrl.trimEnd('/')
                val url = "$cleanBase/models"
                val response = llmService.getOpenAiModels(url, "Bearer $apiKey")
                response.data.map { it.id }
            }
            "Qwen" -> {
                LlmConfig.qwenKnownModels
            }
            "Gemini", "Vertex AI" -> {
                try {
                    val cleanBase = effectiveUrl.trimEnd('/')
                    val url = "$cleanBase/models?key=$apiKey"
                    val response = llmService.getGeminiModels(url)
                    response.models
                        .filter { model ->
                            model.supportedGenerationMethods?.contains("generateContent") ?: true
                        }
                        .map { it.name.removePrefix("models/") }
                } catch (e: Exception) {
                    LlmConfig.geminiKnownModels
                }
            }
            else -> throw IllegalArgumentException("Unsupported provider")
        }
    }

    override suspend fun callLlmApi(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        provider: String,
        modelName: String,
        effectiveUrl: String,
        apiKey: String
    ): String {
        return when (provider) {
            "DeepSeek", "OpenAI Compatible", "Qwen" -> {
                val cleanBase = effectiveUrl.trimEnd('/')
                val url = "$cleanBase/chat/completions"

                val userContent: Any = if (imageBase64 != null) {
                    listOf(
                        OpenAiContentPart(type = "text", text = userPrompt),
                        OpenAiContentPart(
                            type = "image_url",
                            image_url = OpenAiImageUrl(url = "data:$mimeType;base64,$imageBase64")
                        )
                    )
                } else {
                    userPrompt
                }

                val request = OpenAiRequest(
                    model = modelName,
                    messages = listOf(
                        OpenAiMessage(role = "system", content = systemPrompt),
                        OpenAiMessage(role = "user", content = userContent)
                    ),
                    temperature = 0.1,
                    response_format = OpenAiResponseFormat("json_object")
                )
                val response = llmService.generateOpenAiCompatible(url, "Bearer $apiKey", request)
                response.choices.firstOrNull()?.message?.content ?: throw Exception("No response from model")
            }
            "Gemini", "Vertex AI" -> {
                val cleanBase = effectiveUrl.trimEnd('/')
                val url = "$cleanBase/models/$modelName:generateContent?key=$apiKey"

                val parts = mutableListOf<GeminiPart>()
                parts.add(GeminiPart(text = userPrompt))
                if (imageBase64 != null && mimeType != null) {
                    parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = imageBase64)))
                }

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = parts)),
                    systemInstruction = GeminiContent(role = "user", parts = listOf(GeminiPart(text = systemPrompt))),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.1,
                        responseMimeType = "application/json"
                    )
                )
                val response = llmService.generateGemini(url, request)
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("No response from model")
            }
            else -> throw Exception("Unsupported provider")
        }
    }
}
