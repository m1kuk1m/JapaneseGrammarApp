package com.example.japanesegrammarapp.ui

import com.example.japanesegrammarapp.domain.repository.ApiEndpointConfig

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
    val globalFloatingEnabled: Boolean = false,
    val globalFloatingAction: Int = 1,
    val allProviders: List<String> = emptyList(),
    val providerEndpoints: Map<String, List<ApiEndpointConfig>> = emptyMap()
)
