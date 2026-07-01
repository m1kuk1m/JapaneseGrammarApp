package com.example.japanesegrammarapp.domain

import com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import java.time.LocalDate

data class StatisticsSummary(
    val totalAnalyses: Int,
    val totalTokens: Int,
    val totalBookmarks: Int,
    val analyzedSentences: List<BookmarkedSentenceDomain>,
    val bookmarkedSentences: List<BookmarkedSentenceDomain> = emptyList(),
    val bookmarkedVocabulary: List<BookmarkedSegmentDomain>,
    val bookmarkedGrammar: List<BookmarkedGrammarPointDomain> = emptyList(),
    val chartData: List<ChartDataPoint>,
    val heatmapData: Map<LocalDate, Int> = emptyMap()
)

data class ChartDataPoint(
    val label: String,
    val analysisCount: Int,
    val tokenUsage: Int,
    val date: LocalDate? = null
)

enum class StatisticsTimeRange {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ALL_TIME
}
