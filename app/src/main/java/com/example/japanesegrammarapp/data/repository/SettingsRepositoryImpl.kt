package com.example.japanesegrammarapp.data.repository

import android.content.SharedPreferences
import com.example.japanesegrammarapp.di.SecurePrefs
import com.example.japanesegrammarapp.di.StandardPrefs
import com.example.japanesegrammarapp.domain.ApplicationScope
import com.example.japanesegrammarapp.domain.model.LlmConfig
import com.example.japanesegrammarapp.domain.model.LlmEndpoint
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectionSettings
import com.example.japanesegrammarapp.domain.model.PromptPreset
import com.example.japanesegrammarapp.domain.repository.LlmApiConfig
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.network.PromptManager
import com.example.japanesegrammarapp.utils.AppLogger
import com.google.gson.Gson
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @StandardPrefs private val settingPrefs: SharedPreferences,
    @SecurePrefs private val securePrefs: SharedPreferences,
    private val gson: Gson,
    @ApplicationScope private val applicationScope: CoroutineScope
) : SettingsRepository {

    // Thread-safe in-memory cache for ultra-fast, non-blocking UI interactions
    @Volatile private var cachedActiveProvider: String? = null
    @Volatile private var cachedUseOcr: Boolean? = null
    @Volatile private var cachedImageTokenizerMode: String? = null
    @Volatile private var cachedOcrBoxDetectionSettings: OcrBoxDetectionSettings? = null
    @Volatile private var cachedBackupProvider: String? = null
    @Volatile private var cachedBackupModel: String? = null
    private val cachedActiveModels = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedModelsList = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
    private val cachedApiKeys = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedApiUrls = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedEndpoints = java.util.concurrent.ConcurrentHashMap<String, List<LlmEndpoint>>()
    private val cachedEndpointApiKeys = java.util.concurrent.ConcurrentHashMap<String, String>()
    
    @Volatile private var cachedThemeMode: String? = null
    @Volatile private var cachedWallpaperUri: String? = null
    @Volatile private var cachedCardFontSizeScale: Float? = null
    @Volatile private var cachedCardSpacingScale: Float? = null
    @Volatile private var cachedFuriganaSizeScale: Float? = null
    @Volatile private var cachedCardInternalPaddingScale: Float? = null
    @Volatile private var cachedFuriganaGapScale: Float? = null
    @Volatile private var cachedCardDetailDisplayMode: String? = null

    private val _themeMode = kotlinx.coroutines.flow.MutableStateFlow("System")
    override val themeMode: kotlinx.coroutines.flow.StateFlow<String> = _themeMode.asStateFlow()

    private val _wallpaperUri = kotlinx.coroutines.flow.MutableStateFlow("")
    override val wallpaperUri: kotlinx.coroutines.flow.StateFlow<String> = _wallpaperUri.asStateFlow()

    private val _cardFontSizeScale = kotlinx.coroutines.flow.MutableStateFlow(1.0f)
    override val cardFontSizeScale: kotlinx.coroutines.flow.StateFlow<Float> = _cardFontSizeScale.asStateFlow()

    private val _cardSpacingScale = kotlinx.coroutines.flow.MutableStateFlow(1.0f)
    override val cardSpacingScale: kotlinx.coroutines.flow.StateFlow<Float> = _cardSpacingScale.asStateFlow()

    private val _furiganaSizeScale = kotlinx.coroutines.flow.MutableStateFlow(1.0f)
    override val furiganaSizeScale: kotlinx.coroutines.flow.StateFlow<Float> = _furiganaSizeScale.asStateFlow()

    private val _cardInternalPaddingScale = kotlinx.coroutines.flow.MutableStateFlow(1.0f)
    override val cardInternalPaddingScale: kotlinx.coroutines.flow.StateFlow<Float> = _cardInternalPaddingScale.asStateFlow()

    private val _furiganaGapScale = kotlinx.coroutines.flow.MutableStateFlow(1.0f)
    override val furiganaGapScale: kotlinx.coroutines.flow.StateFlow<Float> = _furiganaGapScale.asStateFlow()

    private val _cardDetailDisplayMode = kotlinx.coroutines.flow.MutableStateFlow("POPUP")
    override val cardDetailDisplayMode: kotlinx.coroutines.flow.StateFlow<String> = _cardDetailDisplayMode.asStateFlow()

    init {
        applicationScope.launch {
            _themeMode.value = settingPrefs.getString("theme_mode", "System") ?: "System"
            _wallpaperUri.value = settingPrefs.getString("wallpaper_uri", "") ?: ""
            _cardFontSizeScale.value = settingPrefs.getFloat("card_font_size_scale", 1.0f)
            _cardSpacingScale.value = settingPrefs.getFloat("card_spacing_scale", 1.0f)
            _furiganaSizeScale.value = settingPrefs.getFloat("furigana_size_scale", 1.0f)
            _cardInternalPaddingScale.value = settingPrefs.getFloat("card_internal_padding_scale", 1.0f)
            _furiganaGapScale.value = settingPrefs.getFloat("furigana_gap_scale", 1.0f)
            _cardDetailDisplayMode.value = settingPrefs.getString("card_detail_display_mode", "POPUP") ?: "POPUP"
        }
    }

    override fun getAllProviders(): List<String> {
        return LlmConfig.providers
    }

    override fun getBaseProviderType(providerId: String): String {
        return when(providerId) {
            "Gemini" -> "Gemini"
            "Vertex AI" -> "Vertex AI"
            else -> "OpenAI Compatible"
        }
    }

    override fun getAutoNavigateResult(): Boolean {
        return settingPrefs.getBoolean("auto_navigate_result", true)
    }

    override fun setAutoNavigateResult(value: Boolean) {
        settingPrefs.edit().putBoolean("auto_navigate_result", value).apply()
    }

    override fun getAutoDeskewAfterCapture(): Boolean {
        return settingPrefs.getBoolean("auto_deskew_after_capture", false)
    }

    override fun setAutoDeskewAfterCapture(value: Boolean) {
        settingPrefs.edit().putBoolean("auto_deskew_after_capture", value).apply()
    }

    override fun getActiveProvider(): String {
        return cachedActiveProvider ?: synchronized(this) {
            val cached = cachedActiveProvider
            if (cached != null) {
                cached
            } else {
                val value = settingPrefs.getString("active_provider", "Gemini") ?: "Gemini"
                cachedActiveProvider = value
                value
            }
        }
    }

    override fun setActiveProvider(provider: String) {
        cachedActiveProvider = provider
        settingPrefs.edit().putString("active_provider", provider).apply()
    }

    override fun getActiveModel(provider: String): String {
        var cached = cachedActiveModels[provider]
        if (cached == null) {
            synchronized(cachedActiveModels) {
                cached = cachedActiveModels[provider]
                if (cached == null) {
                    cached = settingPrefs.getString("${provider}_selected_model", "") ?: ""
                    cachedActiveModels[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun setActiveModel(provider: String, model: String) {
        cachedActiveModels[provider] = model
        settingPrefs.edit().putString("${provider}_selected_model", model).apply()
    }

    override fun getUseOcr(): Boolean {
        return cachedUseOcr ?: synchronized(this) {
            val cached = cachedUseOcr
            if (cached != null) {
                cached
            } else {
                val value = settingPrefs.getBoolean("use_ocr", false)
                cachedUseOcr = value
                value
            }
        }
    }

    override fun setUseOcr(value: Boolean) {
        cachedUseOcr = value
        settingPrefs.edit().putBoolean("use_ocr", value).apply()
    }

    override fun getImageTokenizerMode(): String {
        return cachedImageTokenizerMode ?: synchronized(this) {
            val cached = cachedImageTokenizerMode
            if (cached != null) {
                cached
            } else {
                val value = settingPrefs.getString("image_tokenizer_mode", "faithful") ?: "faithful"
                cachedImageTokenizerMode = value
                value
            }
        }
    }

    override fun setImageTokenizerMode(mode: String) {
        cachedImageTokenizerMode = mode
        settingPrefs.edit().putString("image_tokenizer_mode", mode).apply()
    }

    override fun getOcrBoxDetectionSettings(): OcrBoxDetectionSettings {
        return cachedOcrBoxDetectionSettings ?: synchronized(this) {
            val cached = cachedOcrBoxDetectionSettings
            if (cached != null) {
                cached
            } else {
                val json = settingPrefs.getString(OCR_BOX_DETECTION_SETTINGS_KEY, null)
                val value = if (!json.isNullOrBlank()) {
                    try {
                        gson.fromJson(json, OcrBoxDetectionSettings::class.java)
                            ?.normalized()
                            ?: OcrBoxDetectionSettings.DEFAULT
                    } catch (e: Exception) {
                        AppLogger.e("SETTINGS", "Failed to parse OCR box detection settings", e)
                        OcrBoxDetectionSettings.DEFAULT
                    }
                } else {
                    OcrBoxDetectionSettings.DEFAULT
                }
                cachedOcrBoxDetectionSettings = value
                value
            }
        }
    }

    override fun setOcrBoxDetectionSettings(settings: OcrBoxDetectionSettings) {
        val normalized = settings.normalized()
        cachedOcrBoxDetectionSettings = normalized
        settingPrefs.edit().putString(OCR_BOX_DETECTION_SETTINGS_KEY, gson.toJson(normalized)).apply()
    }

    override fun resetOcrBoxDetectionSettings() {
        cachedOcrBoxDetectionSettings = OcrBoxDetectionSettings.DEFAULT
        settingPrefs.edit().remove(OCR_BOX_DETECTION_SETTINGS_KEY).apply()
    }

    override fun getModelsForProvider(provider: String): List<String> {
        var cached = cachedModelsList[provider]
        if (cached == null) {
            synchronized(cachedModelsList) {
                cached = cachedModelsList[provider]
                if (cached == null) {
                    val json = settingPrefs.getString("${provider}_models_list_json", null)
                    cached = if (json != null) {
                        try {
                            gson.fromJson(json, Array<String>::class.java)?.toList() ?: (LlmConfig.defaultModels[provider] ?: emptyList())
                        } catch (e: Exception) {
                            LlmConfig.defaultModels[provider] ?: emptyList()
                        }
                    } else {
                        LlmConfig.defaultModels[provider] ?: emptyList()
                    }
                    cachedModelsList[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun saveModelsForProvider(provider: String, models: List<String>) {
        cachedModelsList[provider] = models
        settingPrefs.edit().putString("${provider}_models_list_json", gson.toJson(models)).apply()
    }

    override fun getApiKey(provider: String): String {
        val endpointKey = getEndpoints(provider)
            .asSequence()
            .filter { it.enabled }
            .map { getApiKeyForEndpoint(it.id) }
            .firstOrNull { it.isNotBlank() }
        if (!endpointKey.isNullOrBlank()) {
            return endpointKey
        }

        var cached = cachedApiKeys[provider]
        if (cached == null) {
            synchronized(cachedApiKeys) {
                cached = cachedApiKeys[provider]
                if (cached == null) {
                    cached = securePrefs.getString("${provider}_key", "") ?: ""
                    cachedApiKeys[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun saveApiKey(provider: String, key: String): Boolean {
        val defaultEndpoint = getDefaultEndpoint(provider)
        val endpointSaved = saveSecureString(endpointKey(defaultEndpoint.id), key, "API key", provider) {
            cachedEndpointApiKeys[defaultEndpoint.id] = key
        }
        if (!endpointSaved) {
            return false
        }
        val existingEndpoints = getEndpoints(provider)
        if (existingEndpoints.none { it.id == defaultEndpoint.id }) {
            saveEndpoints(provider, existingEndpoints + defaultEndpoint)
        }
        val legacySaved = saveSecureString("${provider}_key", key, "API key", provider) {
            cachedApiKeys[provider] = key
        }
        return endpointSaved && legacySaved
    }

    override fun getApiUrl(provider: String): String {
        val endpointUrl = getEndpoints(provider)
            .asSequence()
            .filter { it.enabled }
            .map { it.baseUrl }
            .firstOrNull { it.isNotBlank() }
        if (!endpointUrl.isNullOrBlank()) {
            return endpointUrl
        }

        var cached = cachedApiUrls[provider]
        if (cached == null) {
            synchronized(cachedApiUrls) {
                cached = cachedApiUrls[provider]
                if (cached == null) {
                    val defaultUrl = LlmConfig.defaultUrls[provider] ?: ""
                    cached = settingPrefs.getString("${provider}_url", defaultUrl) ?: defaultUrl
                    cachedApiUrls[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun saveApiUrl(provider: String, url: String) {
        cachedApiUrls[provider] = url
        settingPrefs.edit().putString("${provider}_url", url).apply()
        val endpoints = getEndpoints(provider)
        val defaultEndpoint = endpoints.firstOrNull { it.id == defaultEndpointId(provider) } ?: getDefaultEndpoint(provider)
        val updatedDefault = defaultEndpoint.copy(baseUrl = url.ifBlank { LlmConfig.defaultUrls[provider] ?: "" })
        val next = if (endpoints.any { it.id == updatedDefault.id }) {
            endpoints.map { if (it.id == updatedDefault.id) updatedDefault else it }
        } else {
            endpoints + updatedDefault
        }
        saveEndpoints(provider, next)
    }

    override fun getEndpoints(provider: String): List<LlmEndpoint> {
        cachedEndpoints[provider]?.let { return it }
        synchronized(cachedEndpoints) {
            cachedEndpoints[provider]?.let { return it }
            val json = settingPrefs.getString(endpointPoolKey(provider), null)
            val loaded = if (!json.isNullOrBlank()) {
                try {
                    gson.fromJson(json, Array<LlmEndpoint>::class.java)
                        ?.toList()
                        ?.filter { it.provider == provider && it.id.isNotBlank() }
                        ?: emptyList()
                } catch (e: Exception) {
                    AppLogger.e("SETTINGS", "Failed to parse endpoint pool for $provider", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
            val endpoints = if (loaded.isNotEmpty()) loaded else migrateLegacyEndpoint(provider)
            val normalized = endpoints
                .ifEmpty { listOf(getDefaultEndpoint(provider)) }
                .distinctBy { it.id }
                .map { endpoint ->
                    endpoint.copy(enabled = endpoint.enabled && getApiKeyForEndpoint(endpoint.id).isNotBlank())
                }
                .sortedWith(compareBy<LlmEndpoint> { it.priority }.thenBy { it.lastUsedAtMs }.thenBy { it.name })
            cachedEndpoints[provider] = normalized
            if (loaded.isEmpty()) {
                saveEndpoints(provider, normalized)
            }
            return normalized
        }
    }

    override fun getApiKeyForEndpoint(endpointId: String): String {
        var cached = cachedEndpointApiKeys[endpointId]
        if (cached == null) {
            synchronized(cachedEndpointApiKeys) {
                cached = cachedEndpointApiKeys[endpointId]
                if (cached == null) {
                    cached = securePrefs.getString(endpointKey(endpointId), "") ?: ""
                    cachedEndpointApiKeys[endpointId] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun saveEndpoint(endpoint: LlmEndpoint, apiKey: String?): Boolean {
        val nextApiKey = apiKey ?: getApiKeyForEndpoint(endpoint.id)
        val cleanEndpoint = endpoint.copy(
            name = endpoint.name.ifBlank { "Default" },
            baseUrl = endpoint.baseUrl.ifBlank { LlmConfig.defaultUrls[endpoint.provider] ?: "" },
            enabled = endpoint.enabled && nextApiKey.isNotBlank(),
            priority = endpoint.priority.coerceAtLeast(0),
            weight = endpoint.weight.coerceAtLeast(1)
        )
        val keySaved = if (apiKey != null) {
            saveSecureString(endpointKey(cleanEndpoint.id), apiKey, "API key", cleanEndpoint.provider) {
                cachedEndpointApiKeys[cleanEndpoint.id] = apiKey
            }
        } else {
            true
        }
        if (!keySaved) return false

        val endpoints = getEndpoints(cleanEndpoint.provider)
        val next = if (endpoints.any { it.id == cleanEndpoint.id }) {
            endpoints.map { if (it.id == cleanEndpoint.id) cleanEndpoint else it }
        } else {
            endpoints + cleanEndpoint
        }
        saveEndpoints(cleanEndpoint.provider, next)

        if (cleanEndpoint.id == defaultEndpointId(cleanEndpoint.provider)) {
            cachedApiUrls[cleanEndpoint.provider] = cleanEndpoint.baseUrl
            settingPrefs.edit().putString("${cleanEndpoint.provider}_url", cleanEndpoint.baseUrl).apply()
            apiKey?.let { saveSecureString("${cleanEndpoint.provider}_key", it, "API key", cleanEndpoint.provider) {
                cachedApiKeys[cleanEndpoint.provider] = it
            } }
        }
        return true
    }

    override fun deleteEndpoint(provider: String, endpointId: String): Boolean {
        val endpoints = getEndpoints(provider)
        val remaining = endpoints.filterNot { it.id == endpointId }
        val next = remaining.ifEmpty { listOf(getDefaultEndpoint(provider)) }
        val removed = endpoints.size != next.size || endpoints.any { it.id == endpointId }
        if (removed) {
            securePrefs.edit().remove(endpointKey(endpointId)).apply()
            cachedEndpointApiKeys.remove(endpointId)
            saveEndpoints(provider, next)
        }
        return removed
    }

    override fun markEndpointSuccess(provider: String, endpointId: String) {
        updateEndpoint(provider, endpointId) {
            it.copy(
                cooldownUntilMs = 0L,
                consecutiveFailures = 0,
                lastError = null,
                lastUsedAtMs = System.currentTimeMillis()
            )
        }
    }

    override fun markEndpointFailure(provider: String, endpointId: String, error: String, cooldownMs: Long) {
        val now = System.currentTimeMillis()
        updateEndpoint(provider, endpointId) {
            it.copy(
                cooldownUntilMs = now + cooldownMs.coerceAtLeast(0L),
                consecutiveFailures = it.consecutiveFailures + 1,
                lastError = error,
                lastUsedAtMs = now
            )
        }
    }

    override fun touchEndpoint(provider: String, endpointId: String) {
        updateEndpoint(provider, endpointId) {
            it.copy(lastUsedAtMs = System.currentTimeMillis())
        }
    }

    override fun buildLlmApiConfigs(provider: String, modelName: String): List<LlmApiConfig> {
        val now = System.currentTimeMillis()
        val baseProvider = getBaseProviderType(provider)
        val endpoints = getEndpoints(provider)
            .filter { endpoint ->
                endpoint.enabled &&
                    getApiKeyForEndpoint(endpoint.id).isNotBlank() &&
                    (endpoint.cooldownUntilMs <= now || endpoint.cooldownUntilMs == 0L)
            }
            .sortedWith(
                compareBy<LlmEndpoint> { it.priority }
                    .thenByDescending { endpointRotationScore(it, now) }
                    .thenBy { it.consecutiveFailures }
                    .thenBy { it.name }
            )

        return endpoints.map { endpoint ->
            LlmApiConfig(
                provider = provider,
                baseProvider = baseProvider,
                modelName = modelName,
                url = endpoint.baseUrl.ifBlank { LlmConfig.defaultUrls[provider] ?: "" },
                key = getApiKeyForEndpoint(endpoint.id),
                endpointId = endpoint.id,
                endpointName = endpoint.name
            )
        }
    }

    override fun getBackupProvider(): String {
        return cachedBackupProvider ?: synchronized(this) {
            val cached = cachedBackupProvider
            if (cached != null) {
                cached
            } else {
                val value = settingPrefs.getString("backup_provider", "DeepSeek") ?: "DeepSeek"
                cachedBackupProvider = value
                value
            }
        }
    }

    override fun setBackupProvider(provider: String) {
        cachedBackupProvider = provider
        settingPrefs.edit().putString("backup_provider", provider).apply()
    }

    override fun getBackupModel(): String {
        return cachedBackupModel ?: synchronized(this) {
            val cached = cachedBackupModel
            if (cached != null) {
                cached
            } else {
                val value = settingPrefs.getString("backup_model", "") ?: ""
                cachedBackupModel = value
                value
            }
        }
    }

    override fun setBackupModel(model: String) {
        cachedBackupModel = model
        settingPrefs.edit().putString("backup_model", model).apply()
    }

    override fun getThemeMode(): String {
        return cachedThemeMode ?: synchronized(this) {
            val cached = cachedThemeMode
            if (cached != null) cached else {
                val value = settingPrefs.getString("theme_mode", "System") ?: "System"
                cachedThemeMode = value
                value
            }
        }
    }

    override fun setThemeMode(mode: String) {
        cachedThemeMode = mode
        settingPrefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    override fun getWallpaperUri(): String {
        return cachedWallpaperUri ?: synchronized(this) {
            val cached = cachedWallpaperUri
            if (cached != null) cached else {
                val value = settingPrefs.getString("wallpaper_uri", "") ?: ""
                cachedWallpaperUri = value
                value
            }
        }
    }

    override fun setWallpaperUri(uri: String) {
        cachedWallpaperUri = uri
        settingPrefs.edit().putString("wallpaper_uri", uri).apply()
        _wallpaperUri.value = uri
    }

    // TTS Implementation
    @Volatile private var cachedTtsProvider: String? = null
    private val cachedTtsApiUrls = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedTtsApiKeys = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedTtsModels = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedTtsVoices = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedTtsRegions = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun getTtsProvider(): String {
        return cachedTtsProvider ?: synchronized(this) {
            val cached = cachedTtsProvider
            if (cached != null) cached else {
                val value = settingPrefs.getString("tts_provider", "OpenAI") ?: "OpenAI"
                cachedTtsProvider = value
                value
            }
        }
    }

    override fun setTtsProvider(provider: String) {
        cachedTtsProvider = provider
        settingPrefs.edit().putString("tts_provider", provider).apply()
    }

    override fun getTtsApiUrl(provider: String): String {
        var cached = cachedTtsApiUrls[provider]
        if (cached == null) {
            synchronized(cachedTtsApiUrls) {
                cached = cachedTtsApiUrls[provider]
                if (cached == null) {
                    val defaultUrl = when(provider) {
                        "OpenAI" -> "https://api.openai.com/v1/audio/speech"
                        "Google" -> "https://texttospeech.googleapis.com/v1/text:synthesize"
                        "Microsoft" -> "" // Region based, constructed dynamically or input by user
                        else -> ""
                    }
                    cached = settingPrefs.getString("tts_${provider}_url", defaultUrl) ?: defaultUrl
                    cachedTtsApiUrls[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun setTtsApiUrl(provider: String, url: String) {
        cachedTtsApiUrls[provider] = url
        settingPrefs.edit().putString("tts_${provider}_url", url).apply()
    }

    override fun getTtsApiKey(provider: String): String {
        var cached = cachedTtsApiKeys[provider]
        if (cached == null) {
            synchronized(cachedTtsApiKeys) {
                cached = cachedTtsApiKeys[provider]
                if (cached == null) {
                    cached = securePrefs.getString("tts_${provider}_key", "") ?: ""
                    cachedTtsApiKeys[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun setTtsApiKey(provider: String, key: String): Boolean {
        return saveSecureString("tts_${provider}_key", key, "TTS API key", provider) {
            cachedTtsApiKeys[provider] = key
        }
    }

    override fun getTtsModel(provider: String): String {
        var cached = cachedTtsModels[provider]
        if (cached == null) {
            synchronized(cachedTtsModels) {
                cached = cachedTtsModels[provider]
                if (cached == null) {
                    val defaultModel = when(provider) {
                        "OpenAI" -> "tts-1"
                        else -> ""
                    }
                    cached = settingPrefs.getString("tts_${provider}_model", defaultModel) ?: defaultModel
                    cachedTtsModels[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun setTtsModel(provider: String, model: String) {
        cachedTtsModels[provider] = model
        settingPrefs.edit().putString("tts_${provider}_model", model).apply()
    }

    override fun getTtsVoice(provider: String): String {
        var cached = cachedTtsVoices[provider]
        if (cached == null) {
            synchronized(cachedTtsVoices) {
                cached = cachedTtsVoices[provider]
                if (cached == null) {
                    val defaultVoice = when(provider) {
                        "OpenAI" -> "alloy"
                        "Google" -> "ja-JP-Neural2-B"
                        "Microsoft" -> "ja-JP-NanamiNeural"
                        else -> ""
                    }
                    cached = settingPrefs.getString("tts_${provider}_voice", defaultVoice) ?: defaultVoice
                    cachedTtsVoices[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun setTtsVoice(provider: String, voice: String) {
        cachedTtsVoices[provider] = voice
        settingPrefs.edit().putString("tts_${provider}_voice", voice).apply()
    }

    override fun getTtsRegion(provider: String): String {
        var cached = cachedTtsRegions[provider]
        if (cached == null) {
            synchronized(cachedTtsRegions) {
                cached = cachedTtsRegions[provider]
                if (cached == null) {
                    cached = settingPrefs.getString("tts_${provider}_region", "eastus") ?: "eastus"
                    cachedTtsRegions[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun setTtsRegion(provider: String, region: String) {
        cachedTtsRegions[provider] = region
        settingPrefs.edit().putString("tts_${provider}_region", region).apply()
    }

    // Prompt Customization Cache and Implementation
    @Volatile private var cachedActivePromptPresetId: String? = null
    private val cachedPromptPresets = mutableListOf<PromptPreset>()

    private fun loadPresetsIfEmpty() {
        if (cachedPromptPresets.isEmpty()) {
            synchronized(this) {
                if (cachedPromptPresets.isEmpty()) {
                    val json = settingPrefs.getString("prompt_presets_json", null)
                    if (json != null) {
                        try {
                            val loaded = gson.fromJson(json, Array<PromptPreset>::class.java).toList()
                            cachedPromptPresets.addAll(loaded)
                        } catch (e: Exception) {
                            AppLogger.e("SETTINGS", "Failed to parse prompt presets", e)
                        }
                    }

                    // Migration logic
                    if (cachedPromptPresets.isEmpty()) {
                        val legacyPrompts = mutableMapOf<String, String>()
                        val promptKeys = listOf(
                            "prompt_translation", "prompt_segments", "prompt_clauses",
                            "prompt_grammar", "prompt_tokenizer", "prompt_tokenizer_ocr",
                            "prompt_tokenizer_image", "prompt_tokenizer_image_repair"
                        )
                        var hasLegacy = false
                        for (key in promptKeys) {
                            if (settingPrefs.contains(key)) {
                                legacyPrompts[key] = settingPrefs.getString(key, "") ?: ""
                                hasLegacy = true
                            }
                        }
                        
                        val defaultPreset = PromptPreset(
                            id = PromptPreset.DEFAULT_PRESET_ID,
                            name = "Default",
                            prompts = legacyPrompts
                        )
                        cachedPromptPresets.add(defaultPreset)
                        settingPrefs.edit().putString("prompt_presets_json", gson.toJson(cachedPromptPresets)).apply()
                        
                        // Clean up legacy keys
                        if (hasLegacy) {
                            val editor = settingPrefs.edit()
                            for (key in promptKeys) {
                                editor.remove(key)
                            }
                            editor.apply()
                        }
                    }
                }
            }
        }
    }

    override fun getPromptPresets(): List<PromptPreset> {
        loadPresetsIfEmpty()
        return cachedPromptPresets.toList()
    }

    override fun getActivePromptPresetId(): String {
        return cachedActivePromptPresetId ?: synchronized(this) {
            val cached = cachedActivePromptPresetId
            if (cached != null) cached else {
                val value = settingPrefs.getString("active_prompt_preset_id", PromptPreset.DEFAULT_PRESET_ID) ?: PromptPreset.DEFAULT_PRESET_ID
                cachedActivePromptPresetId = value
                value
            }
        }
    }

    override fun setActivePromptPresetId(id: String) {
        cachedActivePromptPresetId = id
        settingPrefs.edit().putString("active_prompt_preset_id", id).apply()
    }

    override fun savePromptPreset(preset: PromptPreset) {
        loadPresetsIfEmpty()
        synchronized(this) {
            val index = cachedPromptPresets.indexOfFirst { it.id == preset.id }
            if (index != -1) {
                cachedPromptPresets[index] = preset
            } else {
                cachedPromptPresets.add(preset)
            }
            settingPrefs.edit().putString("prompt_presets_json", gson.toJson(cachedPromptPresets)).apply()
        }
    }

    override fun deletePromptPreset(id: String) {
        if (id == PromptPreset.DEFAULT_PRESET_ID) return // Cannot delete default preset
        loadPresetsIfEmpty()
        synchronized(this) {
            cachedPromptPresets.removeAll { it.id == id }
            settingPrefs.edit().putString("prompt_presets_json", gson.toJson(cachedPromptPresets)).apply()
            
            if (getActivePromptPresetId() == id) {
                setActivePromptPresetId(PromptPreset.DEFAULT_PRESET_ID)
            }
        }
    }

    private fun getActivePreset(): PromptPreset {
        loadPresetsIfEmpty()
        val activeId = getActivePromptPresetId()
        return cachedPromptPresets.find { it.id == activeId } 
            ?: cachedPromptPresets.find { it.id == PromptPreset.DEFAULT_PRESET_ID }
            ?: PromptPreset(PromptPreset.DEFAULT_PRESET_ID, "Default")
    }

    override fun getCustomPrompt(promptKey: String): String {
        val preset = getActivePreset()
        val customVal = preset.prompts[promptKey]
        if (customVal != null) {
            return customVal
        }

        return when (promptKey) {
            "prompt_translation" -> PromptManager.SYSTEM_PROMPT_TRANSLATION
            "prompt_segments" -> PromptManager.SYSTEM_PROMPT_SEGMENTS
            "prompt_clauses" -> PromptManager.SYSTEM_PROMPT_CLAUSES
            "prompt_grammar" -> PromptManager.SYSTEM_PROMPT_GRAMMAR
            "prompt_tokenizer" -> PromptManager.SYSTEM_PROMPT_TOKENIZER
            "prompt_tokenizer_ocr" -> PromptManager.SYSTEM_PROMPT_TOKENIZER_OCR
            "prompt_tokenizer_image" -> PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE
            "prompt_tokenizer_image_repair" -> PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE_REPAIR
            else -> ""
        }
    }

    override fun saveCustomPrompt(promptKey: String, prompt: String) {
        val preset = getActivePreset()
        val newPrompts = preset.prompts.toMutableMap()
        newPrompts[promptKey] = prompt
        savePromptPreset(preset.copy(prompts = newPrompts))
    }

    override fun resetCustomPrompt(promptKey: String) {
        val preset = getActivePreset()
        val newPrompts = preset.prompts.toMutableMap()
        newPrompts.remove(promptKey)
        savePromptPreset(preset.copy(prompts = newPrompts))
    }

    override fun resetAllCustomPrompts() {
        val preset = getActivePreset()
        savePromptPreset(preset.copy(prompts = emptyMap()))
    }

    private fun endpointPoolKey(provider: String): String = "llm_${provider}_endpoints_json"

    private companion object {
        const val OCR_BOX_DETECTION_SETTINGS_KEY = "ocr_box_detection_settings_json"
    }

    private fun endpointKey(endpointId: String): String = "llm_endpoint_${endpointId}_key"

    private fun defaultEndpointId(provider: String): String =
        "default_${provider.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}"

    private fun getDefaultEndpoint(provider: String): LlmEndpoint {
        return LlmEndpoint(
            id = defaultEndpointId(provider),
            provider = provider,
            name = "Default",
            baseUrl = LlmConfig.defaultUrls[provider] ?: "",
            enabled = true,
            priority = 0,
            weight = 1
        )
    }

    private fun migrateLegacyEndpoint(provider: String): List<LlmEndpoint> {
        val legacyKey = securePrefs.getString("${provider}_key", "") ?: ""
        val defaultUrl = LlmConfig.defaultUrls[provider] ?: ""
        val legacyUrl = settingPrefs.getString("${provider}_url", defaultUrl) ?: defaultUrl
        val endpoint = getDefaultEndpoint(provider).copy(baseUrl = legacyUrl)

        if (legacyKey.isNotBlank()) {
            saveSecureString(endpointKey(endpoint.id), legacyKey, "API key", provider) {
                cachedEndpointApiKeys[endpoint.id] = legacyKey
            }
        }

        return listOf(endpoint)
    }

    private fun saveEndpoints(provider: String, endpoints: List<LlmEndpoint>) {
        val normalized = endpoints
            .filter { it.provider == provider && it.id.isNotBlank() }
            .ifEmpty { listOf(getDefaultEndpoint(provider)) }
            .distinctBy { it.id }
            .map { endpoint ->
                endpoint.copy(
                    name = endpoint.name.ifBlank { "Default" },
                    baseUrl = endpoint.baseUrl.ifBlank { LlmConfig.defaultUrls[provider] ?: "" },
                    enabled = endpoint.enabled && getApiKeyForEndpoint(endpoint.id).isNotBlank(),
                    priority = endpoint.priority.coerceAtLeast(0),
                    weight = endpoint.weight.coerceAtLeast(1)
                )
            }
            .sortedWith(compareBy<LlmEndpoint> { it.priority }.thenBy { it.lastUsedAtMs }.thenBy { it.name })

        cachedEndpoints[provider] = normalized
        settingPrefs.edit().putString(endpointPoolKey(provider), gson.toJson(normalized)).apply()
    }

    private fun updateEndpoint(provider: String, endpointId: String, transform: (LlmEndpoint) -> LlmEndpoint) {
        val endpoints = getEndpoints(provider)
        val next = endpoints.map { endpoint ->
            if (endpoint.id == endpointId) transform(endpoint) else endpoint
        }
        saveEndpoints(provider, next)
    }

    private fun endpointRotationScore(endpoint: LlmEndpoint, now: Long): Long {
        val weight = endpoint.weight.coerceAtLeast(1)
        if (endpoint.lastUsedAtMs <= 0L) {
            return Long.MAX_VALUE
        }
        val idleSeconds = ((now - endpoint.lastUsedAtMs).coerceAtLeast(0L) / 1000L)
        return idleSeconds.coerceAtMost(Long.MAX_VALUE / weight) * weight
    }

    private fun saveSecureString(
        prefKey: String,
        value: String,
        label: String,
        provider: String,
        onSuccess: () -> Unit
    ): Boolean {
        return try {
            val saved = securePrefs.edit().putString(prefKey, value).commit()
            if (saved) {
                onSuccess()
            } else {
                AppLogger.e("SETTINGS", "Failed to save $label for $provider: SharedPreferences commit returned false")
            }
            saved
        } catch (e: Exception) {
            AppLogger.e("SETTINGS", "Failed to save $label for $provider", e)
            false
        }
    }

    override fun getCardFontSizeScale(): Float {
        return cachedCardFontSizeScale ?: synchronized(this) {
            val cached = cachedCardFontSizeScale
            if (cached != null) cached else {
                val value = settingPrefs.getFloat("card_font_size_scale", 1.0f)
                cachedCardFontSizeScale = value
                value
            }
        }
    }

    override fun setCardFontSizeScale(scale: Float) {
        cachedCardFontSizeScale = scale
        settingPrefs.edit().putFloat("card_font_size_scale", scale).apply()
        _cardFontSizeScale.value = scale
    }

    override fun getCardSpacingScale(): Float {
        return cachedCardSpacingScale ?: synchronized(this) {
            val cached = cachedCardSpacingScale
            if (cached != null) cached else {
                val value = settingPrefs.getFloat("card_spacing_scale", 1.0f)
                cachedCardSpacingScale = value
                value
            }
        }
    }

    override fun setCardSpacingScale(scale: Float) {
        cachedCardSpacingScale = scale
        settingPrefs.edit().putFloat("card_spacing_scale", scale).apply()
        _cardSpacingScale.value = scale
    }

    override fun getFuriganaSizeScale(): Float {
        return cachedFuriganaSizeScale ?: synchronized(this) {
            val cached = cachedFuriganaSizeScale
            if (cached != null) cached else {
                val value = settingPrefs.getFloat("furigana_size_scale", 1.0f)
                cachedFuriganaSizeScale = value
                value
            }
        }
    }

    override fun setFuriganaSizeScale(scale: Float) {
        cachedFuriganaSizeScale = scale
        settingPrefs.edit().putFloat("furigana_size_scale", scale).apply()
        _furiganaSizeScale.value = scale
    }

    override fun getCardInternalPaddingScale(): Float {
        return cachedCardInternalPaddingScale ?: synchronized(this) {
            val cached = cachedCardInternalPaddingScale
            if (cached != null) cached else {
                val value = settingPrefs.getFloat("card_internal_padding_scale", 1.0f)
                cachedCardInternalPaddingScale = value
                value
            }
        }
    }

    override fun setCardInternalPaddingScale(scale: Float) {
        cachedCardInternalPaddingScale = scale
        settingPrefs.edit().putFloat("card_internal_padding_scale", scale).apply()
        _cardInternalPaddingScale.value = scale
    }

    override fun getFuriganaGapScale(): Float {
        return cachedFuriganaGapScale ?: synchronized(this) {
            val cached = cachedFuriganaGapScale
            if (cached != null) cached else {
                val value = settingPrefs.getFloat("furigana_gap_scale", 1.0f)
                cachedFuriganaGapScale = value
                value
            }
        }
    }

    override fun setFuriganaGapScale(scale: Float) {
        cachedFuriganaGapScale = scale
        settingPrefs.edit().putFloat("furigana_gap_scale", scale).apply()
        _furiganaGapScale.value = scale
    }

    override fun getCardDetailDisplayMode(): String {
        return cachedCardDetailDisplayMode ?: synchronized(this) {
            val cached = cachedCardDetailDisplayMode
            if (cached != null) cached else {
                val value = settingPrefs.getString("card_detail_display_mode", "POPUP") ?: "POPUP"
                cachedCardDetailDisplayMode = value
                value
            }
        }
    }

    override fun setCardDetailDisplayMode(mode: String) {
        cachedCardDetailDisplayMode = mode
        settingPrefs.edit().putString("card_detail_display_mode", mode).apply()
        _cardDetailDisplayMode.value = mode
    }
}
