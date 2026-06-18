package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.domain.model.DailyTokenUsage
import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.model.LlmConfig
import com.example.japanesegrammarapp.domain.model.LlmEndpoint
import com.example.japanesegrammarapp.domain.model.ModelTokenUsage
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectionSettings
import com.example.japanesegrammarapp.domain.model.TokenizationResult
import com.example.japanesegrammarapp.domain.repository.DetailedResultSerializer
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import com.example.japanesegrammarapp.domain.repository.ImageAttachmentLoader
import com.example.japanesegrammarapp.domain.repository.LlmAnalysisService
import com.example.japanesegrammarapp.domain.repository.LlmApiConfig
import com.example.japanesegrammarapp.domain.repository.LlmResultMetadata
import com.example.japanesegrammarapp.domain.repository.OcrRepository
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.domain.repository.AppLogWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisTaskManagerTest {
    @Test
    fun executeStartsNewAnalysisWhenCompletedDuplicateExists() = runBlocking {
        val history = FakeHistoryRepository(
            AnalysisDomainRecord(
                id = 5,
                originalText = "日本語",
                modelUsed = "Gemini: gemini-test",
                status = AnalysisStatus.COMPLETED
            )
        )
        val llm = FakeLlmAnalysisService()
        val manager = newManager(history = history, llm = llm)

        val id = manager.execute(
            text = "日本語",
            imageUri = null,
            provider = "Gemini",
            modelName = "gemini-test",
            baseUrl = "https://example.test",
            apiKey = "key"
        )

        org.junit.Assert.assertNotEquals(5, id)
        waitUntil { history.getRecordById(id)?.status == AnalysisStatus.COMPLETED }
        assertEquals(1, llm.tokenizerCalls)
        assertTrue(manager.progressFlow.value.isEmpty())
    }

    @Test
    fun cancelDeletesPendingRecordAndClearsProgress() = runBlocking {
        val history = FakeHistoryRepository()
        val llm = FakeLlmAnalysisService(delayMs = 250)
        val manager = newManager(history = history, llm = llm)

        val id = manager.execute(
            text = "キャンセル",
            imageUri = null,
            provider = "Gemini",
            modelName = "gemini-test",
            baseUrl = "https://example.test",
            apiKey = "key"
        )

        manager.cancel(id)
        waitUntil { history.getRecordById(id) == null && !manager.progressFlow.value.containsKey(id) }

        assertEquals(null, history.getRecordById(id))
        assertFalse(manager.progressFlow.value.containsKey(id))
    }

    @Test
    fun backgroundFailureMarksRecordFailedAndClearsProgress() = runBlocking {
        val history = FakeHistoryRepository()
        val manager = newManager(
            history = history,
            settings = FakeSettingsRepository(emptyConfigsAfterFirstLookup = true)
        )

        val id = manager.execute(
            text = "失敗",
            imageUri = null,
            provider = "Gemini",
            modelName = "gemini-test",
            baseUrl = "https://example.test",
            apiKey = "key"
        )

        waitUntil { history.getRecordById(id)?.status == AnalysisStatus.FAILED }
        val record = checkNotNull(history.getRecordById(id))

        assertEquals(AnalysisStatus.FAILED, record.status)
        assertTrue(record.errorMessage.orEmpty().contains("Missing API Key"))
        assertFalse(manager.progressFlow.value.containsKey(id))
    }

    private fun newManager(
        history: FakeHistoryRepository,
        llm: FakeLlmAnalysisService = FakeLlmAnalysisService(),
        settings: FakeSettingsRepository = FakeSettingsRepository()
    ): DefaultAnalysisTaskManager {
        val eventBus = AnalysisEventBus()
        return DefaultAnalysisTaskManager(
            saveAnalysisRecordUseCase = SaveAnalysisRecordUseCase(history),
            getOcrTextUseCase = GetOcrTextUseCase(
                ocrRepository = FakeOcrRepository(),
                imageLoader = FakeImageAttachmentLoader(),
                eventBus = eventBus
            ),
            llmAnalysisService = llm,
            detailedResultSerializer = FakeDetailedResultSerializer(),
            eventBus = eventBus,
            settingsRepository = settings,
            appLogWriter = NoOpAppLogWriter,
            repositoryScope = CoroutineScope(Dispatchers.Default)
        )
    }

    private suspend fun waitUntil(predicate: suspend () -> Boolean) {
        repeat(80) {
            if (predicate()) return
            delay(25)
        }
        error("Condition was not met in time")
    }
}

private object NoOpAppLogWriter : AppLogWriter {
    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}

