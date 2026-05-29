package com.example.japanesegrammarapp.data.repository

import android.content.SharedPreferences
import com.example.japanesegrammarapp.di.SecurePrefs
import com.example.japanesegrammarapp.di.StandardPrefs
import com.example.japanesegrammarapp.network.LlmConfig
import com.google.gson.Gson
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
    private val cachedActiveModels = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedModelsList = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
    private val cachedApiKeys = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val cachedApiUrls = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun getActiveProvider(): String {
        return cachedActiveProvider ?: synchronized(this) {
            val cached = cachedActiveProvider
            if (cached != null) {
                cached
            } else {
                val value = securePrefs.getString("active_provider", "Gemini") ?: "Gemini"
                cachedActiveProvider = value
                value
            }
        }
    }

    override fun setActiveProvider(provider: String) {
        cachedActiveProvider = provider
        securePrefs.edit().putString("active_provider", provider).apply()
    }

    override fun getActiveModel(provider: String): String {
        var cached = cachedActiveModels[provider]
        if (cached == null) {
            synchronized(cachedActiveModels) {
                cached = cachedActiveModels[provider]
                if (cached == null) {
                    cached = securePrefs.getString("${provider}_selected_model", "") ?: ""
                    cachedActiveModels[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun setActiveModel(provider: String, model: String) {
        cachedActiveModels[provider] = model
        securePrefs.edit().putString("${provider}_selected_model", model).apply()
    }

    override fun getUseOcr(): Boolean {
        return cachedUseOcr ?: synchronized(this) {
            val cached = cachedUseOcr
            if (cached != null) {
                cached
            } else {
                val value = settingPrefs.getBoolean("use_ocr", true)
                cachedUseOcr = value
                value
            }
        }
    }

    override fun setUseOcr(value: Boolean) {
        cachedUseOcr = value
        settingPrefs.edit().putBoolean("use_ocr", value).apply()
    }

    override fun getModelsForProvider(provider: String): List<String> {
        var cached = cachedModelsList[provider]
        if (cached == null) {
            synchronized(cachedModelsList) {
                cached = cachedModelsList[provider]
                if (cached == null) {
                    val json = securePrefs.getString("${provider}_models_list_json", null)
                    cached = if (json != null) {
                        try {
                            val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                            gson.fromJson(json, listType) ?: (LlmConfig.defaultModels[provider] ?: emptyList())
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
        securePrefs.edit().putString("${provider}_models_list_json", gson.toJson(models)).apply()
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

    override fun saveApiKey(provider: String, key: String) {
        cachedApiKeys[provider] = key
        securePrefs.edit().putString("${provider}_key", key).apply()
    }

    override fun getApiUrl(provider: String): String {
        var cached = cachedApiUrls[provider]
        if (cached == null) {
            synchronized(cachedApiUrls) {
                cached = cachedApiUrls[provider]
                if (cached == null) {
                    val defaultUrl = LlmConfig.defaultUrls[provider] ?: ""
                    cached = securePrefs.getString("${provider}_url", defaultUrl) ?: defaultUrl
                    cachedApiUrls[provider] = cached!!
                }
            }
        }
        return cached!!
    }

    override fun saveApiUrl(provider: String, url: String) {
        cachedApiUrls[provider] = url
        securePrefs.edit().putString("${provider}_url", url).apply()
    }
}
