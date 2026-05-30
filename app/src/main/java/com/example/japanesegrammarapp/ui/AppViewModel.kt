package com.example.japanesegrammarapp.ui

import android.content.Context
import android.net.Uri
import com.example.japanesegrammarapp.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesegrammarapp.domain.model.*
import com.example.japanesegrammarapp.domain.repository.*
import com.example.japanesegrammarapp.domain.usecase.AnalyzeTextUseCase
import com.example.japanesegrammarapp.domain.usecase.RetryAnalysisUseCase
import com.example.japanesegrammarapp.network.LlmConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject
sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
    object NavigateToResult : UiEvent()
    data class TaskCompleted(val recordId: Int, val message: String) : UiEvent()
    data class ExportContent(val content: String, val filename: String) : UiEvent()
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val llmRepository: LlmRepository,
    private val ocrRepository: OcrRepository,
    private val ttsRepository: TtsRepository,
    private val analyzeTextUseCase: AnalyzeTextUseCase,
    private val retryAnalysisUseCase: RetryAnalysisUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    val history = historyRepository.history
    val totalTokensConsumed: StateFlow<Int> = historyRepository.totalTokensConsumed
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val tokenUsageByModel: StateFlow<List<ModelTokenUsage>> = historyRepository.tokenUsageByModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTokenUsage: StateFlow<List<DailyTokenUsage>> = historyRepository.dailyTokenUsage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPlayingTts: StateFlow<Boolean> = ttsRepository.isPlaying


    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 10)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        // Pre-warm configurations and cache in a background thread to prevent Main thread blockage
        viewModelScope.launch(Dispatchers.IO) {
            val activeProvider = settingsRepository.getActiveProvider()
            val providerModels = LlmConfig.providers.associateWith { settingsRepository.getModelsForProvider(it) }
            val activeModel = settingsRepository.getActiveModel(activeProvider)
            val useOcr = settingsRepository.getUseOcr()
            val backupProvider = settingsRepository.getBackupProvider()
            val backupModel = settingsRepository.getBackupModel()
            val themeMode = settingsRepository.getThemeMode()
            val primaryColor = settingsRepository.getPrimaryColor()
            val wallpaperUri = settingsRepository.getWallpaperUri()

            // Pre-warm all API Keys and URLs into in-memory cache
            LlmConfig.providers.forEach { provider ->
                settingsRepository.getApiKey(provider)
                settingsRepository.getApiUrl(provider)
            }

            val models = providerModels[activeProvider] ?: emptyList()
            val finalActiveModel = if (activeModel.isBlank() && models.isNotEmpty()) models.first() else activeModel

            _uiState.update {
                it.copy(
                    activeProvider = activeProvider,
                    activeModel = finalActiveModel,
                    useOcr = useOcr,
                    providerModels = providerModels,
                    availableModels = models,
                    backupProvider = backupProvider,
                    backupModel = backupModel,
                    themeMode = themeMode,
                    primaryColor = primaryColor,
                    wallpaperUri = wallpaperUri
                )
            }
        }

        // Listen for Repository event completions
        viewModelScope.launch {
            historyRepository.analysisEvents.collect { event ->
                when (event) {
                    is AnalysisEvent.TaskCompleted -> {
                        val currentSelected = _uiState.value.selectedRecord
                        if (currentSelected == null || currentSelected.id != event.recordId) {
                            _uiEvent.emit(UiEvent.TaskCompleted(event.recordId, event.message))
                        }
                    }
                    is AnalysisEvent.OcrFallbackTriggered -> {
                        _uiEvent.emit(UiEvent.ShowError(context.getString(R.string.ocr_fallback_toast)))
                    }
                    is AnalysisEvent.SpellingCorrectedTriggered -> {
                        _uiEvent.emit(UiEvent.ShowError(context.getString(R.string.spelling_corrected_toast)))
                    }
                    is AnalysisEvent.LlmRetryTriggered -> {
                        _uiEvent.emit(UiEvent.ShowError(context.getString(R.string.llm_retry_toast, event.apiTypeLabel, event.attempt)))
                    }
                    is AnalysisEvent.LlmBackupTriggered -> {
                        _uiEvent.emit(UiEvent.ShowError(context.getString(R.string.llm_backup_toast, event.apiTypeLabel, event.backupProvider)))
                    }
                }
            }
        }

        // Keep selected record and detailed parses synced with DB updates
        viewModelScope.launch {
            history.collectLatest { recordList ->
                val currentSelected = _uiState.value.selectedRecord
                if (currentSelected != null) {
                    val updated = recordList.find { it.id == currentSelected.id }
                    if (updated != null && (updated.status != currentSelected.status || 
                        updated.analysisResult != currentSelected.analysisResult || 
                        updated.originalText != currentSelected.originalText)) {
                        
                        if (updated.status == AnalysisStatus.COMPLETED || updated.status == AnalysisStatus.PENDING) {
                            _uiState.update { it.copy(isParsingDetailedResult = true) }
                            withContext(Dispatchers.IO) {
                                val detail = analyzeTextUseCase.parseDetailedResult(updated.originalText, updated.analysisResult)
                                _uiState.update { it.copy(
                                    selectedRecord = updated,
                                    currentOriginalText = updated.originalText,
                                    analysisResult = updated.analysisResult,
                                    detailedResult = detail,
                                    isParsingDetailedResult = false,
                                    selectedRecordProgress = analyzeTextUseCase.progressFlow.value[updated.id]
                                ) }
                            }
                        } else {
                            _uiState.update { it.copy(
                                selectedRecord = updated,
                                currentOriginalText = updated.originalText,
                                analysisResult = updated.analysisResult,
                                detailedResult = null,
                                isParsingDetailedResult = false,
                                selectedRecordProgress = null
                            ) }
                        }
                    }
                }
            }
        }

        // Observe real-time API progress flow from AnalyzeTextUseCase
        viewModelScope.launch {
            analyzeTextUseCase.progressFlow.collect { progressMap ->
                val currentSelected = _uiState.value.selectedRecord
                if (currentSelected != null) {
                    val progress = progressMap[currentSelected.id]
                    _uiState.update { it.copy(selectedRecordProgress = progress) }
                } else {
                    _uiState.update { it.copy(selectedRecordProgress = null) }
                }
            }
        }
    }

    fun selectRecord(record: AnalysisDomainRecord) {
        val current = _uiState.value.selectedRecord
        if (current?.id == record.id && current.analysisResult == record.analysisResult && current.status == record.status) {
            // Already selected and matches. Avoid redundant parse and state emission.
            return
        }

        if (record.status != AnalysisStatus.COMPLETED) {
            _uiState.update { it.copy(isParsingDetailedResult = true) }
            viewModelScope.launch(Dispatchers.IO) {
                val detail = analyzeTextUseCase.parseDetailedResult(record.originalText, record.analysisResult)
                _uiState.update { it.copy(
                    selectedRecord = record,
                    currentOriginalText = record.originalText,
                    analysisResult = record.analysisResult,
                    detailedResult = detail,
                    isParsingDetailedResult = false,
                    selectedRecordProgress = analyzeTextUseCase.progressFlow.value[record.id]
                ) }
            }
            return
        }

        _uiState.update { it.copy(isParsingDetailedResult = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val detail = analyzeTextUseCase.parseDetailedResult(record.originalText, record.analysisResult)
            _uiState.update { it.copy(
                selectedRecord = record,
                currentOriginalText = record.originalText,
                analysisResult = record.analysisResult,
                detailedResult = detail,
                isParsingDetailedResult = false,
                selectedRecordProgress = null
            ) }
        }
    }

    fun clearSelectedRecord() {
        _uiState.update { it.copy(
            selectedRecord = null,
            currentOriginalText = "",
            analysisResult = null,
            detailedResult = null,
            isParsingDetailedResult = false,
            selectedRecordProgress = null
        ) }
    }

    fun startNewAnalysisWithText(text: String) {
        _uiState.update { it.copy(
            selectedRecord = null,
            currentOriginalText = text,
            analysisResult = null,
            detailedResult = null,
            isParsingDetailedResult = false,
            selectedRecordProgress = null
        ) }
    }

    fun setCurrentOriginalText(text: String) {
        _uiState.update { it.copy(currentOriginalText = text) }
    }

    fun setUseOcr(value: Boolean) {
        settingsRepository.setUseOcr(value)
        _uiState.update { it.copy(useOcr = value) }
    }

    fun setActiveProvider(provider: String) {
        settingsRepository.setActiveProvider(provider)
        val models = settingsRepository.getModelsForProvider(provider)
        val selectedModel = settingsRepository.getActiveModel(provider)
        val activeModel = if (selectedModel.isNotBlank() && models.contains(selectedModel)) {
            selectedModel
        } else {
            val fallback = models.firstOrNull() ?: ""
            settingsRepository.setActiveModel(provider, fallback)
            fallback
        }

        _uiState.update { it.copy(
            activeProvider = provider,
            availableModels = models,
            activeModel = activeModel
        ) }
    }

    fun setActiveModel(model: String) {
        val provider = _uiState.value.activeProvider
        settingsRepository.setActiveModel(provider, model)
        _uiState.update { it.copy(activeModel = model) }
    }

    fun saveModelsForProvider(provider: String, models: List<String>) {
        settingsRepository.saveModelsForProvider(provider, models)
        _uiState.update { state ->
            val updatedProviderModels = state.providerModels.toMutableMap().apply {
                put(provider, models)
            }
            var nextAvailable = state.availableModels
            var nextActive = state.activeModel
            if (state.activeProvider == provider) {
                nextAvailable = models
                if (!models.contains(state.activeModel)) {
                    nextActive = models.firstOrNull() ?: ""
                    settingsRepository.setActiveModel(provider, nextActive)
                }
            }
            var nextBackup = state.backupModel
            if (state.backupProvider == provider) {
                if (!models.contains(state.backupModel)) {
                    nextBackup = models.firstOrNull() ?: ""
                    settingsRepository.setBackupModel(nextBackup)
                }
            }
            state.copy(
                providerModels = updatedProviderModels,
                availableModels = nextAvailable,
                activeModel = nextActive,
                backupModel = nextBackup
            )
        }
    }

    fun setBackupProvider(provider: String) {
        settingsRepository.setBackupProvider(provider)
        val models = settingsRepository.getModelsForProvider(provider)
        val selectedModel = settingsRepository.getBackupModel()
        val backupModel = if (selectedModel.isNotBlank() && models.contains(selectedModel)) {
            selectedModel
        } else {
            val fallback = models.firstOrNull() ?: ""
            settingsRepository.setBackupModel(fallback)
            fallback
        }

        _uiState.update { it.copy(
            backupProvider = provider,
            backupModel = backupModel
        ) }
    }

    fun setBackupModel(model: String) {
        settingsRepository.setBackupModel(model)
        _uiState.update { it.copy(backupModel = model) }
    }

    fun fetchModels(provider: String, baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true, fetchingProvider = provider) }
            try {
                val fetchedModels = llmRepository.fetchModels(provider, baseUrl, apiKey)
                saveModelsForProvider(provider, fetchedModels)
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is retrofit2.HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string() ?: ""
                        "HTTP ${e.code()}: ${e.message()}\n$errorBody"
                    }
                    else -> e.localizedMessage ?: "Unknown Error"
                }
                _uiEvent.emit(UiEvent.ShowError(errorMsg))
            } finally {
                _uiState.update { it.copy(isFetchingModels = false, fetchingProvider = null) }
            }
        }
    }

    fun analyzeText(
        text: String,
        imageUri: Uri?,
        provider: String,
        modelName: String,
        baseUrl: String,
        apiKey: String
    ) {
        viewModelScope.launch {
            try {
                val recordId = analyzeTextUseCase.execute(text, imageUri?.toString(), provider, modelName, baseUrl, apiKey)
                val record = historyRepository.getRecordById(recordId)
                if (record != null) {
                    _uiState.update { it.copy(selectedRecord = record) }
                }
                _uiEvent.emit(UiEvent.ShowError(context.getString(R.string.analysis_started_toast)))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: context.getString(R.string.error_analysis_failed_to_start)))
            }
        }
    }

    fun retryAnalysis(recordId: Int) {
        viewModelScope.launch {
            try {
                retryAnalysisUseCase.execute(recordId)
                val record = historyRepository.getRecordById(recordId)
                if (record != null) {
                    _uiState.update { it.copy(selectedRecord = record) }
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: context.getString(R.string.error_retry_failed)))
            }
        }
    }

    fun cancelAnalysis(recordId: Int) {
        analyzeTextUseCase.cancel(recordId)
        if (_uiState.value.selectedRecord?.id == recordId) {
            clearSelectedRecord()
        }
    }

    fun deleteRecord(record: AnalysisDomainRecord) {
        viewModelScope.launch {
            historyRepository.deleteRecord(record)
        }
    }

    fun exportRecord(record: AnalysisDomainRecord) {
        viewModelScope.launch {
            val content = buildRecordExportText(record)
            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            val filename = "analysis_${sdf.format(java.util.Date(record.timestamp))}.txt"
            _uiEvent.emit(UiEvent.ExportContent(content, filename))
        }
    }

    fun exportAllHistory(records: List<AnalysisDomainRecord>) {
        viewModelScope.launch {
            if (records.isEmpty()) {
                _uiEvent.emit(UiEvent.ShowError(context.getString(R.string.no_history_to_export)))
                return@launch
            }
            val sb = StringBuilder()
            sb.appendLine(context.getString(R.string.export_header_title))
            sb.appendLine(context.getString(R.string.export_header_time, java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())))
            sb.appendLine(context.getString(R.string.export_header_count, records.size))
            sb.appendLine("=".repeat(60))
            sb.appendLine()
            records.forEachIndexed { index, record ->
                sb.appendLine(buildRecordExportText(record, index + 1))
                sb.appendLine()
            }
            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            val filename = "analysis_history_${sdf.format(java.util.Date())}.txt"
            _uiEvent.emit(UiEvent.ExportContent(sb.toString(), filename))
        }
    }

    private fun buildRecordExportText(record: AnalysisDomainRecord, index: Int? = null): String {
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault())
        val sb = StringBuilder()
        if (index != null) {
            sb.appendLine(context.getString(R.string.export_record_number, index))
        }
        sb.appendLine(context.getString(R.string.export_record_time, sdf.format(java.util.Date(record.timestamp))))
        sb.appendLine(context.getString(R.string.export_record_model, record.modelUsed))
        val statusStr = when (record.status) {
            AnalysisStatus.PENDING -> context.getString(R.string.history_status_pending)
            AnalysisStatus.FAILED -> context.getString(R.string.history_status_error)
            else -> context.getString(R.string.completed)
        }
        sb.appendLine(context.getString(R.string.export_record_status, statusStr))
        sb.appendLine("-".repeat(40))
        sb.appendLine(context.getString(R.string.export_original_text_section))
        sb.appendLine(record.originalText.ifBlank { context.getString(R.string.export_image_analysis_fallback) })
        if (record.status == AnalysisStatus.COMPLETED && !record.analysisResult.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine(context.getString(R.string.export_result_section))
            sb.appendLine(record.analysisResult)
        } else if (record.status == AnalysisStatus.FAILED) {
            sb.appendLine()
            sb.appendLine(context.getString(R.string.export_error_section))
            sb.appendLine(record.errorMessage ?: context.getString(R.string.unknown_error))
        }
        sb.appendLine("-".repeat(40))
        return sb.toString()
    }

    fun getApiKey(provider: String): String {
        return settingsRepository.getApiKey(provider)
    }

    fun getApiUrl(provider: String): String {
        return settingsRepository.getApiUrl(provider)
    }

    fun saveApiKey(provider: String, key: String) {
        settingsRepository.saveApiKey(provider, key)
    }

    fun saveApiUrl(provider: String, url: String) {
        settingsRepository.saveApiUrl(provider, url)
    }

    suspend fun extractTextFromImage(uri: Uri): String {
        return ocrRepository.extractTextFromImage(uri)
    }

    fun setThemeMode(mode: String) {
        settingsRepository.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setPrimaryColor(colorHex: String) {
        settingsRepository.setPrimaryColor(colorHex)
        _uiState.update { it.copy(primaryColor = colorHex) }
    }

    fun setWallpaperUri(uri: String) {
        settingsRepository.setWallpaperUri(uri)
        _uiState.update { it.copy(wallpaperUri = uri) }
    }

    // TTS Settings Accessors
    fun getTtsProvider(): String = settingsRepository.getTtsProvider()
    fun setTtsProvider(provider: String) {
        settingsRepository.setTtsProvider(provider)
        // Update uiState if needed, but since it's just settings, it might be fine without.
    }
    fun getTtsApiUrl(provider: String): String = settingsRepository.getTtsApiUrl(provider)
    fun setTtsApiUrl(provider: String, url: String) = settingsRepository.setTtsApiUrl(provider, url)
    fun getTtsApiKey(provider: String): String = settingsRepository.getTtsApiKey(provider)
    fun setTtsApiKey(provider: String, key: String) = settingsRepository.setTtsApiKey(provider, key)
    fun getTtsModel(provider: String): String = settingsRepository.getTtsModel(provider)
    fun setTtsModel(provider: String, model: String) = settingsRepository.setTtsModel(provider, model)
    fun getTtsVoice(provider: String): String = settingsRepository.getTtsVoice(provider)
    fun setTtsVoice(provider: String, voice: String) = settingsRepository.setTtsVoice(provider, voice)
    fun getTtsRegion(provider: String): String = settingsRepository.getTtsRegion(provider)
    fun setTtsRegion(provider: String, region: String) = settingsRepository.setTtsRegion(provider, region)

    fun playTtsForCurrentRecord() {
        val detail = _uiState.value.detailedResult
        if (detail != null) {
            val segments = detail.segments
            if (!segments.isNullOrEmpty()) {
                // Concatenate 'reading' for accurate pronunciation. 
                // If reading is null or blank, fallback to text.
                val readingText = segments.joinToString("") { segment: WordSegment ->
                    val reading = segment.reading
                    if (!reading.isNullOrBlank()) reading else (segment.text ?: "")
                }
                if (readingText.isNotBlank()) {
                    ttsRepository.playText(readingText)
                    return
                }
            }
        }
        // Fallback to original text if detailed parse is not available
        val text = _uiState.value.currentOriginalText
        if (text.isNotBlank()) {
            ttsRepository.playText(text)
        }
    }

    fun stopTts() {
        ttsRepository.stop()
    }

    override fun onCleared() {
        ttsRepository.stop()
        super.onCleared()
    }
}
