package com.example.japanesegrammarapp.ui

import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.usecase.AnalysisProgress
import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult

data class WorkspaceUiState(
    val activeProvider: String = "Gemini",
    val activeModel: String = "",
    val availableModels: List<String> = emptyList(),
    val providerModels: Map<String, List<String>> = emptyMap(),
    val selectedRecord: AnalysisDomainRecord? = null,
    val currentOriginalText: String = "",
    val useOcr: Boolean = true,
    val isFetchingModels: Boolean = false,
    val fetchingProvider: String? = null,
    val analysisResult: String? = null,
    val detailedResult: DetailedAnalysisResult? = null,
    val isParsingDetailedResult: Boolean = false,
    val selectedRecordProgress: AnalysisProgress? = null,
    val backupProvider: String = "DeepSeek",
    val backupModel: String = "",
    val themeMode: String = "System",
    val primaryColor: String = "Default",
    val wallpaperUri: String = ""
)
