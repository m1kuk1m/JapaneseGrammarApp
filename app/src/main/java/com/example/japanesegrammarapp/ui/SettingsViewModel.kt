package com.example.japanesegrammarapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.domain.repository.LlmRepository
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import com.example.japanesegrammarapp.domain.model.ModelTokenUsage
import com.example.japanesegrammarapp.domain.model.LlmConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val llmRepository: LlmRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val totalTokensConsumed: StateFlow<Int> = historyRepository.totalTokensConsumed
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val tokenUsageByModel: StateFlow<List<ModelTokenUsage>> = historyRepository.tokenUsageByModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 10)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProvider = settingsRepository.getActiveProvider()
            val providerModels = LlmConfig.providers.associateWith { settingsRepository.getModelsForProvider(it) }
            val activeModel = settingsRepository.getActiveModel(activeProvider)
            val useOcr = settingsRepository.getUseOcr()
            val imageTokenizerMode = settingsRepository.getImageTokenizerMode()
            val backupProvider = settingsRepository.getBackupProvider()
            val backupModel = settingsRepository.getBackupModel()

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
                    imageTokenizerMode = imageTokenizerMode,
                    providerModels = providerModels,
                    availableModels = models,
                    backupProvider = backupProvider,
                    backupModel = backupModel
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }

        viewModelScope.launch {
            settingsRepository.wallpaperUri.collect { uri ->
                _uiState.update { it.copy(wallpaperUri = uri) }
            }
        }
    }

    fun setUseOcr(value: Boolean) {
        settingsRepository.setUseOcr(value)
        _uiState.update { it.copy(useOcr = value) }
    }

    fun setImageTokenizerMode(mode: String) {
        settingsRepository.setImageTokenizerMode(mode)
        _uiState.update { it.copy(imageTokenizerMode = mode) }
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
                _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: "Unknown Error"))
            } finally {
                _uiState.update { it.copy(isFetchingModels = false, fetchingProvider = null) }
            }
        }
    }

    fun getApiKey(provider: String): String = settingsRepository.getApiKey(provider)
    fun getApiUrl(provider: String): String = settingsRepository.getApiUrl(provider)

    fun saveApiKey(provider: String, key: String) = settingsRepository.saveApiKey(provider, key)
    fun saveApiUrl(provider: String, url: String) = settingsRepository.saveApiUrl(provider, url)

    fun setThemeMode(mode: String) {
        settingsRepository.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setWallpaperUri(uri: String) {
        settingsRepository.setWallpaperUri(uri)
        _uiState.update { it.copy(wallpaperUri = uri) }
    }

    // TTS Settings Accessors
    fun getTtsProvider(): String = settingsRepository.getTtsProvider()
    fun setTtsProvider(provider: String) = settingsRepository.setTtsProvider(provider)
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
}
