package com.example.japanesegrammarapp.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesegrammarapp.data.AnalysisEvent
import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.data.repository.HistoryRepository
import com.example.japanesegrammarapp.data.repository.LlmRepository
import com.example.japanesegrammarapp.data.repository.OcrRepository
import com.example.japanesegrammarapp.data.repository.SettingsRepository
import com.example.japanesegrammarapp.domain.usecase.AnalyzeTextUseCase
import com.example.japanesegrammarapp.domain.usecase.RetryAnalysisUseCase
import com.example.japanesegrammarapp.network.LlmConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
    object NavigateToResult : UiEvent()
    data class TaskCompleted(val recordId: Int, val message: String) : UiEvent()
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val llmRepository: LlmRepository,
    private val ocrRepository: OcrRepository,
    private val analyzeTextUseCase: AnalyzeTextUseCase,
    private val retryAnalysisUseCase: RetryAnalysisUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        WorkspaceUiState(
            activeProvider = settingsRepository.getActiveProvider(),
            activeModel = settingsRepository.getActiveModel(settingsRepository.getActiveProvider()),
            useOcr = settingsRepository.getUseOcr(),
            providerModels = LlmConfig.providers.associateWith { settingsRepository.getModelsForProvider(it) }
        )
    )
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    val history = historyRepository.history

    // Guard flag to suppress history auto-sync overwriting a user navigation selection
    @Volatile private var _isUserNavigating = false

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 10)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        // Align active model with available models initially
        val provider = _uiState.value.activeProvider
        val models = _uiState.value.providerModels[provider] ?: emptyList()
        _uiState.update { 
            it.copy(
                availableModels = models,
                activeModel = if (it.activeModel.isBlank() && models.isNotEmpty()) models.first() else it.activeModel
            )
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
                }
            }
        }

        // Keep selected record and detailed parses synced with DB updates
        viewModelScope.launch {
            history.collect { recordList ->
                if (_isUserNavigating) return@collect
                val currentSelected = _uiState.value.selectedRecord
                if (currentSelected != null) {
                    val updated = recordList.find { it.id == currentSelected.id }
                    if (updated != null && (updated.status != currentSelected.status || 
                        updated.analysisResult != currentSelected.analysisResult || 
                        updated.originalText != currentSelected.originalText)) {
                        
                        if (updated.status == "COMPLETED") {
                            viewModelScope.launch(Dispatchers.IO) {
                                val detail = analyzeTextUseCase.parseDetailedResult(updated.originalText, updated.analysisResult)
                                _uiState.update { it.copy(
                                    selectedRecord = updated,
                                    currentOriginalText = updated.originalText,
                                    analysisResult = updated.analysisResult,
                                    detailedResult = detail,
                                    isParsingDetailedResult = false
                                ) }
                            }
                        } else {
                            _uiState.update { it.copy(
                                selectedRecord = updated,
                                currentOriginalText = updated.originalText,
                                analysisResult = updated.analysisResult,
                                detailedResult = null,
                                isParsingDetailedResult = (updated.status == "PENDING")
                            ) }
                        }
                    }
                }
            }
        }
    }

    fun selectRecord(record: AnalysisRecord) {
        _isUserNavigating = true
        
        if (record.status != "COMPLETED") {
            _uiState.update { it.copy(
                selectedRecord = record,
                currentOriginalText = record.originalText,
                analysisResult = record.analysisResult,
                detailedResult = null,
                isParsingDetailedResult = false
            ) }
            _isUserNavigating = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val detail = analyzeTextUseCase.parseDetailedResult(record.originalText, record.analysisResult)
            _uiState.update { it.copy(
                selectedRecord = record,
                currentOriginalText = record.originalText,
                analysisResult = record.analysisResult,
                detailedResult = detail,
                isParsingDetailedResult = false
            ) }
            delay(500L)
            _isUserNavigating = false
        }
    }

    fun clearSelectedRecord() {
        _uiState.update { it.copy(
            selectedRecord = null,
            currentOriginalText = "",
            analysisResult = null,
            detailedResult = null,
            isParsingDetailedResult = false
        ) }
    }

    fun startNewAnalysisWithText(text: String) {
        _uiState.update { it.copy(
            selectedRecord = null,
            currentOriginalText = text,
            analysisResult = null,
            detailedResult = null,
            isParsingDetailedResult = false
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
            state.copy(
                providerModels = updatedProviderModels,
                availableModels = nextAvailable,
                activeModel = nextActive
            )
        }
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
                val recordId = analyzeTextUseCase.execute(text, imageUri, provider, modelName, baseUrl, apiKey)
                val record = historyRepository.getRecordById(recordId)
                if (record != null) {
                    _uiState.update { it.copy(selectedRecord = record) }
                }
                _uiEvent.emit(UiEvent.ShowError("分析タスクを開始しました。"))
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
                _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: "Retry failed"))
            }
        }
    }

    fun cancelAnalysis(recordId: Int) {
        analyzeTextUseCase.cancel(recordId)
        if (_uiState.value.selectedRecord?.id == recordId) {
            clearSelectedRecord()
        }
    }

    fun deleteRecord(record: AnalysisRecord) {
        viewModelScope.launch {
            historyRepository.deleteRecord(record)
        }
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

    override fun onCleared() {
        super.onCleared()
        analyzeTextUseCase.close()
        ocrRepository.close()
    }
}
