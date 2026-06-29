package com.example.japanesegrammarapp.ui

import android.app.Application
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.ConflictStrategy
import com.example.japanesegrammarapp.domain.model.DailyTokenUsage
import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.model.ExportFormat
import com.example.japanesegrammarapp.domain.model.HistoryExportPreview
import com.example.japanesegrammarapp.domain.model.ImportResult
import com.example.japanesegrammarapp.domain.model.ModelTokenUsage
import com.example.japanesegrammarapp.domain.model.WordSegment
import com.example.japanesegrammarapp.domain.repository.BookmarkRepository
import com.example.japanesegrammarapp.domain.repository.DetailedResultSerializer
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import com.example.japanesegrammarapp.domain.repository.TtsRepository
import com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkViewModelFilterTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun filtersAreIndependentAcrossTabs() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.setSearchQuery(BookmarkTab.GRAMMAR, "N2")
        viewModel.setArchiveFilter(BookmarkTab.SENTENCES, ArchiveFilter.ARCHIVED)
        advanceUntilIdle()

        assertEquals("", viewModel.wordFilterState.value.searchQuery)
        assertEquals(ArchiveFilter.ALL, viewModel.wordFilterState.value.archiveFilter)
        assertEquals("N2", viewModel.grammarFilterState.value.searchQuery)
        assertEquals(ArchiveFilter.ARCHIVED, viewModel.sentenceFilterState.value.archiveFilter)
    }

    private fun getUtcMidnightOfLocalDate(localTimeMillis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = localTimeMillis
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH)
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)

        val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        utcCal.clear()
        utcCal.set(year, month, day)
        return utcCal.timeInMillis
    }

    @Test
    fun grammarDateCategoriesOnlyUseGrammarDataAndFilterByDate() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val oldDate = getUtcMidnightOfLocalDate(date("2024/05/01"))

        val categories = viewModel.grammarDateCategories.first { it.contains(oldDate) }
        val expectedDates = setOf(
            oldDate,
            getUtcMidnightOfLocalDate(date("2024/06/01")),
            getUtcMidnightOfLocalDate(System.currentTimeMillis())
        )
        assertEquals(expectedDates, categories)

        viewModel.setFilterMode(BookmarkTab.GRAMMAR, BookmarkFilter.BY_DATE)
        viewModel.setDateFilter(BookmarkTab.GRAMMAR, oldDate)
        advanceUntilIdle()

        assertEquals(
            listOf("N2 grammar"),
            viewModel.filteredGrammarPoints.first { it.isNotEmpty() }.map { it.pattern }
        )
    }

    @Test
    fun dateFiltersMatchExactDate() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val todayMidnight = getUtcMidnightOfLocalDate(System.currentTimeMillis())

        viewModel.setFilterMode(BookmarkTab.GRAMMAR, BookmarkFilter.BY_DATE)
        viewModel.setDateFilter(BookmarkTab.GRAMMAR, todayMidnight)
        advanceUntilIdle()
        assertEquals(
            listOf("today grammar"),
            viewModel.filteredGrammarPoints.first { it.isNotEmpty() }.map { it.pattern }
        )
    }

    private fun date(value: String): Long =
        SimpleDateFormat("yyyy/MM/dd", Locale.US).parse(value)!!.time

    @Test
    fun sortingAppliesToAllBookmarkTypes() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.setSortOrder(BookmarkTab.WORDS, BookmarkSortOrder.OLDEST_FIRST)
        viewModel.setSortOrder(BookmarkTab.SENTENCES, BookmarkSortOrder.OLDEST_FIRST)
        viewModel.setSortOrder(BookmarkTab.GRAMMAR, BookmarkSortOrder.OLDEST_FIRST)
        advanceUntilIdle()

        assertEquals(
            listOf("old word", "new word"),
            viewModel.filteredBookmarks.first { it.isNotEmpty() }.map { it.segmentText }
        )
        assertEquals(
            listOf("old sentence", "new sentence"),
            viewModel.filteredSentences.first { it.isNotEmpty() }.map { it.originalText }
        )
        assertEquals(
            listOf("N2 grammar", "N1 grammar", "today grammar"),
            viewModel.filteredGrammarPoints.first { it.isNotEmpty() }.map { it.pattern }
        )

        viewModel.setSortOrder(BookmarkTab.WORDS, BookmarkSortOrder.NEWEST_FIRST)
        advanceUntilIdle()

        assertEquals(
            listOf("new word", "old word"),
            viewModel.filteredBookmarks.first { it.firstOrNull()?.segmentText == "new word" }.map { it.segmentText }
        )
    }

    private fun createViewModel(): BookmarkViewModel {
        return BookmarkViewModel(
            application = Application(),
            bookmarkRepository = FakeBookmarkRepository(),
            ttsRepository = FakeTtsRepository(),
            historyRepository = FakeHistoryRepository(),
            detailedResultSerializer = FakeDetailedResultSerializer(),
            uiPreferencesRepository = FakeUiPreferencesRepository()
        )
    }

    private class FakeBookmarkRepository : BookmarkRepository {
        private val old = date("2024/05/01")
        private val newer = date("2024/06/01")
        private val today = System.currentTimeMillis()

        override val allBookmarks: Flow<List<BookmarkedSegmentDomain>> = MutableStateFlow(
            listOf(
                BookmarkedSegmentDomain(id = 1, recordId = 1, segmentText = "old word", bookmarkedAt = old),
                BookmarkedSegmentDomain(id = 2, recordId = 1, segmentText = "new word", bookmarkedAt = newer)
            )
        )
        override val allBookmarkedSentences: Flow<List<BookmarkedSentenceDomain>> = MutableStateFlow(
            listOf(
                BookmarkedSentenceDomain(id = 1, recordId = 1, originalText = "old sentence", translation = null, analysisResult = null, modelUsed = null, bookmarkedAt = old),
                BookmarkedSentenceDomain(id = 2, recordId = 2, originalText = "new sentence", translation = null, analysisResult = null, modelUsed = null, bookmarkedAt = newer, isArchived = true)
            )
        )
        private val grammar = MutableStateFlow(
            listOf(
                BookmarkedGrammarPointDomain(id = 1, recordId = 1, pattern = "N2 grammar", bookmarkedAt = old),
                BookmarkedGrammarPointDomain(id = 2, recordId = 2, pattern = "N1 grammar", bookmarkedAt = newer),
                BookmarkedGrammarPointDomain(id = 3, recordId = 3, pattern = "today grammar", bookmarkedAt = today)
            )
        )

        override fun bookmarkedTextsForRecord(recordId: Int): Flow<Set<String>> = MutableStateFlow(emptySet())
        override suspend fun toggleBookmark(segment: WordSegment, recordId: Int, sourceText: String): Boolean? = null
        override suspend fun removeBookmarkById(id: Int) = Unit
        override suspend fun updateWordBookmark(domain: BookmarkedSegmentDomain) = Unit
        override suspend fun exportData(format: ExportFormat, includeWords: Boolean, includeSentences: Boolean, includeGrammarPoints: Boolean): String = ""
        override suspend fun checkConflicts(data: String, format: ExportFormat, includeWords: Boolean, includeSentences: Boolean, includeGrammarPoints: Boolean): Boolean = false
        override suspend fun importData(data: String, format: ExportFormat, includeWords: Boolean, includeSentences: Boolean, includeGrammarPoints: Boolean, conflictStrategy: ConflictStrategy): ImportResult = ImportResult(0, 0, 0, emptyList())
        override suspend fun updateArchivedStatus(id: Int, isArchived: Boolean) = Unit
        override suspend fun archiveMultiple(ids: List<Int>) = Unit
        override fun isSentenceBookmarked(recordId: Int): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun toggleSentenceBookmark(record: AnalysisDomainRecord): Boolean = false
        override suspend fun deleteSentenceBookmark(id: Int) = Unit
        override suspend fun deleteSentenceBookmarkByRecordId(recordId: Int) = Unit
        override suspend fun setSentenceArchivedStatus(id: Int, isArchived: Boolean) = Unit
        override suspend fun detachSentenceBookmarkFromRecord(recordId: Int) = Unit
        override fun getAllGrammarPoints(): Flow<List<BookmarkedGrammarPointDomain>> = grammar
        override fun getGrammarPointsForRecord(recordId: Int): Flow<List<BookmarkedGrammarPointDomain>> = MutableStateFlow(emptyList())
        override suspend fun toggleGrammarPointBookmark(recordId: Int, pattern: String, explanation: String?, sourceText: String): Boolean = false
        override suspend fun deleteGrammarPointById(id: Int) = Unit
        override suspend fun setGrammarPointArchivedStatus(id: Int, isArchived: Boolean) = Unit
        override suspend fun archiveMultipleGrammarPoints(ids: List<Int>) = Unit

        private fun date(value: String): Long =
            SimpleDateFormat("yyyy/MM/dd", Locale.US).parse(value)!!.time
    }

    private companion object {
        fun currentMonth(): String =
            SimpleDateFormat("yyyy/MM", Locale.US).format(Date())
    }

    private class FakeTtsRepository : TtsRepository {
        override val isPlaying = MutableStateFlow(false)
        override fun playText(text: String) = Unit
        override fun stop() = Unit
    }

    private class FakeDetailedResultSerializer : DetailedResultSerializer {
        override fun toJson(result: DetailedAnalysisResult): String = ""
        override fun fromJson(json: String?): DetailedAnalysisResult? = null
    }

    private class FakeUiPreferencesRepository : UiPreferencesRepository {
        override fun getFloatingActionBallX(defaultValue: Float): Float = defaultValue
        override fun getFloatingActionBallY(defaultValue: Float): Float = defaultValue
        override fun saveFloatingActionBallPosition(x: Float, y: Float) = Unit
        override fun getFloatingActionBallMode(defaultValue: String): String = defaultValue
        override fun saveFloatingActionBallMode(mode: String) = Unit
        override fun getLastDictionary(defaultValue: String): String = defaultValue
        override fun saveLastDictionary(dictionary: String) = Unit
        override fun getCropInteraction(defaultValue: String): String = defaultValue
        override fun saveCropInteraction(mode: String) = Unit
    }

    private class FakeHistoryRepository : HistoryRepository {
        override suspend fun getAllRecordsList(): List<AnalysisDomainRecord> = emptyList()
        override suspend fun getAllExportPreviews(): List<HistoryExportPreview> = emptyList()
        override suspend fun getRecordsByIds(ids: List<Int>): List<AnalysisDomainRecord> = emptyList()
        override suspend fun getRecordById(id: Int): AnalysisDomainRecord? = null
        override fun observeRecordById(id: Int): Flow<AnalysisDomainRecord?> = MutableStateFlow(null)
        override suspend fun getRecordByOriginalText(originalText: String): AnalysisDomainRecord? = null
        override suspend fun getNewerRecord(timestamp: Long): AnalysisDomainRecord? = null
        override suspend fun getOlderRecord(timestamp: Long): AnalysisDomainRecord? = null
        override suspend fun insertRecord(record: AnalysisDomainRecord): Long = 0L
        override suspend fun updateRecord(record: AnalysisDomainRecord) = Unit
        override suspend fun deleteRecord(record: AnalysisDomainRecord) = Unit
        override suspend fun markRecordAsRead(id: Int) = Unit
        override val totalTokensConsumed: Flow<Int?> = MutableStateFlow(0)
        override val tokenUsageByModel: Flow<List<ModelTokenUsage>> = MutableStateFlow(emptyList())
        override val dailyTokenUsage: Flow<List<DailyTokenUsage>> = MutableStateFlow(emptyList())
    }
}
