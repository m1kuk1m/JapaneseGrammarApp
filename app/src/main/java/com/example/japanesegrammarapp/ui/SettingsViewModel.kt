package com.example.japanesegrammarapp.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.domain.repository.LlmRepository
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import com.example.japanesegrammarapp.domain.model.ModelTokenUsage
import com.example.japanesegrammarapp.domain.model.DailyTokenUsage
import com.example.japanesegrammarapp.domain.model.EndpointUrlValidator
import com.example.japanesegrammarapp.domain.model.LlmConfig
import com.example.japanesegrammarapp.domain.model.LlmEndpoint
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectionSettings
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectorEngine
import com.example.japanesegrammarapp.domain.model.PromptPreset
import com.example.japanesegrammarapp.domain.model.ReasoningLevel
import com.example.japanesegrammarapp.domain.model.ComponentReasoningLevel
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.screens.moduleDisplayName
import com.example.japanesegrammarapp.utils.ApiDebugLog
import com.example.japanesegrammarapp.utils.ApiLogExportFormatter
import com.example.japanesegrammarapp.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
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

    val dailyTokenUsage: StateFlow<List<DailyTokenUsage>> = historyRepository.dailyTokenUsage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 10)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val allProviders = settingsRepository.getAllProviders()
            val activeProvider = settingsRepository.getActiveProvider()
            val providerModels = allProviders.associateWith { settingsRepository.getModelsForProvider(it) }
            val activeModel = settingsRepository.getActiveModel(activeProvider)
            val useOcr = settingsRepository.getUseOcr()
            val reasoningLevel = settingsRepository.getReasoningLevel()
            val autoNavigateResult = settingsRepository.getAutoNavigateResult()
            val removeAccidentalSpaces = settingsRepository.getRemoveAccidentalSpaces()
            val autoDeskewAfterCapture = settingsRepository.getAutoDeskewAfterCapture()
            val imageTokenizerMode = settingsRepository.getImageTokenizerMode()
            val ocrBoxDetectionSettings = settingsRepository.getOcrBoxDetectionSettings()
            val backupProvider = settingsRepository.getBackupProvider()
            val backupModel = settingsRepository.getBackupModel()
            val useBackupApi = settingsRepository.getUseBackupApi()
            val autoRetryOnError = settingsRepository.getAutoRetryOnError()
            val failoverToNextEndpoint = settingsRepository.getFailoverToNextEndpoint()
            val componentLabels = com.example.japanesegrammarapp.domain.model.AnalysisModule.entries.map { it.id }
            val componentReasoningLevels = componentLabels.associateWith {
                settingsRepository.getComponentReasoningLevel(it)
            }
            val componentModelConfigs = componentLabels.associateWith {
                settingsRepository.getComponentModelConfig(it)
            }

            val models = providerModels[activeProvider] ?: emptyList()
            val finalActiveModel = if (activeModel.isBlank() && models.isNotEmpty()) models.first() else activeModel

            // Load TTS settings
            val ttsProvider = settingsRepository.getTtsProvider()
            val ttsProvidersList = listOf("OpenAI", "Google", "Microsoft")
            val tUrls = ttsProvidersList.associateWith { settingsRepository.getTtsApiUrl(it) }
            val tKeys = ttsProvidersList.associateWith { settingsRepository.getTtsApiKey(it) }
            val tModels = ttsProvidersList.associateWith { settingsRepository.getTtsModel(it) }
            val tVoices = ttsProvidersList.associateWith { settingsRepository.getTtsVoice(it) }
            val tRegions = ttsProvidersList.associateWith { settingsRepository.getTtsRegion(it) }

            val pUrls = allProviders.associateWith { settingsRepository.getApiUrl(it) }
            val pKeys = allProviders.associateWith { settingsRepository.getApiKey(it) }
            val pEndpoints = allProviders.associateWith { settingsRepository.getEndpoints(it) }
            val endpointHasKeys = pEndpoints.values
                .flatten()
                .associate { endpoint -> endpoint.id to settingsRepository.getApiKeyForEndpoint(endpoint.id).isNotBlank() }

            val promptPresets = settingsRepository.getPromptPresets()
            val activePromptPresetId = settingsRepository.getActivePromptPresetId()

            _uiState.update {
                it.copy(
                    reasoningLevel = reasoningLevel,
                    componentReasoningLevels = componentReasoningLevels,
                    componentModelConfigs = componentModelConfigs,
                    activeProvider = activeProvider,
                    activeModel = finalActiveModel,
                    useOcr = useOcr,
                    autoNavigateResult = autoNavigateResult,
                    removeAccidentalSpaces = removeAccidentalSpaces,
                    useBackupApi = useBackupApi,
                    autoRetryOnError = autoRetryOnError,
                    failoverToNextEndpoint = failoverToNextEndpoint,
                    autoDeskewAfterCapture = autoDeskewAfterCapture,

                    imageTokenizerMode = imageTokenizerMode,
                    ocrBoxDetectionSettings = ocrBoxDetectionSettings,
                    providerModels = providerModels,
                    availableModels = models,
                    backupProvider = backupProvider,
                    backupModel = backupModel,
                    allProviders = allProviders,
                    selectedTtsProvider = ttsProvider,
                    ttsUrls = tUrls,
                    ttsKeys = tKeys,
                    ttsModels = tModels,
                    ttsVoices = tVoices,
                    ttsRegions = tRegions,
                    providerUrls = pUrls,
                    providerKeys = pKeys,
                    providerEndpoints = pEndpoints,
                    endpointHasKeys = endpointHasKeys,
                    promptPresets = promptPresets,
                    activePromptPresetId = activePromptPresetId,
                    isSettingsLoaded = true
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

        viewModelScope.launch {
            settingsRepository.cardFontSizeScale.collect { scale ->
                _uiState.update { it.copy(cardFontSizeScale = scale) }
            }
        }

        viewModelScope.launch {
            settingsRepository.cardSpacingScale.collect { scale ->
                _uiState.update { it.copy(cardSpacingScale = scale) }
            }
        }

        viewModelScope.launch {
            settingsRepository.furiganaSizeScale.collect { scale ->
                _uiState.update { it.copy(furiganaSizeScale = scale) }
            }
        }

        viewModelScope.launch {
            settingsRepository.cardInternalPaddingScale.collect { scale ->
                _uiState.update { it.copy(cardInternalPaddingScale = scale) }
            }
        }

        viewModelScope.launch {
            settingsRepository.furiganaGapScale.collect { scale ->
                _uiState.update { it.copy(furiganaGapScale = scale) }
            }
        }

        viewModelScope.launch {
            settingsRepository.cardDetailDisplayMode.collect { mode ->
                _uiState.update { it.copy(cardDetailDisplayMode = mode) }
            }
        }
    }

    fun getApiKey(provider: String): String = settingsRepository.getApiKey(provider)
    fun saveApiKey(provider: String, key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = settingsRepository.saveApiKey(provider, key)
            if (ok) {
                val endpoints = settingsRepository.getEndpoints(provider)
                _uiState.update { state ->
                    state.copy(
                        providerKeys = state.providerKeys.toMutableMap().apply { put(provider, key) },
                        providerEndpoints = state.providerEndpoints.toMutableMap().apply { put(provider, endpoints) }
                    )
                }
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.api_key_save_success))
            } else {
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.api_key_save_failed))
            }
        }
    }
    fun getApiUrl(provider: String): String = settingsRepository.getApiUrl(provider)
    fun setApiUrl(provider: String, url: String) {
        settingsRepository.saveApiUrl(provider, url)
        val endpoints = settingsRepository.getEndpoints(provider)
        _uiState.update { state ->
            state.copy(
                providerUrls = state.providerUrls.toMutableMap().apply { put(provider, url) },
                providerEndpoints = state.providerEndpoints.toMutableMap().apply { put(provider, endpoints) }
            )
        }
    }

    fun getApiKeyForEndpoint(endpointId: String): String = settingsRepository.getApiKeyForEndpoint(endpointId)

    fun createEndpoint(
        provider: String,
        name: String,
        baseUrl: String,
        apiKey: String,
        priority: Int,
        weight: Int
    ) {
        val endpoint = LlmEndpoint(
            id = "endpoint_${UUID.randomUUID()}",
            provider = provider,
            name = name.ifBlank { "Endpoint ${(uiState.value.providerEndpoints[provider]?.size ?: 0) + 1}" },
            baseUrl = baseUrl.ifBlank { LlmConfig.defaultUrls[provider] ?: "" },
            enabled = apiKey.isNotBlank(),
            priority = priority.coerceAtLeast(0),
            weight = weight.coerceAtLeast(1)
        )
        saveEndpoint(endpoint, apiKey)
    }

    fun saveEndpoint(endpoint: LlmEndpoint, apiKey: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = settingsRepository.saveEndpoint(endpoint, apiKey)
            if (ok) {
                refreshEndpointPool(endpoint.provider)
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.endpoint_save_success))
            } else {
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.api_key_save_failed))
            }
        }
    }

    fun toggleEndpoint(provider: String, endpointId: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val endpoint = settingsRepository.getEndpoints(provider).firstOrNull { it.id == endpointId } ?: return@launch
            if (enabled && settingsRepository.getApiKeyForEndpoint(endpointId).isBlank()) {
                refreshEndpointPool(provider)
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.endpoint_missing_key_hint))
                return@launch
            }
            val ok = settingsRepository.saveEndpoint(endpoint.copy(enabled = enabled), null)
            if (ok) {
                refreshEndpointPool(provider)
            } else {
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.api_key_save_failed))
            }
        }
    }

    fun deleteEndpoint(provider: String, endpointId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (settingsRepository.deleteEndpoint(provider, endpointId)) {
                refreshEndpointPool(provider)
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.endpoint_delete_success))
            }
        }
    }

    private fun refreshEndpointPool(provider: String) {
        val endpoints = settingsRepository.getEndpoints(provider)
        val url = settingsRepository.getApiUrl(provider)
        val key = settingsRepository.getApiKey(provider)
        val endpointHasKeys = endpoints.associate { endpoint ->
            endpoint.id to settingsRepository.getApiKeyForEndpoint(endpoint.id).isNotBlank()
        }
        _uiState.update { state ->
            state.copy(
                providerEndpoints = state.providerEndpoints.toMutableMap().apply { put(provider, endpoints) },
                providerUrls = state.providerUrls.toMutableMap().apply { put(provider, url) },
                providerKeys = state.providerKeys.toMutableMap().apply { put(provider, key) },
                endpointHasKeys = state.endpointHasKeys.toMutableMap().apply { putAll(endpointHasKeys) }
            )
        }
    }

    private fun refreshProviders() {
        viewModelScope.launch(Dispatchers.IO) {
            val allProviders = settingsRepository.getAllProviders()
            val providerModels = allProviders.associateWith { settingsRepository.getModelsForProvider(it) }
            val providerEndpoints = allProviders.associateWith { settingsRepository.getEndpoints(it) }
            val endpointHasKeys = providerEndpoints.values
                .flatten()
                .associate { endpoint -> endpoint.id to settingsRepository.getApiKeyForEndpoint(endpoint.id).isNotBlank() }

            _uiState.update {
                it.copy(
                    allProviders = allProviders,
                    providerModels = providerModels,
                    providerEndpoints = providerEndpoints,
                    endpointHasKeys = endpointHasKeys
                )
            }
        }
    }

    fun setUseOcr(value: Boolean) {
        settingsRepository.setUseOcr(value)
        _uiState.update { it.copy(useOcr = value) }
    }

    fun setAutoNavigateResult(value: Boolean) {
        settingsRepository.setAutoNavigateResult(value)
        _uiState.update { it.copy(autoNavigateResult = value) }
    }

    fun setRemoveAccidentalSpaces(value: Boolean) {
        settingsRepository.setRemoveAccidentalSpaces(value)
        _uiState.update { it.copy(removeAccidentalSpaces = value) }
    }


    fun setAutoDeskewAfterCapture(value: Boolean) {
        settingsRepository.setAutoDeskewAfterCapture(value)
        _uiState.update { it.copy(autoDeskewAfterCapture = value) }
    }

    fun setImageTokenizerMode(mode: String) {
        settingsRepository.setImageTokenizerMode(mode)
        _uiState.update { it.copy(imageTokenizerMode = mode) }
    }

    fun setOcrBoxDetectionSettings(settings: OcrBoxDetectionSettings) {
        val normalized = settings.normalized()
        settingsRepository.setOcrBoxDetectionSettings(normalized)
        _uiState.update { it.copy(ocrBoxDetectionSettings = normalized) }
    }

    fun setOcrBoxDetectorEngine(engine: OcrBoxDetectorEngine) {
        val normalized = _uiState.value.ocrBoxDetectionSettings.copy(detectorEngine = engine).normalized()
        settingsRepository.setOcrBoxDetectionSettings(normalized)
        _uiState.update { it.copy(ocrBoxDetectionSettings = normalized) }
    }

    fun setTextSelectEngine(engine: OcrBoxDetectorEngine) {
        val normalized = _uiState.value.ocrBoxDetectionSettings.copy(textSelectEngine = engine).normalized()
        settingsRepository.setOcrBoxDetectionSettings(normalized)
        _uiState.update { it.copy(ocrBoxDetectionSettings = normalized) }
    }

    fun resetOcrBoxDetectionSettings() {
        settingsRepository.resetOcrBoxDetectionSettings()
        _uiState.update { it.copy(ocrBoxDetectionSettings = OcrBoxDetectionSettings.DEFAULT) }
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
                settingsRepository.saveApiUrl(provider, baseUrl)
                saveModelsForProvider(provider, fetchedModels)
                if (fetchedModels.isNotEmpty()) {
                    _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.fetch_models_success, listOf(fetchedModels.size)))
                } else {
                    _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.fetch_models_empty))
                }
            } catch (e: IllegalArgumentException) {
                if (e.message == "Please configure API Key in Settings first.") {
                    _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.err_missing_api_key))
                } else {
                    _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: "Unknown Error"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: "Unknown Error"))
            } finally {
                _uiState.update { it.copy(isFetchingModels = false, fetchingProvider = null) }
            }
        }
    }

    fun fetchModelsForProvider(provider: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true, fetchingProvider = provider) }
            try {
                val endpoints = settingsRepository.getEndpoints(provider)
                val activeEndpoint = endpoints
                    .filter { it.enabled && settingsRepository.getApiKeyForEndpoint(it.id).isNotBlank() && EndpointUrlValidator.isValidHttpUrl(it.baseUrl) }
                    .maxByOrNull { it.priority }

                val (targetUrl, targetKey) = if (activeEndpoint != null) {
                    activeEndpoint.baseUrl to settingsRepository.getApiKeyForEndpoint(activeEndpoint.id)
                } else {
                    val key = settingsRepository.getApiKey(provider)
                    val url = settingsRepository.getApiUrl(provider).ifBlank { LlmConfig.defaultUrls[provider] ?: "" }
                    if (key.isBlank()) {
                        throw IllegalArgumentException(application.getString(R.string.fetch_models_no_endpoint))
                    }
                    url to key
                }

                val fetchedModels = llmRepository.fetchModels(provider, targetUrl, targetKey)
                saveModelsForProvider(provider, fetchedModels)
                if (fetchedModels.isNotEmpty()) {
                    _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.fetch_models_success, listOf(fetchedModels.size)))
                } else {
                    _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.fetch_models_empty))
                }
            } catch (e: IllegalArgumentException) {
                if (e.message == "Please configure API Key in Settings first.") {
                    _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.err_missing_api_key))
                } else {
                    _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: "Unknown Error"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: "Unknown Error"))
            } finally {
                _uiState.update { it.copy(isFetchingModels = false, fetchingProvider = null) }
            }
        }
    }

    fun fetchModelsForEndpoint(provider: String, endpointId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFetchingModels = true,
                    fetchingProvider = provider,
                    fetchingEndpointId = endpointId
                )
            }
            try {
                val endpoint = settingsRepository.getEndpoints(provider).firstOrNull { it.id == endpointId }
                    ?: throw IllegalArgumentException("Endpoint not found")
                val apiKey = settingsRepository.getApiKeyForEndpoint(endpointId)
                if (apiKey.isBlank()) {
                    throw IllegalArgumentException(application.getString(R.string.endpoint_missing_key_hint))
                }
                if (!EndpointUrlValidator.isValidHttpUrl(endpoint.baseUrl)) {
                    throw IllegalArgumentException(application.getString(R.string.endpoint_invalid_url_hint))
                }
                val fetchedModels = llmRepository.fetchModels(provider, endpoint.baseUrl, apiKey)
                saveModelsForProvider(provider, fetchedModels)
                if (fetchedModels.isNotEmpty()) {
                    _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.fetch_models_success, listOf(fetchedModels.size)))
                } else {
                    _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.fetch_models_empty))
                }
            } catch (e: IllegalArgumentException) {
                if (e.message == "Please configure API Key in Settings first.") {
                    _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.err_missing_api_key))
                } else {
                    _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: "Unknown Error"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError(e.localizedMessage ?: "Unknown Error"))
            } finally {
                _uiState.update {
                    it.copy(
                        isFetchingModels = false,
                        fetchingProvider = null,
                        fetchingEndpointId = null
                    )
                }
            }
        }
    }

    fun setThemeMode(mode: String) {
        settingsRepository.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setWallpaperUri(uri: String) {
        settingsRepository.setWallpaperUri(uri)
        _uiState.update { it.copy(wallpaperUri = uri) }
    }

    fun saveWallpaper(sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(application.filesDir, WALLPAPER_FILE_NAME)
                application.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IllegalArgumentException("Unable to open wallpaper URI")

                setWallpaperUri(Uri.fromFile(file).toString())
            } catch (e: Exception) {
                AppLogger.e("SETTINGS", "Failed to save custom wallpaper", e)
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.unknown_error))
            }
        }
    }

    fun clearWallpaper() {
        setWallpaperUri("")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(application.filesDir, WALLPAPER_FILE_NAME)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                AppLogger.e("SETTINGS", "Failed to delete custom wallpaper", e)
            }
        }
    }

    fun shareAppLogs(logs: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(application.cacheDir, "exports/app_logs_export.txt")
                tempFile.parentFile?.mkdirs()
                tempFile.writeText(logs.joinToString("\n"))
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    application,
                    "${application.packageName}.fileprovider",
                    tempFile
                )
                
                _uiEvent.emit(
                    UiEvent.ShareFileEvent(
                        uri = uri,
                        mimeType = "text/plain",
                        chooserTitleResId = R.string.share_logs
                    )
                )
            } catch (e: Exception) {
                AppLogger.e("SETTINGS", "Failed to prepare app logs for sharing", e)
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.unknown_error))
            }
        }
    }

    fun shareApiLogs(logs: List<ApiDebugLog>, includeFull: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val formattedLogs = ApiLogExportFormatter.format(logs, includeFull) {
                    moduleDisplayName(application, it)
                }
                val tempFile = File(application.cacheDir, "exports/api_logs_export.txt")
                tempFile.parentFile?.mkdirs()
                tempFile.writeText(formattedLogs)

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    application,
                    "${application.packageName}.fileprovider",
                    tempFile
                )

                _uiEvent.emit(
                    UiEvent.ShareFileEvent(
                        uri = uri,
                        mimeType = "text/plain",
                        chooserTitleResId = R.string.api_log_share_all
                    )
                )
            } catch (e: Exception) {
                AppLogger.e("SETTINGS", "Failed to prepare API logs for sharing", e)
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.unknown_error))
            }
        }
    }

    // TTS Settings Accessors
    fun getTtsProvider(): String = settingsRepository.getTtsProvider()
    fun setTtsProvider(provider: String) = settingsRepository.setTtsProvider(provider)
    fun getTtsApiUrl(provider: String): String = settingsRepository.getTtsApiUrl(provider)
    fun setTtsApiUrl(provider: String, url: String) = settingsRepository.setTtsApiUrl(provider, url)
    fun getTtsApiKey(provider: String): String = settingsRepository.getTtsApiKey(provider)
    fun saveTtsApiKey(provider: String, key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = settingsRepository.setTtsApiKey(provider, key)
            if (ok) {
                _uiState.update { state ->
                    state.copy(ttsKeys = state.ttsKeys.toMutableMap().apply { put(provider, key) })
                }
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.api_key_save_success))
            } else {
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.api_key_save_failed))
            }
        }
    }
    fun saveTtsSettings(
        selectedProvider: String,
        urls: Map<String, String>,
        keyProvider: String,
        key: String,
        models: Map<String, String>,
        voices: Map<String, String>,
        regions: Map<String, String>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setTtsProvider(selectedProvider)
            urls.forEach { (provider, url) -> settingsRepository.setTtsApiUrl(provider, url) }
            models.forEach { (provider, model) -> settingsRepository.setTtsModel(provider, model) }
            voices.forEach { (provider, voice) -> settingsRepository.setTtsVoice(provider, voice) }
            regions.forEach { (provider, region) -> settingsRepository.setTtsRegion(provider, region) }

            val ok = settingsRepository.setTtsApiKey(keyProvider, key)
            if (ok) {
                _uiState.update { state ->
                    state.copy(
                        selectedTtsProvider = selectedProvider,
                        ttsUrls = urls.toMap(),
                        ttsKeys = state.ttsKeys.toMutableMap().apply { put(keyProvider, key) },
                        ttsModels = models.toMap(),
                        ttsVoices = voices.toMap(),
                        ttsRegions = regions.toMap()
                    )
                }
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.tts_settings_save_success))
            } else {
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.api_key_save_failed))
            }
        }
    }
    fun getTtsModel(provider: String): String = settingsRepository.getTtsModel(provider)
    fun setTtsModel(provider: String, model: String) = settingsRepository.setTtsModel(provider, model)
    fun getTtsVoice(provider: String): String = settingsRepository.getTtsVoice(provider)
    fun setTtsVoice(provider: String, voice: String) = settingsRepository.setTtsVoice(provider, voice)
    fun getTtsRegion(provider: String): String = settingsRepository.getTtsRegion(provider)
    fun setTtsRegion(provider: String, region: String) = settingsRepository.setTtsRegion(provider, region)

    // Prompt Settings Accessors
    fun getCustomPrompt(promptKey: String): String = settingsRepository.getCustomPrompt(promptKey)
    fun saveCustomPrompt(promptKey: String, prompt: String) = settingsRepository.saveCustomPrompt(promptKey, prompt)
    fun resetCustomPrompt(promptKey: String) = settingsRepository.resetCustomPrompt(promptKey)
    fun resetAllCustomPrompts() = settingsRepository.resetAllCustomPrompts()

    fun setActivePromptPreset(id: String) {
        settingsRepository.setActivePromptPresetId(id)
        _uiState.update { it.copy(activePromptPresetId = id) }
    }

    fun createPromptPreset(name: String, copyFromCurrent: Boolean) {
        val newId = UUID.randomUUID().toString()
        val currentPreset = if (copyFromCurrent) {
            _uiState.value.promptPresets.find { it.id == _uiState.value.activePromptPresetId }
        } else null

        val newPreset = PromptPreset(
            id = newId,
            name = name,
            prompts = currentPreset?.prompts ?: emptyMap()
        )
        settingsRepository.savePromptPreset(newPreset)
        settingsRepository.setActivePromptPresetId(newId)
        
        _uiState.update { 
            it.copy(
                promptPresets = settingsRepository.getPromptPresets(),
                activePromptPresetId = newId
            )
        }
    }

    fun renamePromptPreset(id: String, newName: String) {
        val preset = _uiState.value.promptPresets.find { it.id == id } ?: return
        settingsRepository.savePromptPreset(preset.copy(name = newName))
        _uiState.update { it.copy(promptPresets = settingsRepository.getPromptPresets()) }
    }

    fun deletePromptPreset(id: String) {
        if (id == PromptPreset.DEFAULT_PRESET_ID) return
        settingsRepository.deletePromptPreset(id)
        _uiState.update {
            it.copy(
                promptPresets = settingsRepository.getPromptPresets(),
                activePromptPresetId = settingsRepository.getActivePromptPresetId()
            )
        }
    }

    // ── Prompt Preset Export ─────────────────────────────────────────────────

    /** Exports only the currently active [PromptPreset] to a JSON share sheet. */
    fun exportActivePreset() {
        val preset = _uiState.value.run {
            promptPresets.find { it.id == activePromptPresetId } ?: return
        }
        exportPresetsToShare(listOf(preset))
    }

    /** Exports all [PromptPreset]s to a JSON share sheet. */
    fun exportAllPresets() {
        val presets = _uiState.value.promptPresets
        if (presets.isEmpty()) return
        exportPresetsToShare(presets)
    }

    private fun exportPresetsToShare(presets: List<PromptPreset>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Enrich each preset with the full resolved prompt values.
                // PromptPreset.prompts only stores user-modified keys; keys that
                // are still at their built-in default are absent from the map.
                // We call settingsRepository.getCustomPrompt() for every known
                // key so the export file always contains all 8 prompts.
                val allKeys = listOf(
                    "prompt_translation",
                    "prompt_segments",
                    "prompt_clauses",
                    "prompt_grammar",
                    "prompt_tokenizer",
                    "prompt_tokenizer_ocr",
                    "prompt_tokenizer_image",
                    "prompt_tokenizer_image_repair"
                )

                // Switch the active preset to each target preset temporarily so
                // getCustomPrompt() resolves against the correct preset's storage.
                // This is safer than trying to reach into per-preset storage directly.
                val currentActiveId = settingsRepository.getActivePromptPresetId()
                val enrichedPresets = presets.map { preset ->
                    settingsRepository.setActivePromptPresetId(preset.id)
                    val fullPrompts = allKeys.associateWith { key ->
                        settingsRepository.getCustomPrompt(key)
                    }
                    preset.copy(prompts = fullPrompts)
                }
                // Restore the original active preset
                settingsRepository.setActivePromptPresetId(currentActiveId)

                val json = com.example.japanesegrammarapp.utils.PromptPresetExporter.exportToJson(enrichedPresets)
                val tempFile = java.io.File(application.cacheDir, "exports/prompt_presets.json")
                tempFile.parentFile?.mkdirs()
                tempFile.writeText(json)

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    application,
                    "${application.packageName}.fileprovider",
                    tempFile
                )
                _uiEvent.emit(
                    UiEvent.ShareFileEvent(
                        uri = uri,
                        mimeType = "application/json",
                        chooserTitleResId = R.string.export_preset_chooser
                    )
                )
            } catch (e: Exception) {
                AppLogger.e("SETTINGS", "Failed to export prompt presets", e)
                _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.unknown_error))
            }
        }
    }

    // ── Prompt Preset Import ─────────────────────────────────────────────────

    enum class ImportConflictStrategy { RENAME, SKIP }

    /**
     * Parses [json] and saves the contained presets according to [strategy]:
     * - [ImportConflictStrategy.RENAME] – appends " (1)", " (2)" … to duplicate names
     * - [ImportConflictStrategy.SKIP]   – silently drops presets whose name already exists
     */
    fun importPresetsFromJson(json: String, strategy: ImportConflictStrategy) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.japanesegrammarapp.utils.PromptPresetExporter.importFromJson(json)
            result.onFailure { e ->
                val resId = if (e is com.example.japanesegrammarapp.utils.PromptPresetExporter.ImportException
                    && e.message?.contains("no valid") == true
                ) R.string.import_failed_empty else R.string.import_failed_invalid
                _uiEvent.emit(UiEvent.ShowLocalizedError(resId))
                return@launch
            }

            val parsed = result.getOrNull() ?: return@launch
            val existingNames = _uiState.value.promptPresets.map { it.name.lowercase() }.toMutableSet()
            var savedCount = 0

            for (preset in parsed) {
                // The default preset merges directly without name-conflict checks
                if (preset.id == PromptPreset.DEFAULT_PRESET_ID) {
                    settingsRepository.savePromptPreset(preset)
                    savedCount++
                    continue
                }

                val nameLower = preset.name.lowercase()
                when {
                    !existingNames.contains(nameLower) -> {
                        settingsRepository.savePromptPreset(preset)
                        existingNames += nameLower
                        savedCount++
                    }
                    strategy == ImportConflictStrategy.SKIP -> { /* drop */ }
                    strategy == ImportConflictStrategy.RENAME -> {
                        var suffix = 1
                        var candidate = "${preset.name} ($suffix)"
                        while (existingNames.contains(candidate.lowercase())) {
                            suffix++
                            candidate = "${preset.name} ($suffix)"
                        }
                        val renamed = preset.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            name = candidate
                        )
                        settingsRepository.savePromptPreset(renamed)
                        existingNames += candidate.lowercase()
                        savedCount++
                    }
                }
            }

            _uiState.update { it.copy(promptPresets = settingsRepository.getPromptPresets()) }
            _uiEvent.emit(UiEvent.ShowLocalizedError(R.string.import_preset_success, listOf(savedCount)))
        }
    }

    fun setCardFontSizeScale(scale: Float) {
        settingsRepository.setCardFontSizeScale(scale)
    }

    fun setCardSpacingScale(scale: Float) {
        settingsRepository.setCardSpacingScale(scale)
    }

    fun setFuriganaSizeScale(scale: Float) {
        settingsRepository.setFuriganaSizeScale(scale)
    }

    fun setCardInternalPaddingScale(scale: Float) {
        settingsRepository.setCardInternalPaddingScale(scale)
    }

    fun setFuriganaGapScale(scale: Float) {
        settingsRepository.setFuriganaGapScale(scale)
    }

    fun setCardDetailDisplayMode(mode: String) {
        settingsRepository.setCardDetailDisplayMode(mode)
    }

    fun setReasoningLevel(level: ReasoningLevel) {
        settingsRepository.setReasoningLevel(level)
        _uiState.update { it.copy(reasoningLevel = level) }
    }

    fun setComponentReasoningLevel(apiTypeLabel: String, level: ComponentReasoningLevel) {
        settingsRepository.setComponentReasoningLevel(apiTypeLabel, level)
        _uiState.update { state ->
            val updated = state.componentReasoningLevels.toMutableMap().apply {
                this[apiTypeLabel] = level
            }
            state.copy(componentReasoningLevels = updated)
        }
    }

    fun setComponentModelConfig(apiTypeLabel: String, config: com.example.japanesegrammarapp.domain.model.ComponentModelConfig) {
        settingsRepository.setComponentModelConfig(apiTypeLabel, config)
        _uiState.update { state ->
            val updated = state.componentModelConfigs.toMutableMap().apply {
                this[apiTypeLabel] = if (config.isGlobal) {
                    com.example.japanesegrammarapp.domain.model.ComponentModelConfig()
                } else {
                    config
                }
            }
            state.copy(componentModelConfigs = updated)
        }
    }

    fun setUseBackupApi(value: Boolean) {
        settingsRepository.setUseBackupApi(value)
        _uiState.update { it.copy(useBackupApi = value) }
    }

    fun setAutoRetryOnError(value: Boolean) {
        settingsRepository.setAutoRetryOnError(value)
        _uiState.update { it.copy(autoRetryOnError = value) }
    }

    fun setFailoverToNextEndpoint(value: Boolean) {
        settingsRepository.setFailoverToNextEndpoint(value)
        _uiState.update { it.copy(failoverToNextEndpoint = value) }
    }

    private companion object {
        const val WALLPAPER_FILE_NAME = "custom_wallpaper.jpg"
    }
}
