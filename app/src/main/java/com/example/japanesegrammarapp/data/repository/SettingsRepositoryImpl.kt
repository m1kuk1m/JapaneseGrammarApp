package com.example.japanesegrammarapp.data.repository

import android.content.SharedPreferences
import com.example.japanesegrammarapp.di.SecurePrefs
import com.example.japanesegrammarapp.di.StandardPrefs
import com.example.japanesegrammarapp.domain.model.LlmConfig
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.network.PromptManager
import com.example.japanesegrammarapp.utils.AppLogger
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @StandardPrefs private val settingPrefs: SharedPreferences,
    @SecurePrefs private val securePrefs: SharedPreferences,
    private val gson: Gson
) : SettingsRepository {

    // Thread-safe in-memory cache for ultra-fast, non-blocking UI interactions
    @Volatile private var cachedActiveProvider: String? = null
    @Volatile private var cachedUseOcr: Boolean? = null
    @Volatile private var cachedImageTokenizerMode: String? = null
    @Volatile private var cachedBackupProvider: String? = null
    @Volatile private var cachedBackupModel: String? = null
    private val cachedActiveModels = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedModelsList = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
    private val cachedApiKeys = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedApiUrls = java.util.concurrent.ConcurrentHashMap<String, String>()
    
    @Volatile private var cachedThemeMode: String? = null
    @Volatile private var cachedWallpaperUri: String? = null

    private val _themeMode = kotlinx.coroutines.flow.MutableStateFlow("System")
    override val themeMode: kotlinx.coroutines.flow.StateFlow<String> = _themeMode.asStateFlow()

    private val _wallpaperUri = kotlinx.coroutines.flow.MutableStateFlow("")
    override val wallpaperUri: kotlinx.coroutines.flow.StateFlow<String> = _wallpaperUri.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _themeMode.value = settingPrefs.getString("theme_mode", "System") ?: "System"
            _wallpaperUri.value = settingPrefs.getString("wallpaper_uri", "") ?: ""
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
        return saveSecureString("${provider}_key", key, "API key", provider) {
            cachedApiKeys[provider] = key
        }
    }

    override fun getApiUrl(provider: String): String {
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
    private val cachedPrompts = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun getCustomPrompt(promptKey: String): String {
        var cached = cachedPrompts[promptKey]
        if (cached == null) {
            val defaultVal = when (promptKey) {
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
            cached = settingPrefs.getString(promptKey, defaultVal) ?: defaultVal
            cachedPrompts[promptKey] = cached
        }
        return cached
    }

    override fun saveCustomPrompt(promptKey: String, prompt: String) {
        cachedPrompts[promptKey] = prompt
        settingPrefs.edit().putString(promptKey, prompt).apply()
    }

    override fun resetCustomPrompt(promptKey: String) {
        cachedPrompts.remove(promptKey)
        settingPrefs.edit().remove(promptKey).apply()
    }

    override fun resetAllCustomPrompts() {
        cachedPrompts.clear()
        settingPrefs.edit().apply {
            remove("prompt_translation")
            remove("prompt_segments")
            remove("prompt_clauses")
            remove("prompt_grammar")
            remove("prompt_tokenizer")
            remove("prompt_tokenizer_ocr")
            remove("prompt_tokenizer_image")
            remove("prompt_tokenizer_image_repair")
        }.apply()
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
}
