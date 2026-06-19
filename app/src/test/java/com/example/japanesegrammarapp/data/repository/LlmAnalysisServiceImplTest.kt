package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.domain.model.LlmEndpoint
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectionSettings
import com.example.japanesegrammarapp.domain.repository.LlmApiConfig
import com.example.japanesegrammarapp.domain.repository.LlmRepository
import com.example.japanesegrammarapp.domain.repository.LlmResult
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmAnalysisServiceImplTest {
    @Test
    fun executeSegmentsTreatsMultiCharacterEllipsisAsPunctuation() = runBlocking {
        assertEllipsisTokenIsKeptOutOfDetailedAnalysis("……")
        assertEllipsisTokenIsKeptOutOfDetailedAnalysis("......")
    }

    @Test
    fun executeSegmentsDropsWhitespaceOnlyTokensFromDisplaySegments() = runBlocking {
        val repository = FakeLlmRepository(
            response = """
                あの|あの||
                極端に|きょくたんに||
                蹴落とさない|けおとさない||
                で|で||
                もらえます|もらえます||
            """.trimIndent()
        )
        val service = LlmAnalysisServiceImpl(
            llmRepository = repository,
            settingsRepository = FakeSettingsRepository,
            gson = Gson()
        )

        val (result, _) = service.executeSegments(
            text = "あの、極端に\n蹴落とさないでもらえます？",
            tokens = listOf("あの", "、", "極端に", "\n", "蹴落とさない", "で", "もらえます", "？"),
            imageBase64 = null,
            mimeType = null,
            primaryConfigs = listOf(testConfig),
            backupConfigs = emptyList()
        ).last()

        assertTrue(repository.lastUserPrompt.contains("あの, 極端に, 蹴落とさない, で, もらえます"))
        assertEquals(
            listOf("あの", "、", "極端に", "蹴落とさない", "で", "もらえます", "？"),
            result?.segments?.map { it.text }
        )
    }

    private suspend fun assertEllipsisTokenIsKeptOutOfDetailedAnalysis(ellipsisToken: String) {
        val repository = FakeLlmRepository(
            response = """
                あたし|あたし||
                みなしご|みなしご||
                なの|なの||
            """.trimIndent()
        )
        val service = LlmAnalysisServiceImpl(
            llmRepository = repository,
            settingsRepository = FakeSettingsRepository,
            gson = Gson()
        )

        val (result, _) = service.executeSegments(
            text = "${ellipsisToken}あたし、みなしごなの。",
            tokens = listOf(ellipsisToken, "あたし", "、", "みなしご", "なの", "。"),
            imageBase64 = null,
            mimeType = null,
            primaryConfigs = listOf(testConfig),
            backupConfigs = emptyList()
        ).last()

        assertTrue(repository.lastUserPrompt.contains("あたし, みなしご, なの"))
        assertEquals(
            listOf(ellipsisToken, "あたし", "、", "みなしご", "なの", "。"),
            result?.segments?.map { it.text }
        )
        assertEquals("補助記号", result?.segments?.firstOrNull()?.partOfSpeech)
    }

    @Test
    fun executeSegmentsFiltersPunctuationAndUnknownSymbolsLocally() = runBlocking {
        val repository = FakeLlmRepository(
            response = """
                あの|あの||
                元気|げんき||
            """.trimIndent()
        )
        val service = LlmAnalysisServiceImpl(
            llmRepository = repository,
            settingsRepository = FakeSettingsRepository,
            gson = Gson()
        )

        val (result, _) = service.executeSegments(
            text = "あの★元気",
            tokens = listOf("あの", "★", "元気"),
            imageBase64 = null,
            mimeType = null,
            primaryConfigs = listOf(testConfig),
            backupConfigs = emptyList()
        ).last()

        assertTrue(repository.lastUserPrompt.contains("あの, 元気"))
        // The reconstructed final segments should correctly put ★ back in the middle
        assertEquals(
            listOf("あの", "★", "元気"),
            result?.segments?.map { it.text }
        )
        assertEquals("補助記号", result?.segments?.get(1)?.partOfSpeech)
        assertEquals("きごう", result?.segments?.get(1)?.reading)
    }

    @Test
    fun executeSegmentsFiltersCJKDoubleQuotesAndProvidesCustomDescriptions() = runBlocking {
        val repository = FakeLlmRepository(
            response = """
                はなまる|はなまる||
                大正解|たいせいかい||
            """.trimIndent()
        )
        val service = LlmAnalysisServiceImpl(
            llmRepository = repository,
            settingsRepository = FakeSettingsRepository,
            gson = Gson()
        )

        val (result, _) = service.executeSegments(
            text = "〝はなまる大正解〟",
            tokens = listOf("〝", "はなまる", "大正解", "〟"),
            imageBase64 = null,
            mimeType = null,
            primaryConfigs = listOf(testConfig),
            backupConfigs = emptyList()
        ).last()

        assertTrue(repository.lastUserPrompt.contains("はなまる, 大正解"))
        // The final segments should be perfectly aligned with symbols reconstructed locally
        assertEquals(
            listOf("〝", "はなまる", "大正解", "〟"),
            result?.segments?.map { it.text }
        )
        assertEquals("（前双引号）", result?.segments?.get(0)?.meaning)
        assertEquals("（后双引号）", result?.segments?.get(3)?.meaning)
    }

    private companion object {
        val testConfig = LlmApiConfig(
            provider = "Gemini",
            baseProvider = "Gemini",
            modelName = "gemini-test",
            url = "https://example.test",
            key = "key"
        )
    }
}

