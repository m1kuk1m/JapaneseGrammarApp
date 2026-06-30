package com.example.japanesegrammarapp.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.japanesegrammarapp.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesegrammarapp.domain.model.*
import com.example.japanesegrammarapp.domain.repository.BookmarkRepository
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import com.example.japanesegrammarapp.domain.repository.TtsRepository
import com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class BookmarkFilter {
    ALL,
    BY_POS,
    BY_DATE
}

enum class ArchiveFilter {
    UNARCHIVED,
    ARCHIVED,
    ALL
}

enum class BookmarkSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

enum class BookmarkTab {
    WORDS,
    SENTENCES,
    GRAMMAR
}

data class BookmarkFilterState(
    val mode: BookmarkFilter = BookmarkFilter.ALL,
    val selectedPosCategory: String? = null,
    val selectedDateFilter: Long? = null,
    val archiveFilter: ArchiveFilter = ArchiveFilter.ALL,
    val searchQuery: String = "",
    val sortOrder: BookmarkSortOrder = BookmarkSortOrder.NEWEST_FIRST
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val application: Application,
    private val bookmarkRepository: BookmarkRepository,
    private val ttsRepository: TtsRepository,
    private val historyRepository: HistoryRepository,
    private val detailedResultSerializer: com.example.japanesegrammarapp.domain.repository.DetailedResultSerializer,
    val uiPreferencesRepository: UiPreferencesRepository,
    private val settingsRepository: com.example.japanesegrammarapp.domain.repository.SettingsRepository
) : ViewModel() {

    val cardFontSizeScale: StateFlow<Float> = settingsRepository.cardFontSizeScale
    val cardSpacingScale: StateFlow<Float> = settingsRepository.cardSpacingScale


    fun playSentenceTts(analysisResultJson: String?, originalText: String) {
        val detail = detailedResultSerializer.fromJson(analysisResultJson)
        if (detail != null) {
            val segments = detail.segments
            if (!segments.isNullOrEmpty()) {
                val readingText = segments.joinToString("") { segment ->
                    val text = segment.text ?: ""
                    val isPunctuation = text.matches(Regex("^[、。！？!?，．…\\s]+$")) || (segment.partOfSpeech?.contains("補助記号") == true)
                    
                    if (isPunctuation) {
                        text
                    } else {
                        val reading = segment.reading
                        if (!reading.isNullOrBlank()) reading else text
                    }
                }
                if (readingText.isNotBlank()) {
                    val cleanedReading = readingText
                        .replace("マル", "。")
                        .replace("テン", "、")
                        .replace("まる", "。")
                        .replace("てん", "、")
                    ttsRepository.playText(cleanedReading)
                    return
                }
            }
        }
        if (originalText.isNotBlank()) {
            ttsRepository.playText(originalText)
        }
    }

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val allBookmarks: StateFlow<List<BookmarkedSegmentDomain>> =
        bookmarkRepository.allBookmarks
            .onEach { _isLoaded.value = true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Expose only active (non-archived) bookmarks for flashcard and main list */
    val activeBookmarks: StateFlow<List<BookmarkedSegmentDomain>> = allBookmarks
        .map { list -> list.filter { !it.isArchived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedSentences: StateFlow<List<BookmarkedSentenceDomain>> =
        bookmarkRepository.allBookmarkedSentences
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grammarPoints: StateFlow<List<BookmarkedGrammarPointDomain>> =
        bookmarkRepository.getAllGrammarPoints()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeSentenceBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.deleteSentenceBookmark(id)
        }
    }

    fun removeGrammarPointBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.deleteGrammarPointById(id)
        }
    }

    private val _wordFilterState = MutableStateFlow(BookmarkFilterState())
    private val _sentenceFilterState = MutableStateFlow(BookmarkFilterState())
    private val _grammarFilterState = MutableStateFlow(BookmarkFilterState())

    val wordFilterState: StateFlow<BookmarkFilterState> = _wordFilterState.asStateFlow()
    val sentenceFilterState: StateFlow<BookmarkFilterState> = _sentenceFilterState.asStateFlow()
    val grammarFilterState: StateFlow<BookmarkFilterState> = _grammarFilterState.asStateFlow()

    // Existing callers use these as the word-tab state.
    val filterState: StateFlow<BookmarkFilterState> = wordFilterState
    val filterMode: StateFlow<BookmarkFilter> = wordFilterState
        .map { it.mode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkFilter.ALL)

    /** Currently selected POS category filter — only used when filterMode == BY_POS */
    val selectedPosCategory: StateFlow<String?> = wordFilterState
        .map { it.selectedPosCategory }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Currently selected date sub-filter — only used when filterMode == BY_DATE */
    val selectedDateFilter: StateFlow<Long?> = wordFilterState
        .map { it.selectedDateFilter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Available POS categories derived from all bookmarks */
    val posCategories: StateFlow<List<String>> = allBookmarks
        .map { bookmarks ->
            bookmarks.map { it.effectivePosCategory }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wordDateCategories: StateFlow<Set<Long>> = allBookmarks
        .map { bookmarks -> bookmarkDateCategories(bookmarks.map { it.bookmarkedAt }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val sentenceDateCategories: StateFlow<Set<Long>> = bookmarkedSentences
        .map { sentences -> bookmarkDateCategories(sentences.map { it.bookmarkedAt }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val grammarDateCategories: StateFlow<Set<Long>> = grammarPoints
        .map { points -> bookmarkDateCategories(points.map { it.bookmarkedAt }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Existing callers use these as the word-tab categories.
    val dateCategories: StateFlow<Set<Long>> = wordDateCategories

    val isPlayingTts: StateFlow<Boolean> = ttsRepository.isPlaying

    val archiveFilter: StateFlow<ArchiveFilter> = wordFilterState
        .map { it.archiveFilter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchiveFilter.ALL)
    val searchQuery: StateFlow<String> = wordFilterState
        .map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val sortOrder: StateFlow<BookmarkSortOrder> = wordFilterState
        .map { it.sortOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkSortOrder.NEWEST_FIRST)

    val sentenceFilterMode: StateFlow<BookmarkFilter> = sentenceFilterState
        .map { it.mode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkFilter.ALL)
    val sentenceSelectedDateFilter: StateFlow<Long?> = sentenceFilterState
        .map { it.selectedDateFilter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val sentenceArchiveFilter: StateFlow<ArchiveFilter> = sentenceFilterState
        .map { it.archiveFilter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchiveFilter.ALL)
    val sentenceSearchQuery: StateFlow<String> = sentenceFilterState
        .map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val sentenceSortOrder: StateFlow<BookmarkSortOrder> = sentenceFilterState
        .map { it.sortOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkSortOrder.NEWEST_FIRST)

    val grammarFilterMode: StateFlow<BookmarkFilter> = grammarFilterState
        .map { it.mode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkFilter.ALL)
    val grammarSelectedDateFilter: StateFlow<Long?> = grammarFilterState
        .map { it.selectedDateFilter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val grammarArchiveFilter: StateFlow<ArchiveFilter> = grammarFilterState
        .map { it.archiveFilter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchiveFilter.ALL)
    val grammarSearchQuery: StateFlow<String> = grammarFilterState
        .map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val grammarSortOrder: StateFlow<BookmarkSortOrder> = grammarFilterState
        .map { it.sortOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkSortOrder.NEWEST_FIRST)

    fun setArchiveFilter(filter: ArchiveFilter) {
        setArchiveFilter(BookmarkTab.WORDS, filter)
    }

    /** Filtered and sorted bookmarks based on current filter mode and archive state */
    val filteredBookmarks: StateFlow<List<BookmarkedSegmentDomain>> = combine(
        allBookmarks,
        wordFilterState
    ) { bookmarks, filter ->
        bookmarks
            .filterByArchive(filter.archiveFilter) { isArchived }
            .filterByBookmarkMode(filter) { bookmarkedAt }
            .filter {
                filter.mode != BookmarkFilter.BY_POS ||
                    filter.selectedPosCategory == null ||
                    it.effectivePosCategory == filter.selectedPosCategory
            }
            .filterByQuery(filter.searchQuery) {
                listOf(
                    segmentText,
                    surfaceForm,
                    reading,
                    partOfSpeech,
                    dictionaryForm,
                    dictionaryFormReading,
                    meaning,
                    inflection,
                    role,
                    sourceText
                )
            }
            .sortedBy(filter.sortOrder) { it.bookmarkedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSentences: StateFlow<List<BookmarkedSentenceDomain>> = combine(
        bookmarkedSentences,
        sentenceFilterState
    ) { sentences, filter ->
        sentences
            .filterByArchive(filter.archiveFilter) { isArchived }
            .filterByBookmarkMode(filter.copy(mode = filter.mode.takeUnless { it == BookmarkFilter.BY_POS } ?: BookmarkFilter.ALL)) { bookmarkedAt }
            .filterByQuery(filter.searchQuery) {
                listOf(originalText, translation, modelUsed)
            }
            .sortedBy(filter.sortOrder) { it.bookmarkedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredGrammarPoints: StateFlow<List<BookmarkedGrammarPointDomain>> = combine(
        grammarPoints,
        grammarFilterState
    ) { points, filter ->
        points
            .filterByArchive(filter.archiveFilter) { isArchived }
            .filterByBookmarkMode(filter.copy(mode = filter.mode.takeUnless { it == BookmarkFilter.BY_POS } ?: BookmarkFilter.ALL)) { bookmarkedAt }
            .filterByQuery(filter.searchQuery) {
                listOf(pattern, explanation, sourceText)
            }
            .sortedBy(filter.sortOrder) { it.bookmarkedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilterMode(mode: BookmarkFilter) {
        setFilterMode(BookmarkTab.WORDS, mode)
    }

    fun setPosCategory(category: String?) {
        setPosCategory(BookmarkTab.WORDS, category)
    }

    fun setDateFilter(filter: Long?) {
        setDateFilter(BookmarkTab.WORDS, filter)
    }

    fun setSearchQuery(query: String) {
        setSearchQuery(BookmarkTab.WORDS, query)
    }

    fun setSortOrder(sortOrder: BookmarkSortOrder) {
        setSortOrder(BookmarkTab.WORDS, sortOrder)
    }

    fun setFilterMode(tab: BookmarkTab, mode: BookmarkFilter) {
        updateFilterState(tab) { state ->
            val effectiveMode = if (tab == BookmarkTab.WORDS) mode else mode.takeUnless { it == BookmarkFilter.BY_POS } ?: BookmarkFilter.ALL
            state.copy(
                mode = effectiveMode,
                selectedPosCategory = null,
                selectedDateFilter = null
            )
        }
    }

    fun setPosCategory(tab: BookmarkTab, category: String?) {
        if (tab != BookmarkTab.WORDS) return
        updateFilterState(tab) { it.copy(selectedPosCategory = category) }
    }

    fun setDateFilter(tab: BookmarkTab, filter: Long?) {
        updateFilterState(tab) { it.copy(selectedDateFilter = filter) }
    }

    fun setSearchQuery(tab: BookmarkTab, query: String) {
        updateFilterState(tab) { it.copy(searchQuery = query) }
    }

    fun setSortOrder(tab: BookmarkTab, sortOrder: BookmarkSortOrder) {
        updateFilterState(tab) { it.copy(sortOrder = sortOrder) }
    }

    fun setArchiveFilter(tab: BookmarkTab, filter: ArchiveFilter) {
        updateFilterState(tab) { it.copy(archiveFilter = filter) }
    }

    fun resetFilters(tab: BookmarkTab) {
        updateFilterState(tab) { BookmarkFilterState() }
    }

    private fun updateFilterState(
        tab: BookmarkTab,
        transform: (BookmarkFilterState) -> BookmarkFilterState
    ) {
        when (tab) {
            BookmarkTab.WORDS -> _wordFilterState.update(transform)
            BookmarkTab.SENTENCES -> _sentenceFilterState.update { transform(it).withoutPosMode() }
            BookmarkTab.GRAMMAR -> _grammarFilterState.update { transform(it).withoutPosMode() }
        }
    }

    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmarkById(id)
        }
    }

    fun updateWordBookmark(domain: BookmarkedSegmentDomain) {
        viewModelScope.launch {
            bookmarkRepository.updateWordBookmark(domain)
        }
    }

    fun toggleArchiveBookmark(id: Int, isArchived: Boolean) {
        viewModelScope.launch {
            bookmarkRepository.updateArchivedStatus(id, isArchived)
        }
    }

    fun toggleArchiveSentence(id: Int, isArchived: Boolean) {
        viewModelScope.launch {
            bookmarkRepository.setSentenceArchivedStatus(id, isArchived)
        }
    }

    fun toggleArchiveGrammarPoint(id: Int, isArchived: Boolean) {
        viewModelScope.launch {
            bookmarkRepository.setGrammarPointArchivedStatus(id, isArchived)
        }
    }

    fun archiveMasteredCards(ids: Set<Int>) {
        viewModelScope.launch {
            bookmarkRepository.archiveMultiple(ids.toList())
        }
    }

    fun playTts(text: String) {
        ttsRepository.playText(text)
    }

    fun stopTts() {
        ttsRepository.stop()
    }

    override fun onCleared() {
        ttsRepository.stop()
        super.onCleared()
    }

    /**
     * Export bookmarks to a file and share it via system share sheet.
     */
    fun exportAndShare(context: Context, format: ExportFormat, includeWords: Boolean, includeSentences: Boolean, includeGrammarPoints: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = bookmarkRepository.exportData(format, includeWords, includeSentences, includeGrammarPoints)
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val extension = when (format) {
                    ExportFormat.JSON -> "json"
                    ExportFormat.CSV -> "csv"
                    ExportFormat.ANKI_TSV -> "tsv"
                }
                val mimeType = when (format) {
                    ExportFormat.JSON -> "application/json"
                    ExportFormat.CSV -> "text/csv"
                    ExportFormat.ANKI_TSV -> "text/tab-separated-values"
                }
                val fileName = "bookmarks_${sdf.format(Date())}.$extension"
                val file = File(context.cacheDir, fileName)
                file.writeText(data, Charsets.UTF_8)
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, context.getString(R.string.export_bookmarks))
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                com.example.japanesegrammarapp.utils.AppLogger.e("BOOKMARK", "Failed to export bookmarks", e)
            }
        }
    }

    suspend fun importData(
        data: String,
        format: ExportFormat,
        includeWords: Boolean,
        includeSentences: Boolean,
        includeGrammarPoints: Boolean,
        conflictStrategy: ConflictStrategy = ConflictStrategy.SKIP
    ): ImportResult? {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkRepository.importData(data, format, includeWords, includeSentences, includeGrammarPoints, conflictStrategy)
            } catch (e: Exception) {
                com.example.japanesegrammarapp.utils.AppLogger.e("BOOKMARK", "Failed to import bookmarks", e)
                null
            }
        }
    }

    suspend fun checkConflictsFromUri(
        uri: Uri,
        format: ExportFormat,
        includeWords: Boolean,
        includeSentences: Boolean,
        includeGrammarPoints: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val data = application.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: return@withContext false
                bookmarkRepository.checkConflicts(data, format, includeWords, includeSentences, includeGrammarPoints)
            } catch (e: Exception) {
                com.example.japanesegrammarapp.utils.AppLogger.e("BOOKMARK", "Failed to check conflicts from URI", e)
                false
            }
        }
    }

    suspend fun importFromUri(
        uri: Uri,
        format: ExportFormat,
        includeWords: Boolean,
        includeSentences: Boolean,
        includeGrammarPoints: Boolean,
        conflictStrategy: ConflictStrategy = ConflictStrategy.SKIP
    ): ImportResult? {
        return withContext(Dispatchers.IO) {
            try {
                val data = application.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: return@withContext null
                bookmarkRepository.importData(data, format, includeWords, includeSentences, includeGrammarPoints, conflictStrategy)
            } catch (e: Exception) {
                com.example.japanesegrammarapp.utils.AppLogger.e("BOOKMARK", "Failed to import bookmarks from URI", e)
                null
            }
        }
    }
}

private fun <T, R : Comparable<R>> List<T>.sortedBy(
    sortOrder: BookmarkSortOrder,
    selector: (T) -> R
): List<T> {
    return when (sortOrder) {
        BookmarkSortOrder.NEWEST_FIRST -> sortedByDescending(selector)
        BookmarkSortOrder.OLDEST_FIRST -> sortedBy(selector)
    }
}

private fun BookmarkFilterState.withoutPosMode(): BookmarkFilterState =
    if (mode == BookmarkFilter.BY_POS) {
        copy(mode = BookmarkFilter.ALL, selectedPosCategory = null)
    } else {
        copy(selectedPosCategory = null)
    }

private fun <T> List<T>.filterByArchive(
    archiveFilter: ArchiveFilter,
    isArchived: T.() -> Boolean
): List<T> = when (archiveFilter) {
    ArchiveFilter.UNARCHIVED -> filter { !it.isArchived() }
    ArchiveFilter.ARCHIVED -> filter { it.isArchived() }
    ArchiveFilter.ALL -> this
}

private fun <T> List<T>.filterByBookmarkMode(
    filter: BookmarkFilterState,
    bookmarkedAt: T.() -> Long
): List<T> {
    return when {
        filter.mode == BookmarkFilter.BY_DATE && filter.selectedDateFilter != null -> {
            filter { matchesBookmarkDateFilter(it.bookmarkedAt(), filter.selectedDateFilter) }
        }
        else -> this
    }
}

private fun getUtcMidnightOfLocalDate(localTimeMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = localTimeMillis
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val day = cal.get(Calendar.DAY_OF_MONTH)

    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utcCal.clear()
    utcCal.set(year, month, day)
    return utcCal.timeInMillis
}

private fun bookmarkDateCategories(times: List<Long>): Set<Long> {
    return times.map { getUtcMidnightOfLocalDate(it) }.toSet()
}

private fun matchesBookmarkDateFilter(time: Long, selectedDateFilter: Long?): Boolean {
    if (selectedDateFilter == null) return true
    return getUtcMidnightOfLocalDate(time) == selectedDateFilter
}

private fun <T> List<T>.filterByQuery(
    query: String,
    fields: T.() -> List<String?>
): List<T> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return this
    return filter { item ->
        item.fields().any { value ->
            value?.contains(normalizedQuery, ignoreCase = true) == true
        }
    }
}
