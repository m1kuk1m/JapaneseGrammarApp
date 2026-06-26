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

data class BookmarkFilterState(
    val mode: BookmarkFilter = BookmarkFilter.ALL,
    val selectedPosCategory: String? = null,
    val selectedDateFilter: String? = null,
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
    val uiPreferencesRepository: UiPreferencesRepository
) : ViewModel() {

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

    private val _filterState = MutableStateFlow(BookmarkFilterState())
    val filterState: StateFlow<BookmarkFilterState> = _filterState.asStateFlow()
    val filterMode: StateFlow<BookmarkFilter> = filterState
        .map { it.mode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkFilter.ALL)

    /** Currently selected POS category filter — only used when filterMode == BY_POS */
    val selectedPosCategory: StateFlow<String?> = filterState
        .map { it.selectedPosCategory }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Currently selected date sub-filter — only used when filterMode == BY_DATE */
    val selectedDateFilter: StateFlow<String?> = filterState
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

    /** Available date categories derived from all bookmarks */
    val dateCategories: StateFlow<List<String>> = combine(
        allBookmarks,
        bookmarkedSentences,
        grammarPoints
    ) { bookmarks, sentences, grammar ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekStart = cal.timeInMillis

        val categories = mutableSetOf<String>()
        val sdf = SimpleDateFormat("yyyy/MM", Locale.getDefault())
        
        val allTimes = bookmarks.map { it.bookmarkedAt } + 
                       sentences.map { it.bookmarkedAt } + 
                       grammar.map { it.bookmarkedAt }

        allTimes.forEach { time ->
            when {
                time >= todayStart -> categories.add("today")
                time >= weekStart -> categories.add("week")
                else -> categories.add(sdf.format(Date(time)))
            }
        }
        
        val sorted = mutableListOf<String>()
        if (categories.contains("today")) sorted.add("today")
        if (categories.contains("week")) sorted.add("week")
        val dates = categories.filter { it != "today" && it != "week" }.sortedDescending()
        sorted.addAll(dates)
        
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPlayingTts: StateFlow<Boolean> = ttsRepository.isPlaying

    val archiveFilter: StateFlow<ArchiveFilter> = filterState
        .map { it.archiveFilter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchiveFilter.ALL)
    val searchQuery: StateFlow<String> = filterState
        .map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val sortOrder: StateFlow<BookmarkSortOrder> = filterState
        .map { it.sortOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkSortOrder.NEWEST_FIRST)

    fun setArchiveFilter(filter: ArchiveFilter) {
        _filterState.update { it.copy(archiveFilter = filter) }
    }

    /** Filtered and sorted bookmarks based on current filter mode and archive state */
    val filteredBookmarks: StateFlow<List<BookmarkedSegmentDomain>> = combine(
        allBookmarks,
        filterState
    ) { bookmarks, filter ->
        val archiveFilteredList = when (filter.archiveFilter) {
            ArchiveFilter.UNARCHIVED -> bookmarks.filter { !it.isArchived }
            ArchiveFilter.ARCHIVED -> bookmarks.filter { it.isArchived }
            ArchiveFilter.ALL -> bookmarks
        }

        val filteredByMode = when (filter.mode) {
            BookmarkFilter.ALL -> archiveFilteredList
            BookmarkFilter.BY_POS -> archiveFilteredList.filter { filter.selectedPosCategory == null || it.effectivePosCategory == filter.selectedPosCategory }
            BookmarkFilter.BY_DATE -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val todayStart = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val weekStart = cal.timeInMillis
                val sdf = SimpleDateFormat("yyyy/MM", Locale.getDefault())

                val grouped = archiveFilteredList.groupBy {
                    when {
                        it.bookmarkedAt >= todayStart -> "today"
                        it.bookmarkedAt >= weekStart -> "week"
                        else -> sdf.format(Date(it.bookmarkedAt))
                    }
                }

                when (filter.selectedDateFilter) {
                    null -> archiveFilteredList.sortedBy(filter.sortOrder) { it.bookmarkedAt }
                    else -> (grouped[filter.selectedDateFilter] ?: emptyList()).sortedBy(filter.sortOrder) { it.bookmarkedAt }
                }
            }
        }
        filteredByMode.filterByQuery(filter.searchQuery) {
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSentences: StateFlow<List<BookmarkedSentenceDomain>> = combine(
        bookmarkedSentences,
        filterState
    ) { sentences, filter ->
        val archiveFilteredList = when (filter.archiveFilter) {
            ArchiveFilter.UNARCHIVED -> sentences.filter { !it.isArchived }
            ArchiveFilter.ARCHIVED -> sentences.filter { it.isArchived }
            ArchiveFilter.ALL -> sentences
        }
        if (filter.mode != BookmarkFilter.BY_DATE || filter.selectedDateFilter == null) {
            return@combine archiveFilteredList
                .sortedBy(filter.sortOrder) { it.bookmarkedAt }
                .filterByQuery(filter.searchQuery) {
                    listOf(originalText, translation, modelUsed)
                }
        }

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekStart = cal.timeInMillis
        val sdf = SimpleDateFormat("yyyy/MM", Locale.getDefault())

        val grouped = archiveFilteredList.groupBy {
            when {
                it.bookmarkedAt >= todayStart -> "today"
                it.bookmarkedAt >= weekStart -> "week"
                else -> sdf.format(Date(it.bookmarkedAt))
            }
        }

        (grouped[filter.selectedDateFilter] ?: emptyList())
            .sortedBy(filter.sortOrder) { it.bookmarkedAt }
            .filterByQuery(filter.searchQuery) {
                listOf(originalText, translation, modelUsed)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredGrammarPoints: StateFlow<List<BookmarkedGrammarPointDomain>> = combine(
        grammarPoints,
        filterState
    ) { points, filter ->
        val archiveFilteredList = when (filter.archiveFilter) {
            ArchiveFilter.UNARCHIVED -> points.filter { !it.isArchived }
            ArchiveFilter.ARCHIVED -> points.filter { it.isArchived }
            ArchiveFilter.ALL -> points
        }
        if (filter.mode != BookmarkFilter.BY_DATE || filter.selectedDateFilter == null) {
            return@combine archiveFilteredList
                .sortedBy(filter.sortOrder) { it.bookmarkedAt }
                .filterByQuery(filter.searchQuery) {
                    listOf(pattern, explanation, sourceText)
                }
        }

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekStart = cal.timeInMillis
        val sdf = SimpleDateFormat("yyyy/MM", Locale.getDefault())

        val grouped = archiveFilteredList.groupBy {
            when {
                it.bookmarkedAt >= todayStart -> "today"
                it.bookmarkedAt >= weekStart -> "week"
                else -> sdf.format(Date(it.bookmarkedAt))
            }
        }

        (grouped[filter.selectedDateFilter] ?: emptyList())
            .sortedBy(filter.sortOrder) { it.bookmarkedAt }
            .filterByQuery(filter.searchQuery) {
                listOf(pattern, explanation, sourceText)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilterMode(mode: BookmarkFilter) {
        _filterState.update {
            it.copy(
                mode = mode,
                selectedPosCategory = null,
                selectedDateFilter = null
            )
        }
    }

    fun setPosCategory(category: String?) {
        _filterState.update { it.copy(selectedPosCategory = category) }
    }

    fun setDateFilter(filter: String?) {
        _filterState.update { it.copy(selectedDateFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun setSortOrder(sortOrder: BookmarkSortOrder) {
        _filterState.update { it.copy(sortOrder = sortOrder) }
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
