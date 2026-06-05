package com.example.japanesegrammarapp.ui

data class SettingsUiState(
    val activeProvider: String = "Gemini",
    val activeModel: String = "",
    val availableModels: List<String> = emptyList(),
    val providerModels: Map<String, List<String>> = emptyMap(),
    val useOcr: Boolean = true,
    val autoNavigateResult: Boolean = true,
    val imageTokenizerMode: String = "repair",
    val isFetchingModels: Boolean = false,
    val fetchingProvider: String? = null,
    val backupProvider: String = "DeepSeek",
    val backupModel: String = "",
    val themeMode: String = "System",
    val wallpaperUri: String = "",
    val allProviders: List<String> = emptyList(),
    val isSettingsLoaded: Boolean = false,
    val selectedTtsProvider: String = "OpenAI",
    val ttsUrls: Map<String, String> = emptyMap(),
    val ttsKeys: Map<String, String> = emptyMap(),
    val ttsModels: Map<String, String> = emptyMap(),
    val ttsVoices: Map<String, String> = emptyMap(),
    val ttsRegions: Map<String, String> = emptyMap(),
    val providerUrls: Map<String, String> = emptyMap(),
    val providerKeys: Map<String, String> = emptyMap()
)
