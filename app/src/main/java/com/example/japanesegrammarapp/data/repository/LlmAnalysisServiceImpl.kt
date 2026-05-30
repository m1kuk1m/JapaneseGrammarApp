package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.domain.repository.LlmAnalysisService
import com.example.japanesegrammarapp.domain.repository.LlmRepository
import com.example.japanesegrammarapp.domain.repository.LlmApiConfig
import com.example.japanesegrammarapp.domain.repository.LlmResultMetadata
import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.model.TokenizationResult
import com.example.japanesegrammarapp.network.PromptManager
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
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit
    ): Pair<TokenizationResult?, LlmResultMetadata> {
        val systemPrompt = if (isOcrMode) {
            PromptManager.SYSTEM_PROMPT_TOKENIZER_OCR
        } else {
            PromptManager.SYSTEM_PROMPT_TOKENIZER
        }

        val userPrompt = if (isOcrMode) {
            "分析対象のOCRテキスト: \"$text\"\nこのテキストのOCR誤認識（て・で誤認、濁点脱落など）を自动修正した上で、トークン化し、文字列の配列として出力してください。"
        } else {
            if (text.isNotBlank()) {
                "分析対象の文: \"$text\"\nこの文をトークン化し、文字列の配列として出力してください。"
            } else {
                "画像内の日本語テキストをトークン化し、文字列の配列として出力してください。"
            }
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
            val parsed = gson.fromJson(clean, clazz)
            return Pair(parsed, LlmResultMetadata(result.consumedTokens, result.inputTokens, result.outputTokens))
        } catch (e: retrofit2.HttpException) {
            val body = e.response()?.errorBody()?.string() ?: ""
            val safeBody = if (body.length > 200) body.take(200) + "..." else body
            throw Exception("HTTP ${e.code()}: ${e.message()}\n$safeBody", e)
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
