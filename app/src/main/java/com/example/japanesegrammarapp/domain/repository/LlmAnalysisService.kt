package com.example.japanesegrammarapp.domain.repository

import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.model.TokenizationResult

interface LlmAnalysisService {
    suspend fun executeTokenizer(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        isOcrMode: Boolean,
        imageTokenizerMode: String = "faithful",
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {}
    ): Pair<TokenizationResult?, LlmResultMetadata>

    suspend fun executeTranslation(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {}
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata>

    suspend fun executeClauses(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {}
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata>

    suspend fun executeGrammar(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {}
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata>

    suspend fun executeSegments(
        text: String,
        tokens: List<String>,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {}
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata>

}

data class LlmResultMetadata(
    val consumedTokens: Int,
    val inputTokens: Int,
    val outputTokens: Int
)