private class FakeLlmAnalysisService(
    private val delayMs: Long = 0,
    private val failTranslation: Boolean = false
) : LlmAnalysisService {
    var tokenizerCalls = 0

    override suspend fun executeTokenizer(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        isOcrMode: Boolean,
        imageTokenizerMode: String,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): kotlinx.coroutines.flow.Flow<Pair<TokenizationResult?, LlmResultMetadata>> = kotlinx.coroutines.flow.flow {
        tokenizerCalls++
        if (delayMs > 0) delay(delayMs)
        emit(TokenizationResult(tokens = text.toList().map { it.toString() }) to metadata())
    }

    override suspend fun executeTranslation(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): kotlinx.coroutines.flow.Flow<Pair<DetailedAnalysisResult?, LlmResultMetadata>> = kotlinx.coroutines.flow.flow {
        if (failTranslation) error("translation failed")
        if (delayMs > 0) delay(delayMs)
        emit(DetailedAnalysisResult(translation = "translated") to metadata())
    }

    override suspend fun executeClauses(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): kotlinx.coroutines.flow.Flow<Pair<DetailedAnalysisResult?, LlmResultMetadata>> = kotlinx.coroutines.flow.flow {
        if (delayMs > 0) delay(delayMs)
        emit(DetailedAnalysisResult() to metadata())
    }

    override suspend fun executeGrammar(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): kotlinx.coroutines.flow.Flow<Pair<DetailedAnalysisResult?, LlmResultMetadata>> = kotlinx.coroutines.flow.flow {
        if (delayMs > 0) delay(delayMs)
        emit(DetailedAnalysisResult() to metadata())
    }

    override suspend fun executeSegments(
        text: String,
        tokens: List<String>,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): kotlinx.coroutines.flow.Flow<Pair<DetailedAnalysisResult?, LlmResultMetadata>> = kotlinx.coroutines.flow.flow {
        if (delayMs > 0) delay(delayMs)
        emit(DetailedAnalysisResult() to metadata())
    }

    private fun metadata() = LlmResultMetadata(consumedTokens = 1, inputTokens = 1, outputTokens = 0)
}

private class FakeSettingsRepository(
    private val emptyConfigsAfterFirstLookup: Boolean = false
) : SettingsRepository {
    private var configLookupCount = 0
    private val config = LlmApiConfig(
        provider = "Gemini",
        baseProvider = "Gemini",
        modelName = "gemini-test",
        url = "https://example.test",
        key = "key",
        endpointId = "default_gemini",
        endpointName = "Default"
    )

    override fun getAllProviders(): List<String> = LlmConfig.providers
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
    override fun buildLlmApiConfigs(provider: String, modelName: String): List<LlmApiConfig> {
        configLookupCount++
        if (emptyConfigsAfterFirstLookup && configLookupCount > 1) {
            return emptyList()
        }
        return listOf(config.copy(provider = provider, modelName = modelName))
    }
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
}

private class FakeHistoryRepository(vararg initialRecords: AnalysisDomainRecord) : HistoryRepository {
    private val records = linkedMapOf<Int, AnalysisDomainRecord>()
    private var nextId = 1

    init {
        initialRecords.forEach { record ->
            records[record.id] = record
            nextId = maxOf(nextId, record.id + 1)
        }
    }

    override suspend fun getAllRecordsList(): List<AnalysisDomainRecord> = records.values.toList()
    override suspend fun getRecordById(id: Int): AnalysisDomainRecord? = synchronized(records) { records[id] }
    override fun observeRecordById(id: Int): Flow<AnalysisDomainRecord?> = MutableStateFlow(records[id])
    override suspend fun getRecordByOriginalText(originalText: String): AnalysisDomainRecord? =
        synchronized(records) { records.values.firstOrNull { it.originalText == originalText } }

    override suspend fun insertRecord(record: AnalysisDomainRecord): Long = synchronized(records) {
        val id = nextId++
        records[id] = record.copy(id = id)
        id.toLong()
    }

    override suspend fun updateRecord(record: AnalysisDomainRecord) {
        synchronized(records) { records[record.id] = record }
    }

    override suspend fun deleteRecord(record: AnalysisDomainRecord) {
        synchronized(records) { records.remove(record.id) }
    }

    override val totalTokensConsumed: Flow<Int?> = MutableStateFlow(0)
    override val tokenUsageByModel: Flow<List<ModelTokenUsage>> = MutableStateFlow(emptyList())
    override val dailyTokenUsage: Flow<List<DailyTokenUsage>> = MutableStateFlow(emptyList())
}

private class FakeDetailedResultSerializer : DetailedResultSerializer {
    override fun toJson(result: DetailedAnalysisResult): String {
        return "translation=${result.translation};tokens=${result.consumedTokens}/${result.inputTokens}/${result.outputTokens}"
    }

    override fun fromJson(json: String?): DetailedAnalysisResult? = null
}

private class FakeOcrRepository : OcrRepository {
    override suspend fun extractTextFromImage(imageUri: String): String = ""
}

private class FakeImageAttachmentLoader : ImageAttachmentLoader {
    override suspend fun loadAsBase64(uriString: String) = null
}
