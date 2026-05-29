package com.example.japanesegrammarapp.data.repository

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
}