private class FakeLlmRepository(
    private val response: String
) : LlmRepository {
    var lastUserPrompt: String = ""
        private set

    override suspend fun fetchModels(provider: String, baseUrl: String, apiKey: String): List<String> {
        error("Not used")
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
        error("Not used")
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
        lastUserPrompt = userPrompt
        return LlmResult(
            text = response,
            consumedTokens = 3,
            inputTokens = 2,
            outputTokens = 1,
            provider = "Gemini",
            modelName = "gemini-test"
        )
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
    ): kotlinx.coroutines.flow.Flow<com.example.japanesegrammarapp.domain.model.LlmStreamEvent> {
        lastUserPrompt = userPrompt
        return kotlinx.coroutines.flow.flowOf(com.example.japanesegrammarapp.domain.model.LlmStreamEvent.Chunk(response))
    }
}

private object FakeSettingsRepository : SettingsRepository {
    override fun getAllProviders(): List<String> = listOf("Gemini")
    override fun getBaseProviderType(providerId: String): String = providerId
    override fun getApiKey(provider: String): String = "key"
    override fun saveApiKey(provider: String, key: String): Boolean = true
    override fun getApiUrl(provider: String): String = "https://example.test"
    override fun saveApiUrl(provider: String, url: String) = Unit
    override fun getEndpoints(provider: String): List<LlmEndpoint> = emptyList()
    override fun getApiKeyForEndpoint(endpointId: String): String = "key"
    override fun saveEndpoint(endpoint: LlmEndpoint, apiKey: String?): Boolean = true
    override fun deleteEndpoint(provider: String, endpointId: String): Boolean = true
    override fun markEndpointSuccess(provider: String, endpointId: String) = Unit
    override fun markEndpointFailure(provider: String, endpointId: String, error: String, cooldownMs: Long) = Unit
    override fun touchEndpoint(provider: String, endpointId: String) = Unit
    override fun buildLlmApiConfigs(provider: String, modelName: String): List<LlmApiConfig> = listOf(
        LlmApiConfig(provider, provider, modelName, "https://example.test", "key")
    )
    override fun getActiveProvider(): String = "Gemini"
    override fun setActiveProvider(provider: String) = Unit
    override fun getActiveModel(provider: String): String = "gemini-test"
    override fun setActiveModel(provider: String, model: String) = Unit
    override fun getUseOcr(): Boolean = false
    override fun setUseOcr(value: Boolean) = Unit
    override fun getImageTokenizerMode(): String = "faithful"
    override fun setImageTokenizerMode(mode: String) = Unit
    override fun getOcrBoxDetectionSettings(): OcrBoxDetectionSettings = OcrBoxDetectionSettings.DEFAULT
    override fun setOcrBoxDetectionSettings(settings: OcrBoxDetectionSettings) = Unit
    override fun resetOcrBoxDetectionSettings() = Unit
    override fun getAutoNavigateResult(): Boolean = true
    override fun setAutoNavigateResult(value: Boolean) = Unit
    override fun getModelsForProvider(provider: String): List<String> = emptyList()
    override fun saveModelsForProvider(provider: String, models: List<String>) = Unit
    override fun getBackupProvider(): String = ""
    override fun setBackupProvider(provider: String) = Unit
    override fun getBackupModel(): String = ""
    override fun setBackupModel(model: String) = Unit
    override fun getThemeMode(): String = "System"
    override fun setThemeMode(mode: String) = Unit
    override fun getWallpaperUri(): String = ""
    override fun setWallpaperUri(uri: String) = Unit
    override val themeMode = MutableStateFlow("System")
    override val wallpaperUri = MutableStateFlow("")
    override fun getCustomPrompt(promptKey: String): String = ""
    override fun saveCustomPrompt(promptKey: String, prompt: String) = Unit
    override fun resetCustomPrompt(promptKey: String) = Unit
    override fun resetAllCustomPrompts() = Unit
    override fun getTtsProvider(): String = "OpenAI"
    override fun setTtsProvider(provider: String) = Unit
    override fun getTtsApiUrl(provider: String): String = ""
    override fun setTtsApiUrl(provider: String, url: String) = Unit
    override fun getTtsApiKey(provider: String): String = ""
    override fun setTtsApiKey(provider: String, key: String): Boolean = true
    override fun getTtsModel(provider: String): String = ""
    override fun setTtsModel(provider: String, model: String) = Unit
    override fun getTtsVoice(provider: String): String = ""
    override fun setTtsVoice(provider: String, voice: String) = Unit
    override fun getTtsRegion(provider: String): String = ""
    override fun setTtsRegion(provider: String, region: String) = Unit
    override fun getPromptPresets(): List<com.example.japanesegrammarapp.domain.model.PromptPreset> = emptyList()
    override fun getActivePromptPresetId(): String = ""
    override fun setActivePromptPresetId(id: String) = Unit
    override fun savePromptPreset(preset: com.example.japanesegrammarapp.domain.model.PromptPreset) = Unit
    override fun deletePromptPreset(id: String) = Unit
}
