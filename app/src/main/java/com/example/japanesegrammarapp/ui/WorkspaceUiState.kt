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
    val useOcr: Boolean = false,
    val autoNavigateResult: Boolean = true,
    val imageTokenizerMode: String = "faithful",
    val analysisResult: String? = null,
    val detailedResult: DetailedAnalysisResult? = null,
    val isParsingDetailedResult: Boolean = false,
    val selectedRecordProgress: AnalysisProgress? = null,
    val wallpaperUri: String = "",
    val isExternalQuery: Boolean = false,
    /** segmentText values bookmarked for the currently selected record */
    val bookmarkedSegmentTexts: Set<String> = emptySet(),
    val bookmarkedGrammarPointPatterns: Set<String> = emptySet(),
    val isSentenceBookmarked: Boolean = false,
    val cardFontSizeScale: Float = 1.0f,
    val cardSpacingScale: Float = 1.0f,
    val furiganaSizeScale: Float = 1.0f,
    val cardDetailDisplayMode: String = "INLINE"
)
