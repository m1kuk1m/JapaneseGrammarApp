package com.example.japanesegrammarapp.data.repository

import androidx.paging.PagingSource
import com.example.japanesegrammarapp.data.AnalysisDao
import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.data.BookmarkDao
import com.example.japanesegrammarapp.data.BookmarkedGrammarPoint
import com.example.japanesegrammarapp.data.BookmarkedGrammarPointDao
import com.example.japanesegrammarapp.data.BookmarkedSegment
import com.example.japanesegrammarapp.data.BookmarkedSentence
import com.example.japanesegrammarapp.data.BookmarkedSentenceDao
import com.example.japanesegrammarapp.data.DailyStudyCountEntity
import com.example.japanesegrammarapp.data.DailyTokenUsageEntity
import com.example.japanesegrammarapp.data.HistoryExportPreviewEntity
import com.example.japanesegrammarapp.data.ModelTokenUsageEntity
import com.example.japanesegrammarapp.domain.StatisticsTimeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatisticsRepositoryTest {

    @Test
    fun dailySummaryCountsOnlyCompletedAnalysesAndAllBookmarkTypes() = runTest {
        val day = LocalDate.of(2024, 5, 10)
        val inside = millis(day, hour = 9)
        val laterInside = millis(day, hour = 17)
        val outside = millis(day.minusDays(1), hour = 9)
        val repository = createRepository(
            records = listOf(
                AnalysisRecord(id = 1, originalText = "完成1", imageUri = null, timestamp = inside, modelUsed = "m", status = "COMPLETED", consumedTokens = 10),
                AnalysisRecord(id = 2, originalText = "失败", imageUri = null, timestamp = laterInside, modelUsed = "m", status = "FAILED", consumedTokens = 99),
                AnalysisRecord(id = 3, originalText = "完成2", imageUri = null, timestamp = laterInside, modelUsed = "m", status = "COMPLETED", consumedTokens = 20),
                AnalysisRecord(id = 4, originalText = "昨天", imageUri = null, timestamp = outside, modelUsed = "m", status = "COMPLETED", consumedTokens = 30)
            ),
            sentenceBookmarks = listOf(
                BookmarkedSentence(id = 1, recordId = 1, originalText = "句子收藏", translation = null, analysisResult = null, modelUsed = "m", bookmarkedAt = inside),
                BookmarkedSentence(id = 2, recordId = 4, originalText = "昨天收藏", translation = null, analysisResult = null, modelUsed = "m", bookmarkedAt = outside)
            ),
            wordBookmarks = listOf(
                BookmarkedSegment(id = 1, recordId = 1, segmentText = "単語", bookmarkedAt = inside)
            ),
            grammarBookmarks = listOf(
                BookmarkedGrammarPoint(id = 1, recordId = 1, pattern = "〜ている", bookmarkedAt = inside)
            )
        )

        val summary = repository.getStatisticsSummary(StatisticsTimeRange.DAILY, day)

        assertEquals(2, summary.totalAnalyses)
        assertEquals(30, summary.totalTokens)
        assertEquals(3, summary.totalBookmarks)
        assertEquals(listOf("完成2", "完成1"), summary.analyzedSentences.map { it.originalText })
        assertEquals(listOf("句子收藏"), summary.bookmarkedSentences.map { it.originalText })
        assertEquals(listOf("単語"), summary.bookmarkedVocabulary.map { it.segmentText })
        assertEquals(listOf("〜ている"), summary.bookmarkedGrammar.map { it.pattern })
        assertFalse(summary.analyzedSentences.any { it.originalText == "失败" })
    }

    @Test
    fun yearlyHeatmapUsesRealDailyAnalysisCounts() = runTest {
        val targetDay = LocalDate.of(2024, 5, 10)
        val otherDay = LocalDate.of(2024, 5, 11)
        val repository = createRepository(
            records = listOf(
                AnalysisRecord(id = 1, originalText = "a", imageUri = null, timestamp = millis(targetDay, hour = 8), modelUsed = "m", status = "COMPLETED"),
                AnalysisRecord(id = 2, originalText = "b", imageUri = null, timestamp = millis(targetDay, hour = 20), modelUsed = "m", status = "COMPLETED"),
                AnalysisRecord(id = 3, originalText = "c", imageUri = null, timestamp = millis(otherDay, hour = 8), modelUsed = "m", status = "COMPLETED"),
                AnalysisRecord(id = 4, originalText = "failed", imageUri = null, timestamp = millis(targetDay, hour = 12), modelUsed = "m", status = "FAILED")
            )
        )

        val summary = repository.getStatisticsSummary(StatisticsTimeRange.YEARLY, targetDay)

        assertEquals(2, summary.heatmapData[targetDay])
        assertEquals(1, summary.heatmapData[otherDay])
        assertTrue(summary.heatmapData.values.none { it <= 0 })
    }

    private fun createRepository(
        records: List<AnalysisRecord> = emptyList(),
        sentenceBookmarks: List<BookmarkedSentence> = emptyList(),
        wordBookmarks: List<BookmarkedSegment> = emptyList(),
        grammarBookmarks: List<BookmarkedGrammarPoint> = emptyList()
    ): StatisticsRepository {
        return StatisticsRepository(
            analysisDao = FakeAnalysisDao(records),
            bookmarkDao = FakeBookmarkDao(wordBookmarks),
            sentenceDao = FakeBookmarkedSentenceDao(sentenceBookmarks),
            grammarPointDao = FakeBookmarkedGrammarPointDao(grammarBookmarks)
        )
    }

    private class FakeAnalysisDao(
        private val records: List<AnalysisRecord>
    ) : AnalysisDao {
        override suspend fun insert(record: AnalysisRecord): Long = record.id.toLong()
        override suspend fun update(record: AnalysisRecord) = Unit
        override suspend fun delete(record: AnalysisRecord) = Unit
        override suspend fun markAsRead(recordId: Int) = Unit
        override suspend fun getRecordById(id: Int): AnalysisRecord? = records.find { it.id == id }
        override fun observeRecordById(id: Int): Flow<AnalysisRecord?> = MutableStateFlow(getRecordByIdBlocking(id))
        override suspend fun getRecordByOriginalText(originalText: String): AnalysisRecord? = records.find { it.originalText == originalText }
        override suspend fun getNewerRecord(currentTimestamp: Long): AnalysisRecord? = records.filter { it.timestamp > currentTimestamp }.minByOrNull { it.timestamp }
        override suspend fun getOlderRecord(currentTimestamp: Long): AnalysisRecord? = records.filter { it.timestamp < currentTimestamp }.maxByOrNull { it.timestamp }
        override fun getAllRecords(): PagingSource<Int, AnalysisRecord> = unsupported()
        override fun getNoRecords(): PagingSource<Int, AnalysisRecord> = unsupported()
        override fun searchRecords(pattern: String): PagingSource<Int, AnalysisRecord> = unsupported()
        override suspend fun getAllRecordsList(): List<AnalysisRecord> = records
        override suspend fun getAllExportPreviews(): List<HistoryExportPreviewEntity> = emptyList()
        override suspend fun getRecordsByIds(ids: List<Int>): List<AnalysisRecord> = records.filter { it.id in ids }
        override fun getTotalTokensConsumed(): Flow<Int?> = MutableStateFlow(records.sumOf { it.consumedTokens })
        override fun getTokenUsageByModel(): Flow<List<ModelTokenUsageEntity>> = MutableStateFlow(emptyList())
        override fun getDailyTokenUsage(): Flow<List<DailyTokenUsageEntity>> = MutableStateFlow(emptyList())

        override suspend fun getRecordsByTimeRange(startTime: Long, endTime: Long): List<AnalysisRecord> {
            return records
                .filter { it.timestamp in startTime..endTime && it.status == "COMPLETED" }
                .sortedByDescending { it.timestamp }
        }

        override suspend fun getDistinctStudyDates(): List<String> {
            return dailyCounts(Long.MIN_VALUE, Long.MAX_VALUE).map { it.date }
        }

        override suspend fun getDailyStudyCounts(startTime: Long, endTime: Long): List<DailyStudyCountEntity> {
            return dailyCounts(startTime, endTime)
        }

        private fun dailyCounts(startTime: Long, endTime: Long): List<DailyStudyCountEntity> {
            return records
                .filter { it.timestamp in startTime..endTime && it.status == "COMPLETED" }
                .groupingBy { dateString(it.timestamp) }
                .eachCount()
                .toSortedMap()
                .map { (date, count) -> DailyStudyCountEntity(date, count) }
        }

        private fun getRecordByIdBlocking(id: Int): AnalysisRecord? = records.find { it.id == id }
    }

    private class FakeBookmarkDao(
        private val bookmarks: List<BookmarkedSegment>
    ) : BookmarkDao {
        override suspend fun insert(bookmark: BookmarkedSegment): Long = bookmark.id.toLong()
        override suspend fun insertReplace(bookmark: BookmarkedSegment): Long = bookmark.id.toLong()
        override suspend fun update(bookmark: BookmarkedSegment) = Unit
        override fun getAll(): Flow<List<BookmarkedSegment>> = MutableStateFlow(bookmarks)
        override fun getSegmentTextsForRecord(recordId: Int): Flow<List<String>> = MutableStateFlow(emptyList())
        override fun getByRecordAndDictForm(recordId: Int, dictForm: String): Flow<List<BookmarkedSegment>> = MutableStateFlow(emptyList())
        override fun existsBySurfaceForm(recordId: Int, surfaceForm: String): Flow<Boolean> = MutableStateFlow(false)
        override fun existsByDictForm(recordId: Int, dictForm: String): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun existsByKeyDirect(recordId: Int, surfaceForm: String, dictForm: String): Boolean = false
        override suspend fun deleteByKey(recordId: Int, surfaceForm: String, dictForm: String) = Unit
        override suspend fun deleteForImport(recordId: Int, surfaceForm: String, dictionaryForm: String) = Unit
        override suspend fun deleteByDictForm(recordId: Int, dictForm: String) = Unit
        override suspend fun deleteById(id: Int) = Unit
        override fun getCount(): Flow<Int> = MutableStateFlow(bookmarks.size)
        override suspend fun updateArchivedStatus(id: Int, isArchived: Boolean) = Unit
        override suspend fun existsForImport(recordId: Int, surfaceForm: String, dictionaryForm: String): Boolean = false
        override suspend fun archiveMultiple(ids: List<Int>) = Unit
        override suspend fun toggleBookmark(recordId: Int, surfaceForm: String, dictForm: String, dictReading: String, partOfSpeech: String?, posCategory: String?, meaning: String?, inflection: String?, role: String?, sourceText: String): Boolean = true
        override suspend fun getBookmarksByTimeRange(startTime: Long, endTime: Long): List<BookmarkedSegment> {
            return bookmarks.filter { it.bookmarkedAt in startTime..endTime }.sortedByDescending { it.bookmarkedAt }
        }
    }

    private class FakeBookmarkedSentenceDao(
        private val bookmarks: List<BookmarkedSentence>
    ) : BookmarkedSentenceDao {
        override suspend fun insert(bookmark: BookmarkedSentence): Long = bookmark.id.toLong()
        override fun getAll(): Flow<List<BookmarkedSentence>> = MutableStateFlow(bookmarks)
        override fun getByRecordId(recordId: Int): Flow<BookmarkedSentence?> = MutableStateFlow(bookmarks.find { it.recordId == recordId })
        override fun existsByRecordId(recordId: Int): Flow<Boolean> = MutableStateFlow(bookmarks.any { it.recordId == recordId })
        override suspend fun existsByRecordIdDirect(recordId: Int): Boolean = bookmarks.any { it.recordId == recordId }
        override fun existsByOriginalText(originalText: String): Flow<Boolean> = MutableStateFlow(bookmarks.any { it.originalText == originalText })
        override suspend fun existsByOriginalTextDirect(originalText: String): Boolean = bookmarks.any { it.originalText == originalText }
        override suspend fun deleteByOriginalText(originalText: String) = Unit
        override suspend fun deleteByRecordId(recordId: Int) = Unit
        override suspend fun deleteById(id: Int) = Unit
        override suspend fun updateArchivedStatus(id: Int, isArchived: Boolean) = Unit
        override suspend fun detachFromRecord(recordId: Int) = Unit
        override suspend fun getBookmarksByTimeRange(startTime: Long, endTime: Long): List<BookmarkedSentence> {
            return bookmarks.filter { it.bookmarkedAt in startTime..endTime }.sortedByDescending { it.bookmarkedAt }
        }
        override suspend fun toggleSentenceBookmark(recordId: Int, originalText: String, translation: String?, analysisResult: String?, modelUsed: String): Boolean = true
    }

    private class FakeBookmarkedGrammarPointDao(
        private val bookmarks: List<BookmarkedGrammarPoint>
    ) : BookmarkedGrammarPointDao {
        override suspend fun insert(point: BookmarkedGrammarPoint): Long = point.id.toLong()
        override suspend fun insertReplace(point: BookmarkedGrammarPoint): Long = point.id.toLong()
        override fun getAll(): Flow<List<BookmarkedGrammarPoint>> = MutableStateFlow(bookmarks)
        override fun getGrammarPointsForRecord(recordId: Int): Flow<List<BookmarkedGrammarPoint>> = MutableStateFlow(bookmarks.filter { it.recordId == recordId })
        override suspend fun existsByPatternDirect(recordId: Int, pattern: String): Boolean = false
        override fun existsByPattern(recordId: Int, pattern: String): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun deleteByPattern(recordId: Int, pattern: String) = Unit
        override suspend fun deleteForImport(recordId: Int, pattern: String) = Unit
        override suspend fun deleteById(id: Int) = Unit
        override suspend fun updateArchivedStatus(id: Int, isArchived: Boolean) = Unit
        override suspend fun archiveMultiple(ids: List<Int>) = Unit
        override suspend fun toggleGrammarPointBookmark(recordId: Int, pattern: String, explanation: String?, sourceText: String): Boolean = true
        override suspend fun getBookmarksByTimeRange(startTime: Long, endTime: Long): List<BookmarkedGrammarPoint> {
            return bookmarks.filter { it.bookmarkedAt in startTime..endTime }.sortedByDescending { it.bookmarkedAt }
        }
    }

    private companion object {
        private fun millis(date: LocalDate, hour: Int): Long {
            return date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        private fun dateString(timestamp: Long): String {
            return java.time.Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        }

        private fun unsupported(): Nothing = throw UnsupportedOperationException("Not needed for statistics repository tests")
    }
}
