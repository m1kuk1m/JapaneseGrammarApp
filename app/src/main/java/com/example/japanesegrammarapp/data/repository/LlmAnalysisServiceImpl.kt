package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.domain.repository.LlmAnalysisService
import com.example.japanesegrammarapp.domain.repository.LlmRepository
import com.example.japanesegrammarapp.domain.repository.LlmApiConfig
import com.example.japanesegrammarapp.domain.repository.LlmResultMetadata
import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.model.TokenizationResult
import com.example.japanesegrammarapp.network.PromptManager
import com.example.japanesegrammarapp.utils.AppLogger
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmAnalysisServiceImpl @Inject constructor(
    private val llmRepository: LlmRepository,
    private val gson: Gson
) : LlmAnalysisService {

    override suspend fun executeTokenizer(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        isOcrMode: Boolean,
        imageTokenizerMode: String,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): Pair<TokenizationResult?, LlmResultMetadata> {
        val systemPrompt = when {
            imageBase64 != null && imageTokenizerMode == "repair" -> PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE_REPAIR
            imageBase64 != null -> PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE
            isOcrMode -> PromptManager.SYSTEM_PROMPT_TOKENIZER_OCR
            else -> PromptManager.SYSTEM_PROMPT_TOKENIZER
        }

        val userPrompt = when {
            imageBase64 != null && imageTokenizerMode == "repair" -> "画像内の日本語テキストを読み取り、画像が不鮮明な場合は文脈・日本語としての自然さ・濁点/半濁点の有無を総合して、明らかに不合理な読み取りを補正してください。最終的な本文は recognizedText に、分かち書き結果は tokens に出力してください。"
            imageBase64 != null -> "画像内の日本語テキストを原文のまま忠実に認識し、一切修正せず、文字を変更しないでトークン化してください。画像から読み取った原文は recognizedText に、分かち書き結果は tokens に出力してください。"
            isOcrMode -> "分析対象のOCRテキスト: \"$text\"\nこのテキストのOCR誤認識（て・で誤認、濁点脱落など）を自动修正した上で、トークン化し、文字列の配列として出力してください。"
            else -> "分析対象の文: \"$text\"\nこの文をトークン化し、文字列の配列として出力してください。"
        }

        return executeAnalysisStep(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "単語分割",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = TokenizationResult::class.java
        )
    }

    override suspend fun executeTranslation(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata> {
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nこの文の自然な中国語訳を出力してください。"
        } else {
            "画像内の日本語テキストを自然な中国語（簡体字）に翻訳してください。"
        }

        return executeAnalysisStep(
            systemPrompt = PromptManager.SYSTEM_PROMPT_TRANSLATION,
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "翻訳",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = DetailedAnalysisResult::class.java
        )
    }

    override suspend fun executeClauses(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata> {
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nこの文の文節（フレーズ）ごとの文法的役割の詳細な解説を行ってください。"
        } else {
            "画像内のすべての日本語の文節構造と文法的役割を詳細に分析してください。"
        }

        return executeAnalysisStep(
            systemPrompt = PromptManager.SYSTEM_PROMPT_CLAUSES,
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "文節解析",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = DetailedAnalysisResult::class.java
        )
    }

    override suspend fun executeGrammar(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata> {
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nこの文に含まれる最も重要かつ難度の高い文法項目・慣用表現を厳選して解説してください。"
        } else {
            "画像内のすべての日本語の文法表现や固定表現を詳細に分析してください。"
        }

        return executeAnalysisStep(
            systemPrompt = PromptManager.SYSTEM_PROMPT_GRAMMAR,
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "文法解説",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = DetailedAnalysisResult::class.java
        )
    }

    override suspend fun executeSegments(
        text: String,
        tokens: List<String>,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata> {
        val tokensJson = gson.toJson(tokens)
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nユーザーが提供したトークン配列: $tokensJson\n各トークンの詳細な文法分析を行ってください。"
        } else {
            "画像内の日本語テキストのトークン配列: $tokensJson\n各トークンの詳細な文法分析を行ってください。"
        }

        return executeAnalysisStep(
            systemPrompt = PromptManager.SYSTEM_PROMPT_SEGMENTS,
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "詳細文法解析",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = DetailedAnalysisResult::class.java
        )
    }



    private suspend fun <T> executeAnalysisStep(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        apiTypeLabel: String,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        clazz: Class<T>
    ): Pair<T?, LlmResultMetadata> {
        val providerLabel = buildString {
            append(primaryConfig.provider)
            if (backupConfig != null) append(" -> ").append(backupConfig.provider)
        }
        val modelLabel = buildString {
            append(primaryConfig.modelName)
            if (backupConfig != null) append(" -> ").append(backupConfig.modelName)
        }
        try {
            val result = llmRepository.executeWithFailover(
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                imageBase64 = imageBase64,
                mimeType = mimeType,
                apiTypeLabel = apiTypeLabel,
                primaryConfig = primaryConfig,
                backupConfig = backupConfig,
                onRetry = onRetry,
                onBackup = onBackup
            )
            val clean = cleanMarkdownJson(result.text)
            val parsed = try {
                gson.fromJson(clean, clazz)
            } catch (e: Exception) {
                AppLogger.apiError(
                    apiTypeLabel = apiTypeLabel,
                    provider = providerLabel,
                    model = modelLabel,
                    hasImage = imageBase64 != null,
                    userPrompt = userPrompt,
                    systemPrompt = systemPrompt,
                    message = "JSON parse failed: ${e.localizedMessage}",
                    throwable = e,
                    rawResponse = result.text
                )
                throw e
            }
            AppLogger.apiSuccess(
                apiTypeLabel = apiTypeLabel,
                provider = result.provider ?: providerLabel,
                model = result.modelName ?: modelLabel,
                hasImage = imageBase64 != null,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                rawResponse = result.text,
                parsedPreview = clean,
                consumedTokens = result.consumedTokens,
                inputTokens = result.inputTokens,
                outputTokens = result.outputTokens
            )
            return Pair(parsed, LlmResultMetadata(result.consumedTokens, result.inputTokens, result.outputTokens))
        } catch (e: retrofit2.HttpException) {
            val body = e.response()?.errorBody()?.string() ?: ""
            val safeBody = if (body.length > 200) body.take(200) + "..." else body
            val message = "HTTP ${e.code()}: ${e.message()}\n$safeBody"
            AppLogger.apiError(
                apiTypeLabel = apiTypeLabel,
                provider = providerLabel,
                model = modelLabel,
                hasImage = imageBase64 != null,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                message = message,
                throwable = e,
                rawResponse = body
            )
            throw Exception(message, e)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AppLogger.apiError(
                apiTypeLabel = apiTypeLabel,
                provider = providerLabel,
                model = modelLabel,
                hasImage = imageBase64 != null,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                message = e.localizedMessage ?: "Unknown LLM step error",
                throwable = e
            )
            throw e
        }
    }

    private fun cleanMarkdownJson(rawJson: String): String {
        var cleanJson = rawJson.trim()
        if (cleanJson.startsWith("```")) {
            val firstNewLine = cleanJson.indexOf('\n')
            cleanJson = if (firstNewLine != -1) {
                cleanJson.substring(firstNewLine).trim()
            } else {
                cleanJson.removePrefix("```").trim()
            }
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.removeSuffix("```").trim()
        }
        return cleanJson
    }
}
