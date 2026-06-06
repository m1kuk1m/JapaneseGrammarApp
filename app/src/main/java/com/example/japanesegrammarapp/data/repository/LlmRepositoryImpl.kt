package com.example.japanesegrammarapp.data.repository

import kotlinx.coroutines.delay
import com.example.japanesegrammarapp.network.*
import com.example.japanesegrammarapp.domain.model.LlmConfig
import com.example.japanesegrammarapp.domain.repository.LlmRepository
import com.example.japanesegrammarapp.domain.repository.LlmResult
import com.example.japanesegrammarapp.domain.repository.LlmApiConfig
import com.example.japanesegrammarapp.utils.AppLogger
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
        try {
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
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val safeBody = if (errorBody.length > 200) errorBody.take(200) + "..." else errorBody
            throw Exception("HTTP ${e.code()}: ${e.message()}\n$safeBody", e)
        }
    }

    override suspend fun callLlmApi(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        provider: String,
        baseProvider: String,
        modelName: String,
        effectiveUrl: String,
        apiKey: String
    ): LlmResult {
        return when (baseProvider) {
            "OpenAI", "DeepSeek", "OpenAI Compatible", "Qwen" -> {
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
                LlmResult(text, tokens, inputTokens, outputTokens, provider, modelName)

            }
            "Gemini", "Vertex AI" -> {
                val cleanBase = effectiveUrl.trimEnd('/')
                val url = "$cleanBase/models/$modelName:generateContent?key=${apiKey.trim()}"

                val parts = mutableListOf<GeminiPart>()
                parts.add(GeminiPart(text = userPrompt))
                if (imageBase64 != null && mimeType != null) {
                    parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = imageBase64)))
                }

                val safetySettings = listOf(
                    GeminiSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
                    GeminiSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
                    GeminiSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
                    GeminiSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
                )

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = parts)),
                    systemInstruction = GeminiSystemInstruction(parts = listOf(GeminiPart(text = systemPrompt))),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.1,
                        responseMimeType = "application/json"
                    ),
                    safetySettings = safetySettings
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
                LlmResult(text, tokens, inputTokens, outputTokens, provider, modelName)
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
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        recordId: Int?,
        stepName: String?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): LlmResult {
        val primaryProvider = primaryConfig.provider
        val primaryModel = primaryConfig.modelName
        val primaryKey = primaryConfig.key
        val primaryUrl = primaryConfig.url
        val primaryBaseProvider = primaryConfig.baseProvider

        val backupProvider = backupConfig?.provider ?: ""
        val backupModel = backupConfig?.modelName ?: ""
        val backupKey = backupConfig?.key ?: ""
        val backupUrl = backupConfig?.url ?: ""
        val backupBaseProvider = backupConfig?.baseProvider ?: ""

        var attempt = 0
        val maxRetries = 2
        var lastException: Exception? = null

        while (attempt <= maxRetries) {
            val attemptNumber = attempt + 1
            val attemptStartMs = System.currentTimeMillis()
            try {
                AppLogger.apiEvent(
                    apiTypeLabel = apiTypeLabel,
                    provider = primaryProvider,
                    model = primaryModel,
                    status = "START",
                    hasImage = imageBase64 != null,
                    userPrompt = userPrompt,
                    systemPrompt = systemPrompt,
                    message = "Primary request attempt $attemptNumber started",
                    recordId = recordId,
                    stepName = stepName,
                    attempt = attemptNumber
                )
                return callLlmApi(
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    imageBase64 = imageBase64,
                    mimeType = mimeType,
                    provider = primaryProvider,
                    baseProvider = primaryBaseProvider,
                    modelName = primaryModel,
                    effectiveUrl = primaryUrl,
                    apiKey = primaryKey
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val elapsedMs = System.currentTimeMillis() - attemptStartMs
                lastException = e
                AppLogger.e(
                    "LLM_API",
                    "Primary LLM Call failed for [$apiTypeLabel] (attempt $attemptNumber) via $primaryProvider ($primaryModel). Error: ${e.localizedMessage}",
                    e
                )
                attempt++
                if (attempt <= maxRetries) {
                    AppLogger.apiEvent(
                        apiTypeLabel = apiTypeLabel,
                        provider = primaryProvider,
                        model = primaryModel,
                        status = "RETRY",
                        hasImage = imageBase64 != null,
                        userPrompt = userPrompt,
                        systemPrompt = systemPrompt,
                        message = "Primary attempt $attemptNumber failed after ${elapsedMs}ms: ${e.localizedMessage ?: "Unknown error"}. Retrying attempt ${attempt + 1}.",
                        recordId = recordId,
                        stepName = stepName,
                        attempt = attemptNumber,
                        elapsedMs = elapsedMs
                    )
                    onRetry(attempt)
                    delay(1000L)
                } else {
                    AppLogger.apiError(
                        apiTypeLabel = apiTypeLabel,
                        provider = primaryProvider,
                        model = primaryModel,
                        hasImage = imageBase64 != null,
                        userPrompt = userPrompt,
                        systemPrompt = systemPrompt,
                        message = "Primary attempt $attemptNumber failed after ${elapsedMs}ms: ${e.localizedMessage ?: "Unknown error"}",
                        throwable = e,
                        recordId = recordId,
                        stepName = stepName,
                        attempt = attemptNumber,
                        elapsedMs = elapsedMs
                    )
                }
            }
        }

        if (backupProvider.isBlank() || backupModel.isBlank() || backupKey.isBlank()) {
            val errorMsg = "メインAPI（${apiTypeLabel}）の再試行に失敗し、予備APIが設定されていないか有効ではありません。メインAPIエラー: ${lastException?.localizedMessage}"
            AppLogger.e("LLM_API", errorMsg, lastException)
            throw com.example.japanesegrammarapp.domain.model.LlmApiFailedException(
                primaryProvider,
                lastException?.localizedMessage,
                false,
                null,
                null,
                lastException
            )
        }

        AppLogger.apiEvent(
            apiTypeLabel = apiTypeLabel,
            provider = backupProvider,
            model = backupModel,
            status = "BACKUP",
            hasImage = imageBase64 != null,
            userPrompt = userPrompt,
            systemPrompt = systemPrompt,
            message = "Switching to backup provider after primary retries failed: ${lastException?.localizedMessage ?: "Unknown error"}",
            recordId = recordId,
            stepName = stepName
        )
        onBackup(backupProvider)

        val backupStartMs = System.currentTimeMillis()
        try {
            AppLogger.apiEvent(
                apiTypeLabel = apiTypeLabel,
                provider = backupProvider,
                model = backupModel,
                status = "START",
                hasImage = imageBase64 != null,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                message = "Backup request started",
                recordId = recordId,
                stepName = stepName,
                attempt = 1
            )
            return callLlmApi(
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                imageBase64 = imageBase64,
                mimeType = mimeType,
                provider = backupProvider,
                baseProvider = backupBaseProvider,
                modelName = backupModel,
                effectiveUrl = backupUrl,
                apiKey = backupKey
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val elapsedMs = System.currentTimeMillis() - backupStartMs
            val errorMsg = "メインAPIおよび予備APIの呼び出しに失敗しました。メインAPIエラー: ${lastException?.localizedMessage}。予備APIエラー: ${e.localizedMessage}"
            AppLogger.apiError(
                apiTypeLabel = apiTypeLabel,
                provider = backupProvider,
                model = backupModel,
                hasImage = imageBase64 != null,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                message = "Backup request failed after ${elapsedMs}ms: ${e.localizedMessage ?: "Unknown error"}",
                throwable = e,
                recordId = recordId,
                stepName = stepName,
                attempt = 1,
                elapsedMs = elapsedMs
            )
            AppLogger.e("LLM_API", errorMsg, e)
            throw com.example.japanesegrammarapp.domain.model.LlmApiFailedException(
                primaryProvider,
                lastException?.localizedMessage,
                true,
                backupProvider,
                e.localizedMessage,
                e
            )
        }
    }
}
