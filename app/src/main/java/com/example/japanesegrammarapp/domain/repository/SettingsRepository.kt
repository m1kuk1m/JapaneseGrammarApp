package com.example.japanesegrammarapp.domain.repository

interface SettingsRepository {
    fun getAllProviders(): List<String>
    fun getBaseProviderType(providerId: String): String

    fun getApiKey(provider: String): String
    fun saveApiKey(provider: String, key: String): Boolean
    fun getApiUrl(provider: String): String
    fun saveApiUrl(provider: String, url: String)

    fun getActiveProvider(): String
    fun setActiveProvider(provider: String)
    fun getActiveModel(provider: String): String
    fun setActiveModel(provider: String, model: String)
    fun getUseOcr(): Boolean
    fun setUseOcr(value: Boolean)
    fun getImageTokenizerMode(): String
    fun setImageTokenizerMode(mode: String)
    fun getAutoNavigateResult(): Boolean
    fun setAutoNavigateResult(value: Boolean)
    fun getModelsForProvider(provider: String): List<String>
    fun saveModelsForProvider(provider: String, models: List<String>)
    fun getBackupProvider(): String
    fun setBackupProvider(provider: String)
    fun getBackupModel(): String
    fun setBackupModel(model: String)

    fun getThemeMode(): String
    fun setThemeMode(mode: String)
    fun getWallpaperUri(): String
    fun setWallpaperUri(uri: String)

    val themeMode: kotlinx.coroutines.flow.StateFlow<String>
    val wallpaperUri: kotlinx.coroutines.flow.StateFlow<String>

    // Prompt Customization Settings
    fun getCustomPrompt(promptKey: String): String
    fun saveCustomPrompt(promptKey: String, prompt: String)
    fun resetCustomPrompt(promptKey: String)
    fun resetAllCustomPrompts()

    // TTS Settings
    fun getTtsProvider(): String
    fun setTtsProvider(provider: String)
    fun getTtsApiUrl(provider: String): String
    fun setTtsApiUrl(provider: String, url: String)
    fun getTtsApiKey(provider: String): String
    fun setTtsApiKey(provider: String, key: String): Boolean
    fun getTtsModel(provider: String): String
    fun setTtsModel(provider: String, model: String)
    fun getTtsVoice(provider: String): String
    fun setTtsVoice(provider: String, voice: String)
    fun getTtsRegion(provider: String): String
    fun setTtsRegion(provider: String, region: String)
}
