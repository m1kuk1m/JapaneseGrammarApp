package com.example.japanesegrammarapp.domain.repository

interface SettingsRepository {
    fun getActiveProvider(): String
    fun setActiveProvider(provider: String)
    fun getActiveModel(provider: String): String
    fun setActiveModel(provider: String, model: String)
    fun getUseOcr(): Boolean
    fun setUseOcr(value: Boolean)
    fun getModelsForProvider(provider: String): List<String>
    fun saveModelsForProvider(provider: String, models: List<String>)
    fun getApiKey(provider: String): String
    fun saveApiKey(provider: String, key: String)
    fun getApiUrl(provider: String): String
    fun saveApiUrl(provider: String, url: String)
    fun getBackupProvider(): String
    fun setBackupProvider(provider: String)
    fun getBackupModel(): String
    fun setBackupModel(model: String)
    
    fun getThemeMode(): String
    fun setThemeMode(mode: String)
    fun getPrimaryColor(): String
    fun setPrimaryColor(colorHex: String)
    fun getWallpaperUri(): String
    fun setWallpaperUri(uri: String)

    val themeMode: kotlinx.coroutines.flow.StateFlow<String>
    val primaryColor: kotlinx.coroutines.flow.StateFlow<String>
    val wallpaperUri: kotlinx.coroutines.flow.StateFlow<String>

    // TTS Settings
    fun getTtsProvider(): String
    fun setTtsProvider(provider: String)
    fun getTtsApiUrl(provider: String): String
    fun setTtsApiUrl(provider: String, url: String)
    fun getTtsApiKey(provider: String): String
    fun setTtsApiKey(provider: String, key: String)
    fun getTtsModel(provider: String): String
    fun setTtsModel(provider: String, model: String)
    fun getTtsVoice(provider: String): String
    fun setTtsVoice(provider: String, voice: String)
    fun getTtsRegion(provider: String): String
    fun setTtsRegion(provider: String, region: String)
}
