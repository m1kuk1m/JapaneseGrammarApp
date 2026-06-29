package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.*
import com.example.japanesegrammarapp.domain.repository.*
import com.example.japanesegrammarapp.domain.ApplicationScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class AnalysisProgress(
    val tokenizerCompleted: Boolean = false,
    val segmentsCompleted: Boolean = false,
    val clausesCompleted: Boolean = false,
    val translationCompleted: Boolean = false,
    val grammarCompleted: Boolean = false,
    val partialSegments: List<WordSegment>? = null,
    val stepErrors: Map<String, String> = emptyMap()
)

interface AnalysisTaskManager {
    val progressFlow: StateFlow<Map<Int, AnalysisProgress>>

    suspend fun execute(
        text: String,
        imageUri: String?,
        provider: String,
        modelName: String,
        baseUrl: String,
        apiKey: String
    ): Int

    suspend fun executeRetry(recordId: Int, text: String, imageUri: String?)
    fun cancel(recordId: Int)
    fun parseDetailedResult(originalText: String, jsonString: String?): DetailedAnalysisResult?
}

@Singleton
class DefaultAnalysisTaskManager @Inject constructor(
    private val saveAnalysisRecordUseCase: SaveAnalysisRecordUseCase,
    private val getOcrTextUseCase: GetOcrTextUseCase,
    private val llmAnalysisService: LlmAnalysisService,
    private val detailedResultSerializer: DetailedResultSerializer,
    private val eventBus: AnalysisEventBus,
    private val settingsRepository: SettingsRepository,
    private val appLogWriter: AppLogWriter,
    @ApplicationScope private val repositoryScope: CoroutineScope
) : AnalysisTaskManager {
    private val activeJobs = ConcurrentHashMap<Int, Job>()
    private val progressStore = AnalysisProgressStore()
    override val progressFlow: StateFlow<Map<Int, AnalysisProgress>> = progressStore.progressFlow

    override suspend fun execute(
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
        val configuredPrimaryEndpoints = settingsRepository.buildLlmApiConfigs(provider, modelName)
        val hasCompatibleLegacyKey = apiKey.isNotBlank()
        if (configuredPrimaryEndpoints.isEmpty() && !hasCompatibleLegacyKey) {
            throw IllegalArgumentException("Missing API Key.")
        }

        // Prevent duplicate concurrent analysis for the same text
        if (text.isNotBlank()) {
            val duplicate = saveAnalysisRecordUseCase.getByOriginalText(text)
            if (duplicate != null) {
                val isRunning = activeJobs.containsKey(duplicate.id)
                if (isRunning) {
                    return duplicate.id
                } else if (duplicate.status == AnalysisStatus.FAILED || duplicate.status == AnalysisStatus.PENDING) {
                    // Zombie PENDING or FAILED record: restart background analysis
                    executeRetry(duplicate.id, text, duplicate.imageUri)
                    return duplicate.id
                } else if (duplicate.status == AnalysisStatus.COMPLETED) {
                    return duplicate.id
                }
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

    override suspend fun executeRetry(recordId: Int, text: String, imageUri: String?) {
        val job = launchBackgroundAnalysis(recordId, text, imageUri)
        activeJobs[recordId] = job
        job.invokeOnCompletion { activeJobs.remove(recordId) }
    }

    override fun cancel(recordId: Int) {
        val job = activeJobs.remove(recordId)
        job?.cancel()
        repositoryScope.launch {
            val record = saveAnalysisRecordUseCase.getById(recordId)
            if (record != null) {
                saveAnalysisRecordUseCase.delete(record)
            }
        }
    }

    private fun launchBackgroundAnalysis(
        recordId: Int,
        text: String,
        imageUri: String?
    ): Job {
        return repositoryScope.launch(Dispatchers.IO) {
            progressStore.start(recordId)
            val partialResultStore = AnalysisPartialResultStore(
                recordId = recordId,
                saveAnalysisRecordUseCase = saveAnalysisRecordUseCase,
                detailedResultSerializer = detailedResultSerializer,
                appLogWriter = appLogWriter
            )

            try {
                // Fetch LLM API configurations from the provider endpoint pools.
                val primaryProvider = settingsRepository.getActiveProvider()
                val primaryModel = settingsRepository.getActiveModel(primaryProvider)
                val primaryConfigs = settingsRepository.buildLlmApiConfigs(primaryProvider, primaryModel)
                if (primaryConfigs.isEmpty()) {
                    throw IllegalArgumentException("Missing API Key.")
                }

                val backupProvider = settingsRepository.getBackupProvider()
                val backupModel = settingsRepository.getBackupModel()
                val backupConfigs = if (backupProvider.isNotBlank() && backupModel.isNotBlank()) {
                    settingsRepository.buildLlmApiConfigs(backupProvider, backupModel)
                } else {
                    emptyList()
                }

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
                    var tokens = emptyList<String>()
                    var correctedText: String? = null

                    try {
                        llmAnalysisService.executeTokenizer(
                            text = ocrText,
                            imageBase64 = null,
                            mimeType = null,
                            isOcrMode = true,
                            imageTokenizerMode = imageTokenizerMode,
                            primaryConfigs = primaryConfigs,
                            backupConfigs = backupConfigs,
                            onRetry = getRetryListener(AnalysisStep.TOKENIZATION),
                            onBackup = getBackupListener(AnalysisStep.TOKENIZATION),
                            recordId = recordId,
                            stepName = AnalysisStep.TOKENIZATION.name
                        ).collect { res ->
                            val tokenObj = res.first
                            val metadata = res.second
                            tokens = tokenObj?.tokens ?: emptyList()
                            correctedText = tokenObj?.correctedText

                            val skeletonSegments = tokens.map { WordSegment(text = it) }
                            progressStore.updatePartialSegments(recordId, skeletonSegments)
                            partialResultStore.update { current ->
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
                        }
                    } catch (e: Exception) {
                        logStepFailure(recordId, AnalysisStep.TOKENIZATION, e)
                        progressStore.markStepError(recordId, AnalysisStep.TOKENIZATION.name, e.localizedMessage ?: "Tokenizer failed")
                        
                        val fallbackToken = if (ocrText.isNotBlank()) ocrText else "読み取り失敗"
                        tokens = listOf(fallbackToken)
                        correctedText = ocrText
                        
                        val skeletonSegments = tokens.map { WordSegment(text = it) }
                        progressStore.updatePartialSegments(recordId, skeletonSegments)
                        partialResultStore.update { current ->
                            current.copy(segments = skeletonSegments)
                        }
                    }

                    var effectiveText = ocrText
                    val finalCorrectedText = correctedText
                    if (!finalCorrectedText.isNullOrBlank() && finalCorrectedText != ocrText) {
                        effectiveText = finalCorrectedText
                        eventBus.post(AnalysisEvent.SpellingCorrectedTriggered(recordId, effectiveText))
                    }

                    val duplicateRecord = saveAnalysisRecordUseCase.getByOriginalText(effectiveText)
                    if (duplicateRecord != null && duplicateRecord.id != recordId) {
                        // Atomically clean up the newly created record before notifying the UI,
                        // so the event handler does NOT need to call cancelAnalysis() 鈥?which
                        // would be a no-op or race against invokeOnCompletion anyway.
                        val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                        if (currentRecord != null) {
                            saveAnalysisRecordUseCase.delete(currentRecord)
                        }
                        eventBus.post(AnalysisEvent.DuplicateFound(recordId, duplicateRecord.id))
                        return@launch
                    }

                    if (effectiveText != text) {
                        val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                        if (currentRecord != null) {
                            saveAnalysisRecordUseCase.update(currentRecord.copy(originalText = effectiveText))
                        }
                    }

                    progressStore.markTokenizerCompleted(recordId)

                    supervisorScope {
                        // Translation
                        launch {
                            try {
                                llmAnalysisService.executeTranslation(
                                    text = effectiveText,
                                    imageBase64 = null,
                                    mimeType = null,
                                    primaryConfigs = primaryConfigs,
                                    backupConfigs = backupConfigs,
                                    onRetry = getRetryListener(AnalysisStep.TRANSLATION),
                                    onBackup = getBackupListener(AnalysisStep.TRANSLATION),
                                    recordId = recordId,
                                    stepName = AnalysisStep.TRANSLATION.name
                                ).collect { res ->
                                    val obj = res.first
                                    val meta = res.second
                                    partialResultStore.update { current ->
                                    current.copy(
                                        translation = obj?.translation,
                                        consumedTokens = current.consumedTokens + meta.consumedTokens,
                                        inputTokens = current.inputTokens + meta.inputTokens,
                                        outputTokens = current.outputTokens + meta.outputTokens
                                    )
                                }
                                }
                            } catch (e: Exception) {
                                logStepFailure(recordId, AnalysisStep.TRANSLATION, e)
                                progressStore.markStepError(recordId, AnalysisStep.TRANSLATION.name, e.localizedMessage ?: "Unknown error")
                            } finally {
                                progressStore.markTranslationCompleted(recordId)
                            }
                        }

                        // Clauses
                        launch {
                            try {
                                llmAnalysisService.executeClauses(
                                    text = effectiveText,
                                    imageBase64 = null,
                                    mimeType = null,
                                    primaryConfigs = primaryConfigs,
                                    backupConfigs = backupConfigs,
                                    onRetry = getRetryListener(AnalysisStep.CLAUSE_ANALYSIS),
                                    onBackup = getBackupListener(AnalysisStep.CLAUSE_ANALYSIS),
                                    recordId = recordId,
                                    stepName = AnalysisStep.CLAUSE_ANALYSIS.name
                                ).collect { res ->
                                    val obj = res.first
                                    val meta = res.second
                                    partialResultStore.update { current ->
                                    current.copy(
                                        clauses = obj?.clauses,
                                        consumedTokens = current.consumedTokens + meta.consumedTokens,
                                        inputTokens = current.inputTokens + meta.inputTokens,
                                        outputTokens = current.outputTokens + meta.outputTokens
                                    )
                                }
                                }
                            } catch (e: Exception) {
                                logStepFailure(recordId, AnalysisStep.CLAUSE_ANALYSIS, e)
                                progressStore.markStepError(recordId, AnalysisStep.CLAUSE_ANALYSIS.name, e.localizedMessage ?: "Unknown error")
                            } finally {
                                progressStore.markClausesCompleted(recordId)
                            }
                        }

                        // Grammar
                        launch {
                            try {
                                llmAnalysisService.executeGrammar(
                                    text = effectiveText,
                                    imageBase64 = null,
                                    mimeType = null,
                                    primaryConfigs = primaryConfigs,
                                    backupConfigs = backupConfigs,
                                    onRetry = getRetryListener(AnalysisStep.GRAMMAR_EXPLANATION),
                                    onBackup = getBackupListener(AnalysisStep.GRAMMAR_EXPLANATION),
                                    recordId = recordId,
                                    stepName = AnalysisStep.GRAMMAR_EXPLANATION.name
                                ).collect { res ->
                                    val obj = res.first
                                    val meta = res.second
                                    partialResultStore.update { current ->
                                    current.copy(
                                        grammarPoints = obj?.grammarPoints,
                                        consumedTokens = current.consumedTokens + meta.consumedTokens,
                                        inputTokens = current.inputTokens + meta.inputTokens,
                                        outputTokens = current.outputTokens + meta.outputTokens
                                    )
                                }
                                }
                            } catch (e: Exception) {
                                logStepFailure(recordId, AnalysisStep.GRAMMAR_EXPLANATION, e)
                                progressStore.markStepError(recordId, AnalysisStep.GRAMMAR_EXPLANATION.name, e.localizedMessage ?: "Unknown error")
                            } finally {
                                progressStore.markGrammarCompleted(recordId)
                            }
                        }

                        // Segments (detailed segmentation analysis)
                        launch {
                            try {
                                llmAnalysisService.executeSegments(
                                    text = effectiveText,
                                    tokens = tokens,
                                    imageBase64 = null,
                                    mimeType = null,
                                    primaryConfigs = primaryConfigs,
                                    backupConfigs = backupConfigs,
                                    onRetry = getRetryListener(AnalysisStep.DETAILED_GRAMMAR),
                                    onBackup = getBackupListener(AnalysisStep.DETAILED_GRAMMAR),
                                    recordId = recordId,
                                    stepName = AnalysisStep.DETAILED_GRAMMAR.name
                                ).collect { res ->
                                    val obj = res.first
                                    val meta = res.second
                                    obj?.segments?.let { progressStore.updatePartialSegments(recordId, it) }
                                    partialResultStore.update { current ->
                                    val nextSegments = obj?.segments ?: current.segments
                                    current.copy(
                                        segments = nextSegments,
                                        consumedTokens = current.consumedTokens + meta.consumedTokens,
                                        inputTokens = current.inputTokens + meta.inputTokens,
                                        outputTokens = current.outputTokens + meta.outputTokens
                                    )
                                }
                                }
                            } catch (e: Exception) {
                                logStepFailure(recordId, AnalysisStep.DETAILED_GRAMMAR, e)
                                progressStore.markStepError(recordId, AnalysisStep.DETAILED_GRAMMAR.name, e.localizedMessage ?: "Unknown error")
                            } finally {
                                progressStore.markSegmentsCompleted(recordId)
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
                        var tokens = emptyList<String>()
                        var recognizedText: String? = null

                        try {
                            llmAnalysisService.executeTokenizer(
                                text = "",
                                imageBase64 = imageBase64,
                                mimeType = mimeType,
                                isOcrMode = false,
                                imageTokenizerMode = imageTokenizerMode,
                                primaryConfigs = primaryConfigs,
                                backupConfigs = backupConfigs,
                                onRetry = getRetryListener(AnalysisStep.TOKENIZATION),
                                onBackup = getBackupListener(AnalysisStep.TOKENIZATION),
                                recordId = recordId,
                                stepName = AnalysisStep.TOKENIZATION.name
                            ).collect { res ->
                                val tokenObj = res.first
                                val metadata = res.second
                                tokens = tokenObj?.tokens ?: emptyList()
                                recognizedText = tokenObj?.recognizedText

                                val skeletonSegments = tokens.map { WordSegment(text = it) }
                                progressStore.updatePartialSegments(recordId, skeletonSegments)
                                partialResultStore.update { current ->
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
                            }
                        } catch (e: Exception) {
                            logStepFailure(recordId, AnalysisStep.TOKENIZATION, e)
                            progressStore.markStepError(recordId, AnalysisStep.TOKENIZATION.name, e.localizedMessage ?: "Tokenizer failed")
                            
                            tokens = listOf("画像認識失敗")
                            recognizedText = "画像認識失敗"
                            
                            val skeletonSegments = tokens.map { WordSegment(text = it) }
                            progressStore.updatePartialSegments(recordId, skeletonSegments)
                            partialResultStore.update { current ->
                                current.copy(segments = skeletonSegments)
                            }
                        }

                        var effectiveText = text
                        val finalRecognizedText = recognizedText
                        if (!finalRecognizedText.isNullOrBlank()) {
                            effectiveText = finalRecognizedText
                        } else if (tokens.isNotEmpty()) {
                            effectiveText = tokens.joinToString("")
                        }

                        val duplicateRecord = saveAnalysisRecordUseCase.getByOriginalText(effectiveText)
                        if (duplicateRecord != null && duplicateRecord.id != recordId) {
                            // Atomically clean up the newly created record before notifying the UI.
                            val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                            if (currentRecord != null) {
                                saveAnalysisRecordUseCase.delete(currentRecord)
                            }
                            eventBus.post(AnalysisEvent.DuplicateFound(recordId, duplicateRecord.id))
                            return@launch
                        }

                        if (effectiveText != text) {
                            val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                            if (currentRecord != null) {
                                saveAnalysisRecordUseCase.update(currentRecord.copy(originalText = effectiveText))
                            }
                        }

                        progressStore.markTokenizerCompleted(recordId)

                        supervisorScope {
                            // 1. Translation
                            launch {
                                try {
                                    llmAnalysisService.executeTranslation(
                                        text = effectiveText,
                                        imageBase64 = null,
                                        mimeType = null,
                                        primaryConfigs = primaryConfigs,
                                        backupConfigs = backupConfigs,
                                        onRetry = getRetryListener(AnalysisStep.TRANSLATION),
                                        onBackup = getBackupListener(AnalysisStep.TRANSLATION),
                                        recordId = recordId,
                                        stepName = AnalysisStep.TRANSLATION.name
                                    ).collect { res ->
                                        val obj = res.first
                                        val meta = res.second
                                        partialResultStore.update { current ->
                                        current.copy(
                                            translation = obj?.translation,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                    }
                                } catch (e: Exception) {
                                    logStepFailure(recordId, AnalysisStep.TRANSLATION, e)
                                    progressStore.markStepError(recordId, AnalysisStep.TRANSLATION.name, e.localizedMessage ?: "Unknown error")
                                } finally {
                                    progressStore.markTranslationCompleted(recordId)
                                }
                            }

                            // 2. Clauses
                            launch {
                                try {
                                    llmAnalysisService.executeClauses(
                                        text = effectiveText,
                                        imageBase64 = null,
                                        mimeType = null,
                                        primaryConfigs = primaryConfigs,
                                        backupConfigs = backupConfigs,
                                        onRetry = getRetryListener(AnalysisStep.CLAUSE_ANALYSIS),
                                        onBackup = getBackupListener(AnalysisStep.CLAUSE_ANALYSIS),
                                        recordId = recordId,
                                        stepName = AnalysisStep.CLAUSE_ANALYSIS.name
                                    ).collect { res ->
                                        val obj = res.first
                                        val meta = res.second
                                        partialResultStore.update { current ->
                                        current.copy(
                                            clauses = obj?.clauses,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                    }
                                } catch (e: Exception) {
                                    logStepFailure(recordId, AnalysisStep.CLAUSE_ANALYSIS, e)
                                    progressStore.markStepError(recordId, AnalysisStep.CLAUSE_ANALYSIS.name, e.localizedMessage ?: "Unknown error")
                                } finally {
                                    progressStore.markClausesCompleted(recordId)
                                }
                            }

                            // 3. Grammar
                            launch {
                                try {
                                    llmAnalysisService.executeGrammar(
                                        text = effectiveText,
                                        imageBase64 = null,
                                        mimeType = null,
                                        primaryConfigs = primaryConfigs,
                                        backupConfigs = backupConfigs,
                                        onRetry = getRetryListener(AnalysisStep.GRAMMAR_EXPLANATION),
                                        onBackup = getBackupListener(AnalysisStep.GRAMMAR_EXPLANATION),
                                        recordId = recordId,
                                        stepName = AnalysisStep.GRAMMAR_EXPLANATION.name
                                    ).collect { res ->
                                        val obj = res.first
                                        val meta = res.second
                                        partialResultStore.update { current ->
                                        current.copy(
                                            grammarPoints = obj?.grammarPoints,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                    }
                                } catch (e: Exception) {
                                    logStepFailure(recordId, AnalysisStep.GRAMMAR_EXPLANATION, e)
                                    progressStore.markStepError(recordId, AnalysisStep.GRAMMAR_EXPLANATION.name, e.localizedMessage ?: "Unknown error")
                                } finally {
                                    progressStore.markGrammarCompleted(recordId)
                                }
                            }

                            // 4. Segments
                            launch {
                                try {
                                    llmAnalysisService.executeSegments(
                                        text = effectiveText,
                                        tokens = tokens,
                                        imageBase64 = null,
                                        mimeType = null,
                                        primaryConfigs = primaryConfigs,
                                        backupConfigs = backupConfigs,
                                        onRetry = getRetryListener(AnalysisStep.DETAILED_GRAMMAR),
                                        onBackup = getBackupListener(AnalysisStep.DETAILED_GRAMMAR),
                                        recordId = recordId,
                                        stepName = AnalysisStep.DETAILED_GRAMMAR.name
                                    ).collect { res ->
                                        val obj = res.first
                                        val meta = res.second
                                        obj?.segments?.let { progressStore.updatePartialSegments(recordId, it) }
                                        partialResultStore.update { current ->
                                        val nextSegments = obj?.segments ?: current.segments
                                        current.copy(
                                            segments = nextSegments,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                    }
                                } catch (e: Exception) {
                                    logStepFailure(recordId, AnalysisStep.DETAILED_GRAMMAR, e)
                                    progressStore.markStepError(recordId, AnalysisStep.DETAILED_GRAMMAR.name, e.localizedMessage ?: "Unknown error")
                                } finally {
                                    progressStore.markSegmentsCompleted(recordId)
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
                                    llmAnalysisService.executeTranslation(
                                        text = text,
                                        imageBase64 = imageBase64,
                                        mimeType = mimeType,
                                        primaryConfigs = primaryConfigs,
                                        backupConfigs = backupConfigs,
                                        onRetry = getRetryListener(AnalysisStep.TRANSLATION),
                                        onBackup = getBackupListener(AnalysisStep.TRANSLATION),
                                        recordId = recordId,
                                        stepName = AnalysisStep.TRANSLATION.name
                                    ).collect { res ->
                                        val obj = res.first
                                        val meta = res.second
                                        partialResultStore.update { current ->
                                        current.copy(
                                            translation = obj?.translation,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                    }
                                } catch (e: Exception) {
                                    logStepFailure(recordId, AnalysisStep.TRANSLATION, e)
                                    progressStore.markStepError(recordId, AnalysisStep.TRANSLATION.name, e.localizedMessage ?: "Unknown error")
                                } finally {
                                    progressStore.markTranslationCompleted(recordId)
                                }
                            }

                            // 2. Clauses
                            launch {
                                try {
                                    llmAnalysisService.executeClauses(
                                        text = text,
                                        imageBase64 = imageBase64,
                                        mimeType = mimeType,
                                        primaryConfigs = primaryConfigs,
                                        backupConfigs = backupConfigs,
                                        onRetry = getRetryListener(AnalysisStep.CLAUSE_ANALYSIS),
                                        onBackup = getBackupListener(AnalysisStep.CLAUSE_ANALYSIS),
                                        recordId = recordId,
                                        stepName = AnalysisStep.CLAUSE_ANALYSIS.name
                                    ).collect { res ->
                                        val obj = res.first
                                        val meta = res.second
                                        partialResultStore.update { current ->
                                        current.copy(
                                            clauses = obj?.clauses,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                    }
                                } catch (e: Exception) {
                                    logStepFailure(recordId, AnalysisStep.CLAUSE_ANALYSIS, e)
                                    progressStore.markStepError(recordId, AnalysisStep.CLAUSE_ANALYSIS.name, e.localizedMessage ?: "Unknown error")
                                } finally {
                                    progressStore.markClausesCompleted(recordId)
                                }
                            }

                            // 3. Grammar
                            launch {
                                try {
                                    llmAnalysisService.executeGrammar(
                                        text = text,
                                        imageBase64 = imageBase64,
                                        mimeType = mimeType,
                                        primaryConfigs = primaryConfigs,
                                        backupConfigs = backupConfigs,
                                        onRetry = getRetryListener(AnalysisStep.GRAMMAR_EXPLANATION),
                                        onBackup = getBackupListener(AnalysisStep.GRAMMAR_EXPLANATION),
                                        recordId = recordId,
                                        stepName = AnalysisStep.GRAMMAR_EXPLANATION.name
                                    ).collect { res ->
                                        val obj = res.first
                                        val meta = res.second
                                        partialResultStore.update { current ->
                                        current.copy(
                                            grammarPoints = obj?.grammarPoints,
                                            consumedTokens = current.consumedTokens + meta.consumedTokens,
                                            inputTokens = current.inputTokens + meta.inputTokens,
                                            outputTokens = current.outputTokens + meta.outputTokens
                                        )
                                    }
                                    }
                                } catch (e: Exception) {
                                    logStepFailure(recordId, AnalysisStep.GRAMMAR_EXPLANATION, e)
                                    progressStore.markStepError(recordId, AnalysisStep.GRAMMAR_EXPLANATION.name, e.localizedMessage ?: "Unknown error")
                                } finally {
                                    progressStore.markGrammarCompleted(recordId)
                                }
                            }

                            // 4. Tokenizer & Segments Sequence
                            launch {
                                var tokens = emptyList<String>()
                                try {
                                    // 4a. Execute Tokenizer first
                                    llmAnalysisService.executeTokenizer(
                                        text = text,
                                        imageBase64 = imageBase64,
                                        mimeType = mimeType,
                                        isOcrMode = false,
                                        imageTokenizerMode = imageTokenizerMode,
                                        primaryConfigs = primaryConfigs,
                                        backupConfigs = backupConfigs,
                                        onRetry = getRetryListener(AnalysisStep.TOKENIZATION),
                                        onBackup = getBackupListener(AnalysisStep.TOKENIZATION),
                                        recordId = recordId,
                                        stepName = AnalysisStep.TOKENIZATION.name
                                    ).collect { res ->
                                        val tokenObj = res.first
                                        val tokenMeta = res.second
                                        tokens = tokenObj?.tokens ?: emptyList()

                                        val skeletonSegments = tokens.map { WordSegment(text = it) }
                                        progressStore.updatePartialSegments(recordId, skeletonSegments)
                                        partialResultStore.update { current ->
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
                                    }
                                } catch (e: Exception) {
                                    logStepFailure(recordId, AnalysisStep.TOKENIZATION, e)
                                    progressStore.markStepError(recordId, AnalysisStep.TOKENIZATION.name, e.localizedMessage ?: "Tokenizer failed")
                                    val fallbackToken = if (text.isNotBlank()) text else "読み取り失敗"
                                    tokens = listOf(fallbackToken)
                                    val skeletonSegments = tokens.map { WordSegment(text = it) }
                                    progressStore.updatePartialSegments(recordId, skeletonSegments)
                                    partialResultStore.update { current ->
                                        current.copy(segments = skeletonSegments)
                                    }
                                } finally {
                                    progressStore.markTokenizerCompleted(recordId)
                                }

                                try {
                                    // 4b. Execute detailed segments analysis using the retrieved tokens
                                    llmAnalysisService.executeSegments(
                                        text = text,
                                        tokens = tokens,
                                        imageBase64 = imageBase64,
                                        mimeType = mimeType,
                                        primaryConfigs = primaryConfigs,
                                        backupConfigs = backupConfigs,
                                        onRetry = getRetryListener(AnalysisStep.DETAILED_GRAMMAR),
                                        onBackup = getBackupListener(AnalysisStep.DETAILED_GRAMMAR),
                                        recordId = recordId,
                                        stepName = AnalysisStep.DETAILED_GRAMMAR.name
                                    ).collect { resSeg ->
                                        val segObj = resSeg.first
                                        val segMeta = resSeg.second
                                        segObj?.segments?.let { progressStore.updatePartialSegments(recordId, it) }
                                        partialResultStore.update { current ->
                                            val nextSegments = segObj?.segments ?: current.segments
                                            current.copy(
                                                segments = nextSegments,
                                                consumedTokens = current.consumedTokens + segMeta.consumedTokens,
                                                inputTokens = current.inputTokens + segMeta.inputTokens,
                                                outputTokens = current.outputTokens + segMeta.outputTokens
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    logStepFailure(recordId, AnalysisStep.DETAILED_GRAMMAR, e)
                                    progressStore.markStepError(recordId, AnalysisStep.DETAILED_GRAMMAR.name, e.localizedMessage ?: "Unknown error")
                                } finally {
                                    progressStore.markSegmentsCompleted(recordId)
                                }
                            }
                        }
                    }
                }

                val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                if (currentRecord != null) {
                    var updatedText = currentRecord.originalText
                    if (updatedText.isBlank()) {
                        val combinedSentence = partialResultStore.combinedSegmentText()
                        if (combinedSentence.isNotBlank()) {
                            updatedText = combinedSentence
                        }
                    }
                    val finalResultJson = partialResultStore.toJson()
                    val finalResultSnapshot = partialResultStore.snapshot()
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
                appLogWriter.error("LLM_API", "Background analysis execution failed for recordId: $recordId", e)

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
                progressStore.finish(recordId)
            }
        }
    }

    private fun logStepFailure(recordId: Int, step: AnalysisStep, error: Exception) {
        appLogWriter.error(
            "LLM_STEP",
            "Analysis step ${step.name} failed for recordId: $recordId",
            error
        )
    }

    override fun parseDetailedResult(originalText: String, jsonString: String?): DetailedAnalysisResult? {
        return detailedResultSerializer.fromJson(jsonString)
    }
}
