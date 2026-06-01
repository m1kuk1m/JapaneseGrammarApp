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
import androidx.paging.cachedIn
import javax.inject.Inject

import androidx.paging.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
    data class ShowLocalizedError(val resId: Int, val args: List<Any> = emptyList()) : UiEvent()
    object NavigateToResult : UiEvent()
    data class TaskCompleted(val recordId: Int, val message: String) : UiEvent()
    data class ExportRecordEvent(val record: com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord, val filename: String) : UiEvent()
    data class ExportAllHistoryEvent(val records: List<com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord>, val filename: String) : UiEvent()
}

data class HistoryUiRecord(
    val record: AnalysisDomainRecord,
    val dateStr: String,
    val modelText: String,
    val formattedTokens: String?
)

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

    private val historyDateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault())

    val history = historyRepository.history
        .map { pagingData ->
            pagingData.map { record ->
                val dateStr = historyDateFormatter.format(Instant.ofEpochMilli(record.timestamp))
                val modelText = record.modelUsed.substringAfter(": ").take(12)
                val tokens = if (record.status == AnalysisStatus.COMPLETED && record.consumedTokens > 0) {
                    if (record.consumedTokens >= 1000) String.format(java.util.Locale.US, "%.1fk", record.consumedTokens / 1000.0) else record.consumedTokens.toString()
                } else null
                HistoryUiRecord(record, dateStr, modelText, tokens)
            }
        }
        .cachedIn(viewModelScope)

    val totalTokensConsumed: StateFlow<Int> = historyRepository.totalTokensConsumed
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val tokenUsageByModel: StateFlow<List<ModelTokenUsage>> = historyRepository.tokenUsageByModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTokenUsage: StateFlow<List<DailyTokenUsage>> = historyRepository.dailyTokenUsage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPlayingTts: StateFlow<Boolean> = ttsRepository.isPlaying

    private val _allHistoryForExport = MutableStateFlow<List<AnalysisDomainRecord>>(emptyList())
    val allHistoryForExport: StateFlow<List<AnalysisDomainRecord>> = _allHistoryForExport.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 10)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProvider = settingsRepository.getActiveProvider()
            val providerModels = LlmConfig.providers.associateWith { settingsRepository.getModelsForProvider(it) }
            val activeModel = settingsRepository.getActiveModel(activeProvider)
            val useOcr = settingsRepository.getUseOcr()
            val imageTokenizerMode = settingsRepository.getImageTokenizerMode()

            val models = providerModels[activeProvider] ?: emptyList()
            val finalActiveModel = if (activeModel.isBlank() && models.isNotEmpty()) models.first() else activeModel

            _uiState.update {
                it.copy(
                    activeProvider = activeProvider,
                    activeModel = finalActiveModel,
                    useOcr = useOcr,
                    imageTokenizerMode = imageTokenizerMode,
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
                        } else {
                            refreshSelectedRecordFromRepository(event.recordId, clearProgress = true)
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
            analyzeTextUseCase.progressFlow.collect { progressMap ->
                val currentSelected = _uiState.value.selectedRecord
                if (currentSelected != null) {
                    val progress = progressMap[currentSelected.id]
                    _uiState.update { it.copy(selectedRecordProgress = progress) }
                    if (progress == null || progress.grammarCompleted || progress.translationCompleted || progress.clausesCompleted || progress.segmentsCompleted) {
                        refreshSelectedRecordFromRepository(currentSelected.id, clearProgress = progress == null)
                    }
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

    fun selectRecordById(recordId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = historyRepository.getRecordById(recordId)
            if (record != null) {
                selectRecord(record)
            }
        }
    }

    private suspend fun refreshSelectedRecordFromRepository(recordId: Int, clearProgress: Boolean = false) {
        val updated = withContext(Dispatchers.IO) { historyRepository.getRecordById(recordId) } ?: return
        val detail = withContext(Dispatchers.IO) {
            analyzeTextUseCase.parseDetailedResult(updated.originalText, updated.analysisResult)
        }
        _uiState.update { state ->
            if (state.selectedRecord?.id != recordId) {
                state
            } else {
                state.copy(
                    selectedRecord = updated,
                    currentOriginalText = updated.originalText,
                    analysisResult = updated.analysisResult,
                    detailedResult = detail,
                    isParsingDetailedResult = false,
                    selectedRecordProgress = if (clearProgress || updated.status == AnalysisStatus.COMPLETED) {
                        null
                    } else {
                        analyzeTextUseCase.progressFlow.value[recordId]
                    }
                )
            }
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
            val filename = "history_export_${System.currentTimeMillis()}.txt"
            _uiEvent.emit(UiEvent.ExportAllHistoryEvent(records, filename))
        }
    }

    fun loadAllHistoryForExport() {
        viewModelScope.launch(Dispatchers.IO) {
            _allHistoryForExport.value = historyRepository.getAllRecordsList()
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
