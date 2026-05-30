package com.example.japanesegrammarapp.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.japanesegrammarapp.network.*
import com.example.japanesegrammarapp.domain.repository.LlmRepository
import com.example.japanesegrammarapp.domain.repository.LlmResult
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmRepositoryImpl @Inject constructor(
    private val llmService: LlmApiService,
    private val settingsRepository: SettingsRepository
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
                val response = llmService.getOpenAiModels(url, "Bearer ${apiKey.trim()}")
                response.data.map { it.id }
            }
            "Qwen" -> {
                LlmConfig.qwenKnownModels
            }
            "Gemini", "Vertex AI" -> {
                try {
                    val cleanBase = effectiveUrl.trimEnd('/')
                    val url = "$cleanBase/models?key=${apiKey.trim()}"
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
    ): LlmResult {
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
                val response = llmService.generateOpenAiCompatible(url, "Bearer ${apiKey.trim()}", request)
                val text = response.choices.firstOrNull()?.message?.content ?: throw Exception("No response from model")
                val tokens = response.usage?.total_tokens ?: 0
                var inputTokens = response.usage?.prompt_tokens ?: 0
                var outputTokens = response.usage?.completion_tokens ?: 0
                if (tokens > 0 && inputTokens == 0 && outputTokens == 0) {
                    inputTokens = tokens * 6 / 10
                    outputTokens = tokens - inputTokens
                }
                LlmResult(text, tokens, inputTokens, outputTokens)

            }
            "Gemini", "Vertex AI" -> {
                val cleanBase = effectiveUrl.trimEnd('/')
                val url = "$cleanBase/models/$modelName:generateContent?key=${apiKey.trim()}"

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
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("No response from model")
                val tokens = response.usageMetadata?.totalTokenCount ?: 0
                var inputTokens = response.usageMetadata?.promptTokenCount ?: 0
                var outputTokens = response.usageMetadata?.candidatesTokenCount ?: 0
                if (tokens > 0 && inputTokens == 0 && outputTokens == 0) {
                    inputTokens = tokens * 6 / 10
                    outputTokens = tokens - inputTokens
                }
                LlmResult(text, tokens, inputTokens, outputTokens)
            }
            else -> throw Exception("Unsupported provider")
        }
    }

    override suspend fun executeWithFailover(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        apiTypeLabel: String,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): LlmResult {
        val primaryProvider = settingsRepository.getActiveProvider()
        val primaryModel = settingsRepository.getActiveModel(primaryProvider)
        val primaryKey = settingsRepository.getApiKey(primaryProvider)
        val primaryUrl = settingsRepository.getApiUrl(primaryProvider)

        val backupProvider = settingsRepository.getBackupProvider()
        val backupModel = settingsRepository.getBackupModel()
        val backupKey = settingsRepository.getApiKey(backupProvider)
        val backupUrl = settingsRepository.getApiUrl(backupProvider)

        var attempt = 0
        val maxRetries = 2
        var lastException: Exception? = null

        while (attempt <= maxRetries) {
            try {
                return callLlmApi(
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    imageBase64 = imageBase64,
                    mimeType = mimeType,
                    provider = primaryProvider,
                    modelName = primaryModel,
                    effectiveUrl = primaryUrl,
                    apiKey = primaryKey
                )
            } catch (e: Exception) {
                lastException = e
                attempt++
                if (attempt <= maxRetries) {
                    onRetry(attempt)
                    delay(1000L)
                }
            }
        }

        if (backupProvider.isBlank() || backupModel.isBlank() || backupKey.isBlank()) {
            throw Exception("メインAPI（${apiTypeLabel}）の再試行に失敗し、予備APIが設定されていないか有効ではありません。メインAPIエラー: ${lastException?.localizedMessage}", lastException)
        }

        onBackup(backupProvider)

        try {
            return callLlmApi(
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                imageBase64 = imageBase64,
                mimeType = mimeType,
                provider = backupProvider,
                modelName = backupModel,
                effectiveUrl = backupUrl,
                apiKey = backupKey
            )
        } catch (e: Exception) {
            throw Exception("メインAPIおよび予備APIの呼び出しに失敗しました。メインAPIエラー: ${lastException?.localizedMessage}。予備APIエラー: ${e.localizedMessage}", e)
        }
    }
}
