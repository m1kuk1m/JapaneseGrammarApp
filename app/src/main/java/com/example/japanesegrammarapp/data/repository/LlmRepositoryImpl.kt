package com.example.japanesegrammarapp.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.japanesegrammarapp.network.*
import com.example.japanesegrammarapp.domain.model.LlmConfig
import com.example.japanesegrammarapp.domain.repository.LlmRepository
import com.example.japanesegrammarapp.domain.repository.LlmResult
import com.example.japanesegrammarapp.domain.repository.LlmApiConfig
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose

@Singleton
class LlmRepositoryImpl @Inject constructor(
    private val llmService: LlmApiService,
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: okhttp3.OkHttpClient,
    private val gson: com.google.gson.Gson
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
                val response = try {
                    llmService.generateGemini(url, request)
                } catch (e: retrofit2.HttpException) {
                    throw Exception(formatHttpError(e), e)
                }
                val text = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull { !it.text.isNullOrBlank() }
                    ?.text
                    ?: throw Exception(buildGeminiNoTextMessage(response))
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

    private fun formatHttpError(e: retrofit2.HttpException): String {
        val errorBody = e.response()?.errorBody()?.string().orEmpty()
        val safeBody = errorBody.safeTruncate(1200)
        return buildString {
            append("HTTP ").append(e.code()).append(": ").append(e.message())
            if (safeBody.isNotBlank()) {
                append("\n").append(safeBody)
            }
        }
    }

    private fun buildGeminiNoTextMessage(response: GeminiResponse): String {
        val candidate = response.candidates?.firstOrNull()
        val usage = response.usageMetadata
        return buildString {
            append("No text response from Gemini")
            append("; candidates=").append(response.candidates?.size ?: 0)

            candidate?.finishReason
                ?.takeIf { it.isNotBlank() }
                ?.let { append("; finishReason=").append(it) }

            response.promptFeedback?.blockReason
                ?.takeIf { it.isNotBlank() }
                ?.let { append("; promptBlockReason=").append(it) }

            val candidateSafety = candidate?.safetyRatings.formatSafetyRatings()
            if (candidateSafety.isNotBlank()) {
                append("; candidateSafety=[").append(candidateSafety).append("]")
            }

            val promptSafety = response.promptFeedback?.safetyRatings.formatSafetyRatings()
            if (promptSafety.isNotBlank()) {
                append("; promptSafety=[").append(promptSafety).append("]")
            }

            if (usage != null) {
                append("; tokens total=")
                    .append(usage.totalTokenCount ?: 0)
                    .append(", prompt=")
                    .append(usage.promptTokenCount ?: 0)
                    .append(", candidates=")
                    .append(usage.candidatesTokenCount ?: 0)
            }
        }
    }

    private fun List<GeminiSafetyRating>?.formatSafetyRatings(): String {
        return this.orEmpty()
            .filter { rating ->
                !rating.category.isNullOrBlank() ||
                    !rating.probability.isNullOrBlank() ||
                    rating.blocked != null
            }
            .joinToString(", ") { rating ->
                buildString {
                    append(rating.category ?: "UNKNOWN")
                    rating.probability?.takeIf { it.isNotBlank() }?.let {
                        append(":").append(it)
                    }
                    rating.blocked?.let {
                        append(":blocked=").append(it)
                    }
                }
            }
            .safeTruncate(600)
    }

    private fun String.safeTruncate(maxLength: Int): String {
        return if (length > maxLength) take(maxLength) + "\n...<truncated>" else this
    }

    private suspend fun executeSingleWithFailover(
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

    override suspend fun executeWithFailover(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        apiTypeLabel: String,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        recordId: Int?,
        stepName: String?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): LlmResult {
        val primaryProvider = primaryConfigs.firstOrNull()?.provider ?: "Main API"
        val primaryAttempt = tryConfigPool(
            configs = primaryConfigs,
            role = "Primary",
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = apiTypeLabel,
            recordId = recordId,
            stepName = stepName,
            onRetry = onRetry
        )
        primaryAttempt.result?.let { return it }

        val backupProvider = backupConfigs.firstOrNull()?.provider
        if (backupConfigs.isEmpty() || backupProvider.isNullOrBlank()) {
            throw com.example.japanesegrammarapp.domain.model.LlmApiFailedException(
                primaryProvider,
                primaryAttempt.lastException?.localizedMessage ?: "No enabled API endpoint is configured.",
                false,
                null,
                null,
                primaryAttempt.lastException
            )
        }

        AppLogger.apiEvent(
            apiTypeLabel = apiTypeLabel,
            provider = backupProvider,
            model = backupConfigs.first().modelName,
            status = "BACKUP",
            hasImage = imageBase64 != null,
            userPrompt = userPrompt,
            systemPrompt = systemPrompt,
            message = "Switching to backup endpoint pool after primary pool failed: ${primaryAttempt.lastException?.localizedMessage ?: "Unknown error"}",
            recordId = recordId,
            stepName = stepName
        )
        onBackup(backupProvider)

        val backupAttempt = tryConfigPool(
            configs = backupConfigs,
            role = "Backup",
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = apiTypeLabel,
            recordId = recordId,
            stepName = stepName,
            onRetry = {}
        )
        backupAttempt.result?.let { return it }

        throw com.example.japanesegrammarapp.domain.model.LlmApiFailedException(
            primaryProvider,
            primaryAttempt.lastException?.localizedMessage ?: "No enabled API endpoint is configured.",
            true,
            backupProvider,
            backupAttempt.lastException?.localizedMessage ?: "No enabled backup endpoint is configured.",
            backupAttempt.lastException ?: primaryAttempt.lastException
        )
    }

    private data class PoolAttempt(
        val result: LlmResult?,
        val lastException: Exception?
    )

    private suspend fun tryConfigPool(
        configs: List<LlmApiConfig>,
        role: String,
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        apiTypeLabel: String,
        recordId: Int?,
        stepName: String?,
        onRetry: (attempt: Int) -> Unit
    ): PoolAttempt {
        if (configs.isEmpty()) {
            return PoolAttempt(null, IllegalArgumentException("No enabled API endpoint is configured."))
        }

        var lastException: Exception? = null
        configs.forEachIndexed { index, config ->
            val attemptNumber = index + 1
            val attemptStartMs = System.currentTimeMillis()
            val endpointLabel = config.endpointName.ifBlank {
                config.endpointId.ifBlank { "default" }
            }
            try {
                if (config.endpointId.isNotBlank()) {
                    settingsRepository.touchEndpoint(config.provider, config.endpointId)
                }
                AppLogger.apiEvent(
                    apiTypeLabel = apiTypeLabel,
                    provider = config.provider,
                    model = config.modelName,
                    status = "START",
                    hasImage = imageBase64 != null,
                    userPrompt = userPrompt,
                    systemPrompt = systemPrompt,
                    message = "$role request attempt $attemptNumber started via endpoint $endpointLabel",
                    recordId = recordId,
                    stepName = stepName,
                    attempt = attemptNumber
                )

                val result = callLlmApi(
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    imageBase64 = imageBase64,
                    mimeType = mimeType,
                    provider = config.provider,
                    baseProvider = config.baseProvider,
                    modelName = config.modelName,
                    effectiveUrl = config.url,
                    apiKey = config.key
                )
                if (config.endpointId.isNotBlank()) {
                    settingsRepository.markEndpointSuccess(config.provider, config.endpointId)
                }
                return PoolAttempt(
                    result.copy(
                        endpointId = config.endpointId,
                        endpointName = config.endpointName
                    ),
                    null
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val elapsedMs = System.currentTimeMillis() - attemptStartMs
                lastException = e
                if (config.endpointId.isNotBlank()) {
                    settingsRepository.markEndpointFailure(
                        provider = config.provider,
                        endpointId = config.endpointId,
                        error = e.localizedMessage ?: "Unknown error",
                        cooldownMs = endpointCooldownMs(e)
                    )
                }
                AppLogger.e(
                    "LLM_API",
                    "$role LLM call failed for [$apiTypeLabel] via ${config.provider} ($endpointLabel, ${config.modelName}). Error: ${e.localizedMessage}",
                    e
                )

                if (index < configs.lastIndex) {
                    AppLogger.apiEvent(
                        apiTypeLabel = apiTypeLabel,
                        provider = config.provider,
                        model = config.modelName,
                        status = "RETRY",
                        hasImage = imageBase64 != null,
                        userPrompt = userPrompt,
                        systemPrompt = systemPrompt,
                        message = "$role endpoint $endpointLabel failed after ${elapsedMs}ms: ${e.localizedMessage ?: "Unknown error"}. Trying next endpoint.",
                        recordId = recordId,
                        stepName = stepName,
                        attempt = attemptNumber,
                        elapsedMs = elapsedMs
                    )
                    onRetry(attemptNumber)
                    delay(600L)
                } else {
                    AppLogger.apiError(
                        apiTypeLabel = apiTypeLabel,
                        provider = config.provider,
                        model = config.modelName,
                        hasImage = imageBase64 != null,
                        userPrompt = userPrompt,
                        systemPrompt = systemPrompt,
                        message = "$role endpoint $endpointLabel failed after ${elapsedMs}ms: ${e.localizedMessage ?: "Unknown error"}",
                        throwable = e,
                        recordId = recordId,
                        stepName = stepName,
                        attempt = attemptNumber,
                        elapsedMs = elapsedMs
                    )
                }
            }
        }

        return PoolAttempt(null, lastException)
    }

    private fun endpointCooldownMs(error: Exception): Long {
        val http = error.cause as? retrofit2.HttpException ?: error as? retrofit2.HttpException
        return when (http?.code()) {
            401, 403 -> 24 * 60 * 60 * 1000L
            429 -> 2 * 60 * 1000L
            in 500..599 -> 60 * 1000L
            else -> 30 * 1000L
        }
    }

    override suspend fun executeWithStreaming(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        apiTypeLabel: String,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        recordId: Int?,
        stepName: String?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): kotlinx.coroutines.flow.Flow<com.example.japanesegrammarapp.domain.model.LlmStreamEvent> = kotlinx.coroutines.flow.flow {
        val configs = primaryConfigs + backupConfigs
        if (configs.isEmpty()) throw Exception("No config available for streaming")

        var lastException: Exception? = null

        for ((index, config) in configs.withIndex()) {
            val attemptNumber = index + 1
            val isBackup = index >= primaryConfigs.size
            if (isBackup && index == primaryConfigs.size) {
                com.example.japanesegrammarapp.utils.AppLogger.apiEvent(
                    apiTypeLabel = apiTypeLabel,
                    provider = config.provider,
                    model = config.modelName,
                    status = "BACKUP",
                    hasImage = imageBase64 != null,
                    userPrompt = userPrompt,
                    systemPrompt = systemPrompt,
                    message = "Switching to backup endpoint pool after primary pool failed.",
                    recordId = recordId,
                    stepName = stepName
                )
                onBackup(config.provider)
            }

            val attemptStartMs = System.currentTimeMillis()
            val endpointLabel = config.endpointName.ifBlank {
                config.endpointId.ifBlank { "default" }
            }
            val role = if (isBackup) "Backup" else "Primary"

            try {
                com.example.japanesegrammarapp.utils.AppLogger.apiEvent(
                    apiTypeLabel = apiTypeLabel,
                    provider = config.provider,
                    model = config.modelName,
                    status = "START",
                    hasImage = imageBase64 != null,
                    userPrompt = userPrompt,
                    systemPrompt = systemPrompt,
                    message = "$role request attempt $attemptNumber started via endpoint $endpointLabel",
                    recordId = recordId,
                    stepName = stepName,
                    attempt = attemptNumber
                )
                
                if (config.endpointId.isNotBlank()) {
                    settingsRepository.touchEndpoint(config.provider, config.endpointId)
                }

                val streamFlow = kotlinx.coroutines.flow.callbackFlow {
                    val requestUrl: String
                    val requestBodyStr: String
                    
                    when (config.baseProvider) {
                        "OpenAI", "DeepSeek", "OpenAI Compatible", "Qwen" -> {
                            val cleanBase = config.url.trimEnd('/')
                            requestUrl = "$cleanBase/chat/completions"
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
                                model = config.modelName,
                                messages = listOf(
                                    OpenAiMessage(role = "system", content = systemPrompt),
                                    OpenAiMessage(role = "user", content = userContent)
                                ),
                                temperature = 0.1,
                                stream = true,
                                stream_options = mapOf("include_usage" to true)
                            )
                            requestBodyStr = gson.toJson(request)
                        }
                        "Gemini", "Vertex AI" -> {
                            val cleanBase = config.url.trimEnd('/')
                            requestUrl = "$cleanBase/models/${config.modelName}:streamGenerateContent?alt=sse&key=${config.key.trim()}"
                            
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
                                    temperature = 0.1
                                ),
                                safetySettings = safetySettings
                            )
                            requestBodyStr = gson.toJson(request)
                        }
                        else -> throw Exception("Unsupported provider for streaming")
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = requestBodyStr.toRequestBody(mediaType)
                    val httpRequest = okhttp3.Request.Builder()
                        .url(requestUrl)
                        .post(body)
                        .apply {
                            if (config.baseProvider in listOf("OpenAI", "DeepSeek", "OpenAI Compatible", "Qwen")) {
                                addHeader("Authorization", "Bearer ${config.key.trim()}")
                            }
                            addHeader("Accept", "text/event-stream")
                        }
                        .build()

                    val factory = okhttp3.sse.EventSources.createFactory(okHttpClient)
                    var currentUsage = com.example.japanesegrammarapp.domain.repository.LlmResultMetadata(0, 0, 0)
                    var hasEmittedMetadata = false

                    val eventSourceListener = object : okhttp3.sse.EventSourceListener() {
                        override fun onEvent(eventSource: okhttp3.sse.EventSource, id: String?, type: String?, data: String) {
                            if (data == "[DONE]") return
                            try {
                                var textChunk = ""
                                if (config.baseProvider in listOf("OpenAI", "DeepSeek", "OpenAI Compatible", "Qwen")) {
                                    val response = gson.fromJson(data, OpenAiResponse::class.java)
                                    val choice = response.choices?.firstOrNull()
                                    if (choice?.finish_reason == "content_filter") {
                                        throw Exception("OpenAI API Blocked: Content filter triggered (finish_reason=content_filter)")
                                    }
                                    textChunk = choice?.delta?.content ?: ""
                                    response.usage?.let { u ->
                                        currentUsage = com.example.japanesegrammarapp.domain.repository.LlmResultMetadata(
                                            consumedTokens = u.total_tokens ?: 0,
                                            inputTokens = u.prompt_tokens ?: 0,
                                            outputTokens = u.completion_tokens ?: 0
                                        )
                                    }
                                } else {
                                    val response = gson.fromJson(data, GeminiResponse::class.java)
                                    if (!response.promptFeedback?.blockReason.isNullOrBlank()) {
                                        throw Exception("Gemini API Blocked: Prompt flagged (blockReason=${response.promptFeedback?.blockReason})")
                                    }
                                    val candidate = response.candidates?.firstOrNull()
                                    val finishReason = candidate?.finishReason
                                    if (finishReason != null && finishReason != "STOP" && finishReason != "MAX_TOKENS") {
                                        throw Exception(buildGeminiNoTextMessage(response))
                                    }
                                    textChunk = candidate?.content?.parts?.firstOrNull()?.text ?: ""
                                    response.usageMetadata?.let { u ->
                                        currentUsage = com.example.japanesegrammarapp.domain.repository.LlmResultMetadata(
                                            consumedTokens = u.totalTokenCount ?: 0,
                                            inputTokens = u.promptTokenCount ?: 0,
                                            outputTokens = u.candidatesTokenCount ?: 0
                                        )
                                    }
                                }
                                if (textChunk.isNotEmpty()) {
                                    trySend(com.example.japanesegrammarapp.domain.model.LlmStreamEvent.Chunk(textChunk))
                                }
                            } catch (e: Exception) {
                                AppLogger.e("LLM_API", "Error parsing SSE chunk: $data", e)
                                close(e)
                            }
                        }

                        override fun onFailure(eventSource: okhttp3.sse.EventSource, t: Throwable?, response: okhttp3.Response?) {
                            if (t != null) {
                                AppLogger.e("LLM_API", "SSE Failure: ${t.message}", t)
                                close(t)
                            } else {
                                val errorMsg = "SSE Failure HTTP Code: ${response?.code}"
                                AppLogger.e("LLM_API", errorMsg, Exception(errorMsg))
                                close(Exception(errorMsg))
                            }
                        }

                        override fun onClosed(eventSource: okhttp3.sse.EventSource) {
                            if (!hasEmittedMetadata) {
                                hasEmittedMetadata = true
                                trySend(com.example.japanesegrammarapp.domain.model.LlmStreamEvent.Metadata(
                                    provider = config.provider,
                                    modelName = config.modelName,
                                    usage = currentUsage
                                ))
                            }
                            close()
                        }
                    }

                    val eventSource = factory.newEventSource(httpRequest, eventSourceListener)
                    awaitClose { eventSource.cancel() }
                }

                streamFlow.collect { event ->
                    emit(event)
                }

                if (config.endpointId.isNotBlank()) {
                    settingsRepository.markEndpointSuccess(config.provider, config.endpointId)
                }
                return@flow 
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val elapsedMs = System.currentTimeMillis() - attemptStartMs
                lastException = e
                if (config.endpointId.isNotBlank()) {
                    settingsRepository.markEndpointFailure(
                        provider = config.provider,
                        endpointId = config.endpointId,
                        error = e.localizedMessage ?: "Unknown error",
                        cooldownMs = endpointCooldownMs(e)
                    )
                }
                com.example.japanesegrammarapp.utils.AppLogger.e("LLM_API", "Streaming attempt $attemptNumber failed via ${config.provider}", e)
                if (index < configs.lastIndex) {
                    com.example.japanesegrammarapp.utils.AppLogger.apiEvent(
                        apiTypeLabel = apiTypeLabel,
                        provider = config.provider,
                        model = config.modelName,
                        status = "RETRY",
                        hasImage = imageBase64 != null,
                        userPrompt = userPrompt,
                        systemPrompt = systemPrompt,
                        message = "$role endpoint $endpointLabel failed after ${elapsedMs}ms: ${e.localizedMessage ?: "Unknown error"}. Trying next endpoint.",
                        recordId = recordId,
                        stepName = stepName,
                        attempt = attemptNumber,
                        elapsedMs = elapsedMs
                    )
                    onRetry(attemptNumber)
                    kotlinx.coroutines.delay(600L)
                }
            }
        }
        throw lastException ?: Exception("Streaming failed for unknown reasons")
    }
}
