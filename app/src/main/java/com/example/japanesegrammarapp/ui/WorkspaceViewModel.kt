package com.example.japanesegrammarapp.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.*
import com.example.japanesegrammarapp.domain.repository.*
import com.example.japanesegrammarapp.domain.usecase.AnalyzeTextUseCase
import com.example.japanesegrammarapp.domain.usecase.RetryAnalysisUseCase
import com.example.japanesegrammarapp.domain.usecase.AnalysisEventBus
import com.example.japanesegrammarapp.domain.model.LlmConfig
import com.example.japanesegrammarapp.utils.RecordExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
    data class ShowLocalizedError(val resId: Int, val args: List<Any> = emptyList()) : UiEvent()
    object NavigateToResult : UiEvent()
    data class TaskCompleted(val recordId: Int, val message: String) : UiEvent()
    data class ExportRecordEvent(val record: com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord, val filename: String) : UiEvent()
    data class ExportAllHistoryEvent(val records: List<com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord>, val filename: String) : UiEvent()
}

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val ocrRepository: OcrRepository,
    private val ttsRepository: TtsRepository,
    private val analyzeTextUseCase: AnalyzeTextUseCase,
    private val retryAnalysisUseCase: RetryAnalysisUseCase,
    private val eventBus: AnalysisEventBus
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
        viewModelScope.launch(Dispatchers.IO) {
            val activeProvider = settingsRepository.getActiveProvider()
            val providerModels = LlmConfig.providers.associateWith { settingsRepository.getModelsForProvider(it) }
            val activeModel = settingsRepository.getActiveModel(activeProvider)
            val useOcr = settingsRepository.getUseOcr()

            val models = providerModels[activeProvider] ?: emptyList()
            val finalActiveModel = if (activeModel.isBlank() && models.isNotEmpty()) models.first() else activeModel

            _uiState.update {
                it.copy(
                    activeProvider = activeProvider,
                    activeModel = finalActiveModel,
                    useOcr = useOcr,
                    providerModels = providerModels,
                    availableModels = models
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.wallpaperUri.collect { uri ->
                _uiState.update { it.copy(wallpaperUri = uri) }
            }
        }

        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is AnalysisEvent.TaskCompleted -> {
                        val currentSelected = _uiState.value.selectedRecord
                        if (currentSelected == null || currentSelected.id != event.recordId) {
                            _uiEvent.emit(UiEvent.TaskCompleted(event.recordId, event.message))
                        }
                    }
                    is AnalysisEvent.OcrFallbackTriggered -> {
                        _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.ocr_fallback_toast))
                    }
                    is AnalysisEvent.SpellingCorrectedTriggered -> {
                        _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.spelling_corrected_toast))
                    }
                    is AnalysisEvent.LlmRetryTriggered -> {
                        _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.llm_retry_toast, listOf(event.apiTypeLabel, event.attempt)))
                    }
                    is AnalysisEvent.LlmBackupTriggered -> {
                        _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.llm_backup_toast, listOf(event.apiTypeLabel, event.backupProvider)))
                    }
                }
            }
        }

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
                selectedRecordProgress = if (record.status != AnalysisStatus.COMPLETED) {
                    analyzeTextUseCase.progressFlow.value[record.id]
                } else null
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

    fun setActiveModel(model: String) {
        val provider = _uiState.value.activeProvider
        settingsRepository.setActiveModel(provider, model)
        _uiState.update { it.copy(activeModel = model) }
    }

    fun startAnalysis(text: String, imageUri: Uri?) {
        viewModelScope.launch {
            val provider = _uiState.value.activeProvider
            val model = _uiState.value.activeModel.ifBlank { "default" }
            val key = getApiKey(provider)
            val url = getApiUrl(provider)
            analyzeText(text, imageUri, provider, model, url, key)
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
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.analysis_started_toast))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: "Analysis failed to start"))
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
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.error_retry_failed))
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
            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            val filename = "analysis_${sdf.format(java.util.Date(record.timestamp))}.txt"
            _uiEvent.emit(UiEvent.ExportRecordEvent(record, filename))
        }
    }

    fun exportAllHistory(records: List<AnalysisDomainRecord>) {
        viewModelScope.launch {
            if (records.isEmpty()) {
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.no_history_to_export))
                return@launch
            }
            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            val filename = "analysis_history_${sdf.format(java.util.Date())}.txt"
            _uiEvent.emit(UiEvent.ExportAllHistoryEvent(records, filename))
        }
    }

    fun getApiKey(provider: String): String = settingsRepository.getApiKey(provider)
    fun getApiUrl(provider: String): String = settingsRepository.getApiUrl(provider)

    suspend fun extractTextFromImage(uri: Uri): String {
        return ocrRepository.extractTextFromImage(uri.toString())
    }

    fun playTtsForCurrentRecord() {
        val detail = _uiState.value.detailedResult
        if (detail != null) {
            val segments = detail.segments
            if (!segments.isNullOrEmpty()) {
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
