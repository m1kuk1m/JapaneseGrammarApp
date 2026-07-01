package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.data.AnalysisDao
import com.example.japanesegrammarapp.data.BookmarkDao
import com.example.japanesegrammarapp.data.BookmarkedSentenceDao
import com.example.japanesegrammarapp.data.mapper.toDomain
import com.example.japanesegrammarapp.domain.ChartDataPoint
import com.example.japanesegrammarapp.domain.StatisticsSummary
import com.example.japanesegrammarapp.domain.StatisticsTimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class StatisticsRepository @Inject constructor(
    private val analysisDao: AnalysisDao,
    private val bookmarkDao: BookmarkDao,
    private val sentenceDao: BookmarkedSentenceDao,
    private val grammarPointDao: com.example.japanesegrammarapp.data.BookmarkedGrammarPointDao
) {
    suspend fun getStatisticsSummary(
        timeRange: StatisticsTimeRange,
        referenceDate: LocalDate
    ): StatisticsSummary = withContext(Dispatchers.IO) {
        val (startDate, endDate) = getStartAndEndDate(timeRange, referenceDate)
        
        val startMillis = if (timeRange == StatisticsTimeRange.ALL_TIME) 0L else startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = if (timeRange == StatisticsTimeRange.ALL_TIME) Long.MAX_VALUE else endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        val records = analysisDao.getRecordsByTimeRange(startMillis, endMillis)
        val sentenceBookmarks = sentenceDao.getBookmarksByTimeRange(startMillis, endMillis)
        val bookmarks = bookmarkDao.getBookmarksByTimeRange(startMillis, endMillis)
        val grammarPoints = grammarPointDao.getBookmarksByTimeRange(startMillis, endMillis)
        
        val totalAnalyses = records.size
        val totalTokens = records.sumOf { it.consumedTokens }
        val totalBookmarks = sentenceBookmarks.size + bookmarks.size + grammarPoints.size

        val chartData = buildChartData(timeRange, startDate, endDate, records)
        
        val heatmapData = if (timeRange == StatisticsTimeRange.YEARLY) {
            buildHeatmapData(startMillis, endMillis)
        } else {
            emptyMap()
        }

        val sentencesDomain = records.map {
            com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain(
                id = it.id,
                recordId = it.id,
                originalText = it.originalText,
                translation = null,
                analysisResult = it.analysisResult,
                modelUsed = it.modelUsed,
                bookmarkedAt = it.timestamp
            )
        }

        val sentenceBookmarksDomain = sentenceBookmarks.map { it.toDomain() }

        val vocabularyDomain = bookmarks.map {
            com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain(
                id = it.id,
                recordId = it.recordId,
                segmentText = it.segmentText,
                surfaceForm = it.surfaceForm,
                reading = it.reading,
                partOfSpeech = it.partOfSpeech,
                posCategory = it.posCategory,
                dictionaryForm = it.dictionaryForm,
                dictionaryFormReading = it.dictionaryFormReading,
                meaning = it.meaning,
                inflection = it.inflection,
                role = it.role,
                bookmarkedAt = it.bookmarkedAt,
                sourceText = it.sourceText,
                isArchived = it.isArchived
            )
        }

        val grammarDomain = grammarPoints.map {
            com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain(
                id = it.id,
                recordId = it.recordId,
                pattern = it.pattern,
                explanation = it.explanation,
                bookmarkedAt = it.bookmarkedAt,
                sourceText = it.sourceText,
                isArchived = it.isArchived
            )
        }

        StatisticsSummary(
            totalAnalyses = totalAnalyses,
            totalTokens = totalTokens,
            totalBookmarks = totalBookmarks,
            analyzedSentences = sentencesDomain,
            bookmarkedSentences = sentenceBookmarksDomain,
            bookmarkedVocabulary = vocabularyDomain,
            bookmarkedGrammar = grammarDomain,
            chartData = chartData,
            heatmapData = heatmapData
        )
    }

    private fun getStartAndEndDate(timeRange: StatisticsTimeRange, referenceDate: LocalDate): Pair<LocalDate, LocalDate> {
        return when (timeRange) {
            StatisticsTimeRange.DAILY -> {
                Pair(referenceDate, referenceDate)
            }
            StatisticsTimeRange.WEEKLY -> {
                val dayOfWeek = referenceDate.dayOfWeek.value
                val start = referenceDate.minusDays(dayOfWeek - 1L)
                val end = start.plusDays(6)
                Pair(start, end)
            }
            StatisticsTimeRange.MONTHLY -> {
                val start = referenceDate.withDayOfMonth(1)
                val end = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth())
                Pair(start, end)
            }
            StatisticsTimeRange.YEARLY -> {
                val start = referenceDate.withDayOfYear(1)
                val end = referenceDate.withDayOfYear(referenceDate.lengthOfYear())
                Pair(start, end)
            }
            StatisticsTimeRange.ALL_TIME -> {
                Pair(referenceDate, referenceDate)
            }
        }
    }

    private fun buildChartData(
        timeRange: StatisticsTimeRange,
        startDate: LocalDate,
        endDate: LocalDate,
        records: List<com.example.japanesegrammarapp.data.AnalysisRecord>
    ): List<ChartDataPoint> {
        return when (timeRange) {
            StatisticsTimeRange.DAILY -> {
                (0..23).map { hour ->
                    val recordsInHour = records.filter {
                        val dt = java.time.Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
                        dt.hour == hour
                    }
                    ChartDataPoint(
                        label = String.format(Locale.getDefault(), "%02d", hour),
                        analysisCount = recordsInHour.size,
                        tokenUsage = recordsInHour.sumOf { it.consumedTokens },
                        date = startDate
                    )
                }
            }
            StatisticsTimeRange.WEEKLY -> {
                (0..6).map { offset ->
                    val d = startDate.plusDays(offset.toLong())
                    val recordsInDay = records.filter {
                        val dt = java.time.Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        dt == d
                    }
                    val label = d.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
                    ChartDataPoint(
                        label = label,
                        analysisCount = recordsInDay.size,
                        tokenUsage = recordsInDay.sumOf { it.consumedTokens },
                        date = d
                    )
                }
            }
            StatisticsTimeRange.MONTHLY -> {
                val daysInMonth = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
                (0 until daysInMonth).map { offset ->
                    val d = startDate.plusDays(offset.toLong())
                    val recordsInDay = records.filter {
                        val dt = java.time.Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        dt == d
                    }
                    val label = d.dayOfMonth.toString()
                    ChartDataPoint(
                        label = label,
                        analysisCount = recordsInDay.size,
                        tokenUsage = recordsInDay.sumOf { it.consumedTokens },
                        date = d
                    )
                }
            }
            StatisticsTimeRange.YEARLY -> {
                (1..12).map { month ->
                    val recordsInMonth = records.filter {
                        val dt = java.time.Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        dt.monthValue == month
                    }
                    val label = startDate.withMonth(month).format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                    ChartDataPoint(
                        label = label,
                        analysisCount = recordsInMonth.size,
                        tokenUsage = recordsInMonth.sumOf { it.consumedTokens },
                        date = startDate.withMonth(month)
                    )
                }
            }
            StatisticsTimeRange.ALL_TIME -> {
                emptyList()
            }
        }
    }
    
    private suspend fun buildHeatmapData(startMillis: Long, endMillis: Long): Map<LocalDate, Int> {
        val counts = analysisDao.getDailyStudyCounts(startMillis, endMillis)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val map = mutableMapOf<LocalDate, Int>()
        counts.forEach { daily ->
            try {
                map[LocalDate.parse(daily.date, formatter)] = daily.count
            } catch (e: Exception) {
                // ignore parsing error
            }
        }
        return map
    }
}
