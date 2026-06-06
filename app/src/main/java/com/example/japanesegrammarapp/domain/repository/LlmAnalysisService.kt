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
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {},
        recordId: Int? = null,
        stepName: String? = null
    ): Pair<TokenizationResult?, LlmResultMetadata>

    suspend fun executeTranslation(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {},
        recordId: Int? = null,
        stepName: String? = null
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata>

    suspend fun executeClauses(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {},
        recordId: Int? = null,
        stepName: String? = null
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata>

    suspend fun executeGrammar(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {},
        recordId: Int? = null,
        stepName: String? = null
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata>

    suspend fun executeSegments(
        text: String,
        tokens: List<String>,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit = {},
        onBackup: (backupProvider: String) -> Unit = {},
        recordId: Int? = null,
        stepName: String? = null
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata>

}

data class LlmResultMetadata(
    val consumedTokens: Int,
    val inputTokens: Int,
    val outputTokens: Int
)
