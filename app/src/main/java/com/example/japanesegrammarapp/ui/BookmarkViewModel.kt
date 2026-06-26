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

    private val _filterMode = MutableStateFlow(BookmarkFilter.ALL)
    val filterMode: StateFlow<BookmarkFilter> = _filterMode.asStateFlow()

    /** Currently selected POS category filter — only used when filterMode == BY_POS */
    private val _selectedPosCategory = MutableStateFlow<String?>(null)
    val selectedPosCategory: StateFlow<String?> = _selectedPosCategory.asStateFlow()

    /** Currently selected date sub-filter — only used when filterMode == BY_DATE */
    private val _selectedDateFilter = MutableStateFlow<String?>(null)
    val selectedDateFilter: StateFlow<String?> = _selectedDateFilter.asStateFlow()

    /** Available POS categories derived from all bookmarks */
    val posCategories: StateFlow<List<String>> = allBookmarks
        .map { bookmarks ->
            bookmarks.map { it.effectivePosCategory }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Available date categories derived from all bookmarks */
    val dateCategories: StateFlow<List<String>> = allBookmarks
        .map { bookmarks ->
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
            
            bookmarks.forEach {
                when {
                    it.bookmarkedAt >= todayStart -> categories.add("today")
                    it.bookmarkedAt >= weekStart -> categories.add("week")
                    else -> categories.add(sdf.format(Date(it.bookmarkedAt)))
                }
            }
            
            val sorted = mutableListOf<String>()
            if (categories.contains("today")) sorted.add("today")
            if (categories.contains("week")) sorted.add("week")
            val dates = categories.filter { it != "today" && it != "week" }.sortedDescending()
            sorted.addAll(dates)
            
            sorted
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPlayingTts: StateFlow<Boolean> = ttsRepository.isPlaying

    private val _archiveFilter = MutableStateFlow(ArchiveFilter.ALL)
    val archiveFilter: StateFlow<ArchiveFilter> = _archiveFilter.asStateFlow()

    fun setArchiveFilter(filter: ArchiveFilter) {
        _archiveFilter.value = filter
    }

    /** Filtered and sorted bookmarks based on current filter mode and archive state */
    val filteredBookmarks: StateFlow<List<BookmarkedSegmentDomain>> = combine(
        allBookmarks,
        _filterMode,
        _selectedPosCategory,
        _selectedDateFilter,
        _archiveFilter
    ) { bookmarks, mode, posCat, dateFilter, archiveF ->
        val archiveFilteredList = when (archiveF) {
            ArchiveFilter.UNARCHIVED -> bookmarks.filter { !it.isArchived }
            ArchiveFilter.ARCHIVED -> bookmarks.filter { it.isArchived }
            ArchiveFilter.ALL -> bookmarks
        }

        when (mode) {
            BookmarkFilter.ALL -> archiveFilteredList
            BookmarkFilter.BY_POS -> archiveFilteredList.filter { posCat == null || it.effectivePosCategory == posCat }
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

                when (dateFilter) {
                    null -> archiveFilteredList.sortedByDescending { it.bookmarkedAt }
                    else -> (grouped[dateFilter] ?: emptyList()).sortedByDescending { it.bookmarkedAt }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilterMode(mode: BookmarkFilter) {
        _filterMode.value = mode
        _selectedPosCategory.value = null
        _selectedDateFilter.value = null
    }

    fun setPosCategory(category: String?) {
        _selectedPosCategory.value = category
    }

    fun setDateFilter(filter: String?) {
        _selectedDateFilter.value = filter
    }

    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmarkById(id)
        }
    }

    fun toggleArchiveBookmark(id: Int, isArchived: Boolean) {
        viewModelScope.launch {
            bookmarkRepository.updateArchivedStatus(id, isArchived)
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
