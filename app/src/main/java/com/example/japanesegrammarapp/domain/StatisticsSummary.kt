package com.example.japanesegrammarapp.domain

import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.data.BookmarkedSegment
import java.time.LocalDate

data class StatisticsSummary(
    val totalAnalyses: Int,
    val totalTokens: Int,
    val totalBookmarks: Int,
    val analyzedSentences: List<AnalysisRecord>,
    val bookmarkedVocabulary: List<BookmarkedSegment>,
    val chartData: List<ChartDataPoint>,
    val heatmapData: Map<LocalDate, Int> = emptyMap()
)

data class ChartDataPoint(
    val label: String,
    val analysisCount: Int,
    val tokenUsage: Int
)

enum class StatisticsTimeRange {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}
