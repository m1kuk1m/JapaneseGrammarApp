package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.*
import com.example.japanesegrammarapp.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
data class AnalysisProgress(
    val tokenizerCompleted: Boolean = false,
    val segmentsCompleted: Boolean = false,
    val clausesCompleted: Boolean = false,
    val translationCompleted: Boolean = false,
    val grammarCompleted: Boolean = false
)

@Singleton
class AnalyzeTextUseCase @Inject constructor(
    private val saveAnalysisRecordUseCase: SaveAnalysisRecordUseCase,
    private val getOcrTextUseCase: GetOcrTextUseCase,
    private val llmAnalysisService: LlmAnalysisService,
    private val detailedResultSerializer: DetailedResultSerializer,
    private val eventBus: AnalysisEventBus,
    private val settingsRepository: SettingsRepository
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Int, Job>()

    private val _progressFlow = MutableStateFlow<Map<Int, AnalysisProgress>>(emptyMap())
    val progressFlow: StateFlow<Map<Int, AnalysisProgress>> = _progressFlow.asStateFlow()

    suspend fun execute(
        text: String,
        imageUri: String?,
        provider: String,
        modelName: String,
        baseUrl: String,
        apiKey: String
    ): Int {
        if (text.isBlank() && imageUri.isNullOrBlank()) {
            throw IllegalArgumentException("Please enter text or capture an image.")
        }
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Missing API Key.")
        }

        // Fast-path for explicit text input: if a record already exists, return it directly
        if (text.isNotBlank()) {
            val duplicate = saveAnalysisRecordUseCase.getByOriginalText(text)
            if (duplicate != null) {
                return duplicate.id
            }
        }

        val record = AnalysisDomainRecord(
            originalText = text.ifBlank { "" },
            imageUri = imageUri,
            analysisResult = null,
            modelUsed = "$provider: $modelName",
            status = AnalysisStatus.PENDING
        )
        val recordId = saveAnalysisRecordUseCase.insert(record).toInt()

        val job = launchBackgroundAnalysis(recordId, text, imageUri)
        activeJobs[recordId] = job
        job.invokeOnCompletion { activeJobs.remove(recordId) }

        return recordId
    }

    suspend fun executeRetry(recordId: Int, text: String, imageUri: String?) {
        val job = launchBackgroundAnalysis(recordId, text, imageUri)
        activeJobs[recordId] = job
        job.invokeOnCompletion { activeJobs.remove(recordId) }
    }

    fun cancel(recordId: Int) {
        val job = activeJobs.remove(recordId)
        job?.cancel()
        repositoryScope.launch {
            val record = saveAnalysisRecordUseCase.getById(recordId)
            if (record != null) {
                saveAnalysisRecordUseCase.delete(record)
            }
        }
    }

    fun close() {
        repositoryScope.cancel()
    }

    private fun launchBackgroundAnalysis(
        recordId: Int,
        text: String,
        imageUri: String?
    ): Job {
        return repositoryScope.launch(Dispatchers.IO) {
            _progressFlow.update { it + (recordId to AnalysisProgress()) }
            var partialResult = DetailedAnalysisResult()
            val partialResultMutex = kotlinx.coroutines.sync.Mutex()

            suspend fun updatePartialResult(updateBlock: (DetailedAnalysisResult) -> DetailedAnalysisResult) {
                partialResultMutex.withLock {
                    try {
                        partialResult = updateBlock(partialResult)
                        val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                        if (currentRecord != null) {
                            val mergedResult = detailedResultSerializer.toJson(partialResult)
                            saveAnalysisRecordUseCase.update(
                                currentRecord.copy(
                                    analysisResult = mergedResult,
                                    consumedTokens = partialResult.consumedTokens,
                                    inputTokens = partialResult.inputTokens,
                                    outputTokens = partialResult.outputTokens
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            try {
                // Fetch LLM API Configurations from Settings
                val primaryProvider = settingsRepository.getActiveProvider()
                val primaryBase = settingsRepository.getBaseProviderType(primaryProvider)
                val primaryModel = settingsRepository.getActiveModel(primaryProvider)
                val primaryKey = settingsRepository.getApiKey(primaryProvider)
                val primaryUrl = settingsRepository.getApiUrl(primaryProvider)

                val backupProvider = settingsRepository.getBackupProvider()
                val backupBase = if (backupProvider.isNotBlank()) settingsRepository.getBaseProviderType(backupProvider) else ""
                val backupModel = settingsRepository.getBackupModel()
                val backupKey = if (backupProvider.isNotBlank()) settingsRepository.getApiKey(backupProvider) else ""
                val backupUrl = if (backupProvider.isNotBlank()) settingsRepository.getApiUrl(backupProvider) else ""

                val primaryConfig = LlmApiConfig(primaryProvider, primaryBase, primaryModel, primaryUrl, primaryKey)
                val backupConfig = if (backupProvider.isNotBlank() && backupModel.isNotBlank() && backupKey.isNotBlank()) {
                    LlmApiConfig(backupProvider, backupBase, backupModel, backupUrl, backupKey)
                } else null

                // Perform OCR if needed
                val isOcrEnabled = settingsRepository.getUseOcr()
                val imageTokenizerMode = settingsRepository.getImageTokenizerMode()
                val ocrResult = getOcrTextUseCase.execute(text, imageUri, isOcrEnabled, recordId)

                val isOcrMode = ocrResult.isOcrMode
                val ocrText = ocrResult.ocrText
                val imageBase64 = ocrResult.imagePayload?.base64Data
                val mimeType = ocrResult.imagePayload?.mimeType

                val getRetryListener = { step: AnalysisStep ->
                    { attempt: Int ->
                        repositoryScope.launch {
                            eventBus.post(AnalysisEvent.LlmRetryTriggered(recordId, step, attempt))
                        }
                        Unit
                    }
                }

                val getBackupListener = { step: AnalysisStep ->
                    { backupProv: String ->
                        repositoryScope.launch {
                            eventBus.post(AnalysisEvent.LlmBackupTriggered(recordId, step, backupProv))
                        }
                        Unit
                    }
                }

                if (isOcrMode) {
                    // ==========================================
                    // OCR Mode Flow (Sequential Tokenizer first)
                    // ==========================================
                    val tokenRes = llmAnalysisService.executeTokenizer(
                        text = ocrText,
                        imageBase64 = null,
                        mimeType = null,
                        isOcrMode = true,
                        imageTokenizerMode = imageTokenizerMode,
                        primaryConfig = primaryConfig,
                        backupConfig = backupConfig,
                        onRetry = getRetryListener(AnalysisStep.TOKENIZATION),
                        onBackup = getBackupListener(AnalysisStep.TOKENIZATION)
                    )
                    val tokenObj = tokenRes.first
                    val metadata = tokenRes.second
                    val tokens = tokenObj?.tokens ?: emptyList()
                    val correctedText = tokenObj?.correctedText

                    var effectiveText = ocrText
                    if (!correctedText.isNullOrBlank() && correctedText != ocrText) {
                        effectiveText = correctedText
                        eventBus.post(AnalysisEvent.SpellingCorrectedTriggered(recordId, effectiveText))
                    }

                    val duplicateRecord = saveAnalysisRecordUseCase.getByOriginalText(effectiveText)
                    if (duplicateRecord != null && duplicateRecord.id != recordId) {
                        eventBus.post(AnalysisEvent.DuplicateFound(recordId, duplicateRecord.id))
                        return@launch
                    }

                    val skeletonSegments = tokens.map { WordSegment(text = it) }
                    if (effectiveText != text) {
                        val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                        if (currentRecord != null) {
                            saveAnalysisRecordUseCase.update(currentRecord.copy(originalText = effectiveText))
                        }
                    }
                    updatePartialResult { current ->
                        var updated = current
                        if (skeletonSegments.isNotEmpty()) {
                            updated = updated.copy(segments = skeletonSegments)
                        }
                        updated.copy(
                            consumedTokens = updated.consumedTokens + metadata.consumedTokens,
                            inputTokens = updated.inputTokens + metadata.inputTokens,
                            outputTokens = updated.outputTokens + metadata.outputTokens
                        )
                    }

                    _progressFlow.update { map ->
                        val current = map[recordId] ?: AnalysisProgress()
                        map + (recordId to current.copy(tokenizerCompleted = true))
                    }

                    supervisorScope {
                        // Translation
                        launch {
                            try {
                                val res = llmAnalysisService.executeTranslation(effectiveText, null, null, primaryConfig, backupConfig, getRetryListener(AnalysisStep.TRANSLATION), getBackupListener(AnalysisStep.TRANSLATION))
                                val obj = res.first
                                val meta = res.second
                                updatePartialResult { current ->
                                    current.copy(
                                        translation = obj?.translation,
                                        consumedTokens = current.consumedTokens + meta.consumedTokens,
                                        inputTokens = current.inputTokens + meta.inputTokens,
                                        outputTokens = current.outputTokens + meta.outputTokens
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(translationCompleted = true))
                                }
                            }
                        }

                        // Clauses
                        launch {
                            try {
                                val res = llmAnalysisService.executeClauses(effectiveText, null, null, primaryConfig, backupConfig, getRetryListener(AnalysisStep.CLAUSE_ANALYSIS), getBackupListener(AnalysisStep.CLAUSE_ANALYSIS))
                                val obj = res.first
                                val meta = res.second
                                updatePartialResult { current ->
                                    current.copy(
                                        clauses = obj?.clauses,
                                        consumedTokens = current.consumedTokens + meta.consumedTokens,
                                        inputTokens = current.inputTokens + meta.inputTokens,
                                        outputTokens = current.outputTokens + meta.outputTokens
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(clausesCompleted = true))
                                }
                            }
                        }

                        // Grammar
                        launch {
                            try {
                                val res = llmAnalysisService.executeGrammar(effectiveText, null, null, primaryConfig, backupConfig, getRetryListener(AnalysisStep.GRAMMAR_EXPLANATION), getBackupListener(AnalysisStep.GRAMMAR_EXPLANATION))
                                val obj = res.first
                                val meta = res.second
                                updatePartialResult { current ->
                                    current.copy(
                                        grammarPoints = obj?.grammarPoints,
                                        consumedTokens = current.consumedTokens + meta.consumedTokens,
                                        inputTokens = current.inputTokens + meta.inputTokens,
                                        outputTokens = current.outputTokens + meta.outputTokens
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(grammarCompleted = true))
                                }
                            }
                        }

                        // Segments (detailed segmentation analysis)
                        launch {
                            try {
                                val res = llmAnalysisService.executeSegments(effectiveText, tokens, null, null, primaryConfig, backupConfig, getRetryListener(AnalysisStep.DETAILED_GRAMMAR), getBackupListener(AnalysisStep.DETAILED_GRAMMAR))
                                val obj = res.first
                                val meta = res.second
                                updatePartialResult { current ->
                                    val nextSegments = obj?.segments ?: current.segments
                                    current.copy(
                                        segments = nextSegments,
                                        consumedTokens = current.consumedTokens + meta.consumedTokens,
                                        inputTokens = current.inputTokens + meta.inputTokens,
                                        outputTokens = current.outputTokens + meta.outputTokens
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(segmentsCompleted = true))
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // Non-OCR Mode Flow (Tokenizer, Translation, Clauses, Grammar in parallel. Segments runs after Tokenizer.)
                    // ==========================================
                    if (imageBase64 != null) {
                        // ==========================================
                        // Direct Image Upload Mode Flow (Sequential, Image uploaded to Tokenizer only)
                        // ==========================================
                        val tokenRes = llmAnalysisService.executeTokenizer(
                            text = "",
                            imageBase64 = imageBase64,
                            mimeType = mimeType,
                            isOcrMode = false,
                            imageTokenizerMode = imageTokenizerMode,
                            primaryConfig = primaryConfig,
                            backupConfig = backupConfig,
                            onRetry = getRetryListener(AnalysisStep.TOKENIZATION),
                            onBackup = getBackupListener(AnalysisStep.TOKENIZATION)
                        )
                        val tokenObj = tokenRes.first
                        val metadata = tokenRes.second
                        val tokens = tokenObj?.tokens ?: emptyList()
                        val recognizedText = tokenObj?.recognizedText

                        var effectiveText = text
                        if (!recognizedText.isNullOrBlank()) {
                            effectiveText = recognizedText
                        } else if (tokens.isNotEmpty()) {
                            effectiveText = tokens.joinToString("")
                        }

                        val duplicateRecord = saveAnalysisRecordUseCase.getByOriginalText(effectiveText)
                        if (duplicateRecord != null && duplicateRecord.id != recordId) {
                            eventBus.post(AnalysisEvent.DuplicateFound(recordId, duplicateRecord.id))
                            return@launch
                        }

                        val skeletonSegments = tokens.map { WordSegment(text = it) }
                        if (effectiveText != text) {
                            val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                            if (currentRecord != null) {
                                saveAnalysisRecordUseCase.update(currentRecord.copy(originalText = effectiveText))
                            }
                        }
                        updatePartialResult { current ->
                            var updated = current
                            if (skeletonSegments.isNotEmpty()) {
                                updated = updated.copy(segments = skeletonSegments)
                            }
                            updated.copy(
                                consumedTokens = updated.consumedTokens + metadata.consumedTokens,
                                inputTokens = updated.inputTokens + metadata.inputTokens,
                                outputTokens = updated.outputTokens + metadata.outputTokens
                            )
                        }

                        _progressFlow.update { map ->
                            val current = map[recordId] ?: AnalysisProgress()
                            map + (recordId to current.copy(tokenizerCompleted = true))
                        }

                        supervisorScope {
                            // 1. Translation
                            launch {
                                try {
                                    val res = llmAnalysisService.executeTranslation(effectiveText, null, null, primaryConfig, backupConfig, getRetryListener(AnalysisStep.TRANSLATION), getBackupListener(AnalysisStep.TRANSLATION))
                                    val obj = res.first
                                    val meta = res.second
                                    updatePartialResult { current ->
                                        current.copy(
                                            translation = obj?.translation,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    _progressFlow.update { map ->
                                        val current = map[recordId] ?: AnalysisProgress()
                                        map + (recordId to current.copy(translationCompleted = true))
                                    }
                                }
                            }

                            // 2. Clauses
                            launch {
                                try {
                                    val res = llmAnalysisService.executeClauses(effectiveText, null, null, primaryConfig, backupConfig, getRetryListener(AnalysisStep.CLAUSE_ANALYSIS), getBackupListener(AnalysisStep.CLAUSE_ANALYSIS))
                                    val obj = res.first
                                    val meta = res.second
                                    updatePartialResult { current ->
                                        current.copy(
                                            clauses = obj?.clauses,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    _progressFlow.update { map ->
                                        val current = map[recordId] ?: AnalysisProgress()
                                        map + (recordId to current.copy(clausesCompleted = true))
                                    }
                                }
                            }

                            // 3. Grammar
                            launch {
                                try {
                                    val res = llmAnalysisService.executeGrammar(effectiveText, null, null, primaryConfig, backupConfig, getRetryListener(AnalysisStep.GRAMMAR_EXPLANATION), getBackupListener(AnalysisStep.GRAMMAR_EXPLANATION))
                                    val obj = res.first
                                    val meta = res.second
                                    updatePartialResult { current ->
                                        current.copy(
                                            grammarPoints = obj?.grammarPoints,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    _progressFlow.update { map ->
                                        val current = map[recordId] ?: AnalysisProgress()
                                        map + (recordId to current.copy(grammarCompleted = true))
                                    }
                                }
                            }

                            // 4. Segments
                            launch {
                                try {
                                    val res = llmAnalysisService.executeSegments(effectiveText, tokens, null, null, primaryConfig, backupConfig, getRetryListener(AnalysisStep.DETAILED_GRAMMAR), getBackupListener(AnalysisStep.DETAILED_GRAMMAR))
                                    val obj = res.first
                                    val meta = res.second
                                    updatePartialResult { current ->
                                        val nextSegments = obj?.segments ?: current.segments
                                        current.copy(
                                            segments = nextSegments,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    _progressFlow.update { map ->
                                        val current = map[recordId] ?: AnalysisProgress()
                                        map + (recordId to current.copy(segmentsCompleted = true))
                                    }
                                }
                            }
                        }
                    } else {
                        // ==========================================
                        // Direct Text Mode Flow (Tokenizer, Translation, Clauses, Grammar in parallel. Segments runs after Tokenizer.)
                        // ==========================================
                        supervisorScope {
                            // 1. Translation
                            launch {
                                try {
                                    val res = llmAnalysisService.executeTranslation(text, imageBase64, mimeType, primaryConfig, backupConfig, getRetryListener(AnalysisStep.TRANSLATION), getBackupListener(AnalysisStep.TRANSLATION))
                                    val obj = res.first
                                    val meta = res.second
                                    updatePartialResult { current ->
                                        current.copy(
                                            translation = obj?.translation,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    _progressFlow.update { map ->
                                        val current = map[recordId] ?: AnalysisProgress()
                                        map + (recordId to current.copy(translationCompleted = true))
                                    }
                                }
                            }

                            // 2. Clauses
                            launch {
                                try {
                                    val res = llmAnalysisService.executeClauses(text, imageBase64, mimeType, primaryConfig, backupConfig, getRetryListener(AnalysisStep.CLAUSE_ANALYSIS), getBackupListener(AnalysisStep.CLAUSE_ANALYSIS))
                                    val obj = res.first
                                    val meta = res.second
                                    updatePartialResult { current ->
                                        current.copy(
                                            clauses = obj?.clauses,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    _progressFlow.update { map ->
                                        val current = map[recordId] ?: AnalysisProgress()
                                        map + (recordId to current.copy(clausesCompleted = true))
                                    }
                                }
                            }

                            // 3. Grammar
                            launch {
                                try {
                                    val res = llmAnalysisService.executeGrammar(text, imageBase64, mimeType, primaryConfig, backupConfig, getRetryListener(AnalysisStep.GRAMMAR_EXPLANATION), getBackupListener(AnalysisStep.GRAMMAR_EXPLANATION))
                                    val obj = res.first
                                    val meta = res.second
                                    updatePartialResult { current ->
                                        current.copy(
                                            grammarPoints = obj?.grammarPoints,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    _progressFlow.update { map ->
                                        val current = map[recordId] ?: AnalysisProgress()
                                        map + (recordId to current.copy(grammarCompleted = true))
                                    }
                                }
                            }

                            // 4. Tokenizer & Segments Sequence
                            launch {
                                try {
                                    // 4a. Execute Tokenizer first
                                    val tokenRes = llmAnalysisService.executeTokenizer(
                                        text = text,
                                        imageBase64 = imageBase64,
                                        mimeType = mimeType,
                                        isOcrMode = false,
                                        imageTokenizerMode = imageTokenizerMode,
                                        primaryConfig = primaryConfig,
                                        backupConfig = backupConfig,
                                        onRetry = getRetryListener(AnalysisStep.TOKENIZATION),
                                        onBackup = getBackupListener(AnalysisStep.TOKENIZATION)
                                    )
                                    val tokenObj = tokenRes.first
                                    val tokenMeta = tokenRes.second
                                    val tokens = tokenObj?.tokens ?: emptyList()

                                    val skeletonSegments = tokens.map { WordSegment(text = it) }
                                    updatePartialResult { current ->
                                        var updated = current
                                        if (skeletonSegments.isNotEmpty()) {
                                            updated = updated.copy(segments = skeletonSegments)
                                        }
                                        updated.copy(
                                            consumedTokens = updated.consumedTokens + tokenMeta.consumedTokens,
                                            inputTokens = updated.inputTokens + tokenMeta.inputTokens,
                                            outputTokens = updated.outputTokens + tokenMeta.outputTokens
                                        )
                                    }

                                    _progressFlow.update { map ->
                                        val current = map[recordId] ?: AnalysisProgress()
                                        map + (recordId to current.copy(tokenizerCompleted = true))
                                    }

                                    // 4b. Execute detailed segments analysis using the retrieved tokens
                                    val resSeg = llmAnalysisService.executeSegments(
                                        text = text,
                                        tokens = tokens,
                                        imageBase64 = imageBase64,
                                        mimeType = mimeType,
                                        primaryConfig = primaryConfig,
                                        backupConfig = backupConfig,
                                        onRetry = getRetryListener(AnalysisStep.DETAILED_GRAMMAR),
                                        onBackup = getBackupListener(AnalysisStep.DETAILED_GRAMMAR)
                                    )
                                    val segObj = resSeg.first
                                    val segMeta = resSeg.second

                                    updatePartialResult { current ->
                                        val nextSegments = segObj?.segments ?: current.segments
                                        current.copy(
                                            segments = nextSegments,
                                            consumedTokens = current.consumedTokens + segMeta.consumedTokens,
                                            inputTokens = current.inputTokens + segMeta.inputTokens,
                                            outputTokens = current.outputTokens + segMeta.outputTokens
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    _progressFlow.update { map ->
                                        val current = map[recordId] ?: AnalysisProgress()
                                        map + (recordId to current.copy(segmentsCompleted = true))
                                    }
                                }
                            }
                        }
                    }
                }

                val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                if (currentRecord != null) {
                    var updatedText = currentRecord.originalText
                    if (updatedText.isBlank()) {
                        val combinedSentence = partialResultMutex.withLock {
                            partialResult.segments?.joinToString("") { it.text ?: "" } ?: ""
                        }
                        if (combinedSentence.isNotBlank()) {
                            updatedText = combinedSentence
                        }
                    }
                    val finalResultJson = partialResultMutex.withLock {
                        detailedResultSerializer.toJson(partialResult)
                    }
                    val finalResultSnapshot = partialResultMutex.withLock { partialResult }
                    val updatedRecord = currentRecord.copy(
                        originalText = updatedText,
                        analysisResult = finalResultJson,
                        status = AnalysisStatus.COMPLETED,
                        consumedTokens = finalResultSnapshot.consumedTokens,
                        inputTokens = finalResultSnapshot.inputTokens,
                        outputTokens = finalResultSnapshot.outputTokens
                    )
                    saveAnalysisRecordUseCase.update(updatedRecord)

                    eventBus.post(AnalysisEvent.TaskCompleted(recordId, updatedText, updatedText.length > 10))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                com.example.japanesegrammarapp.utils.AppLogger.e("LLM_API", "Background analysis execution failed for recordId: $recordId", e)
                e.printStackTrace()

                val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                if (currentRecord != null) {
                    val fullMessage = buildString {
                        append(e.localizedMessage ?: "Unknown network error")
                        append("\n\n--- Stack Trace ---\n")
                        append(e.stackTraceToString().take(1500))
                        if (e.stackTraceToString().length > 1500) {
                            append("...")
                        }
                    }
                    saveAnalysisRecordUseCase.update(
                        currentRecord.copy(
                            status = AnalysisStatus.FAILED,
                            errorMessage = fullMessage
                        )
                    )
                    eventBus.post(AnalysisEvent.TaskFailed(recordId, e))
                }
            } finally {
                _progressFlow.update { it - recordId }
            }
        }
    }

    fun parseDetailedResult(originalText: String, jsonString: String?): DetailedAnalysisResult? {
        return detailedResultSerializer.fromJson(jsonString)
    }
}
