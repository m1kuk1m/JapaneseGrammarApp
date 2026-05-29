package com.example.japanesegrammarapp.ui

import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.network.DetailedAnalysisResult

data class WorkspaceUiState(
    val activeProvider: String = "Gemini",
    val activeModel: String = "",
    val availableModels: List<String> = emptyList(),
    val providerModels: Map<String, List<String>> = emptyMap(),
    val selectedRecord: AnalysisRecord? = null,
    val currentOriginalText: String = "",
    val useOcr: Boolean = true,
    val isFetchingModels: Boolean = false,
    val fetchingProvider: String? = null,
    val analysisResult: String? = null,
    val detailedResult: DetailedAnalysisResult? = null,
    val isParsingDetailedResult: Boolean = false
)
