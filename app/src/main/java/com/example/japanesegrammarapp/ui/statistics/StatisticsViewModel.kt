package com.example.japanesegrammarapp.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesegrammarapp.data.repository.StatisticsRepository
import com.example.japanesegrammarapp.domain.StatisticsSummary
import com.example.japanesegrammarapp.domain.StatisticsTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class StatisticsUiState(
    val timeRange: StatisticsTimeRange = StatisticsTimeRange.DAILY,
    val referenceDate: LocalDate = LocalDate.now(),
    val selectedDetailDate: LocalDate? = null,
    val summary: StatisticsSummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateDirection: Int = 1
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: StatisticsRepository,
    val uiPreferencesRepository: com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository,
    private val bookmarkRepository: com.example.japanesegrammarapp.domain.repository.BookmarkRepository,
    private val ttsRepository: com.example.japanesegrammarapp.domain.repository.TtsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun setTimeRange(timeRange: StatisticsTimeRange) {
        _uiState.update { it.copy(timeRange = timeRange, referenceDate = LocalDate.now(), selectedDetailDate = null) }
        loadStatistics()
    }

    fun navigatePrevious() {
        val current = _uiState.value
        if (current.timeRange == StatisticsTimeRange.ALL_TIME) return
        val newDate = when (current.timeRange) {
            StatisticsTimeRange.DAILY -> current.referenceDate.minusDays(1)
            StatisticsTimeRange.WEEKLY -> current.referenceDate.minusWeeks(1)
            StatisticsTimeRange.MONTHLY -> current.referenceDate.minusMonths(1)
            StatisticsTimeRange.YEARLY -> current.referenceDate.minusYears(1)
            StatisticsTimeRange.ALL_TIME -> current.referenceDate
        }
        _uiState.update { it.copy(referenceDate = newDate, selectedDetailDate = null, navigateDirection = -1) }
        loadStatistics()
    }

    fun navigateNext() {
        val current = _uiState.value
        if (current.timeRange == StatisticsTimeRange.ALL_TIME) return
        val newDate = when (current.timeRange) {
            StatisticsTimeRange.DAILY -> current.referenceDate.plusDays(1)
            StatisticsTimeRange.WEEKLY -> current.referenceDate.plusWeeks(1)
            StatisticsTimeRange.MONTHLY -> current.referenceDate.plusMonths(1)
            StatisticsTimeRange.YEARLY -> current.referenceDate.plusYears(1)
            StatisticsTimeRange.ALL_TIME -> current.referenceDate
        }
        _uiState.update { it.copy(referenceDate = newDate, selectedDetailDate = null, navigateDirection = 1) }
        loadStatistics()
    }
    
    fun resetDate() {
        _uiState.update { it.copy(referenceDate = LocalDate.now(), selectedDetailDate = null) }
        loadStatistics()
    }

    fun setSelectedDetailDate(date: LocalDate?) {
        _uiState.update { it.copy(selectedDetailDate = date) }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val currentState = _uiState.value
                val summary = repository.getStatisticsSummary(currentState.timeRange, currentState.referenceDate)
                _uiState.update { it.copy(summary = summary, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmarkById(id)
            loadStatistics()
        }
    }

    fun removeSentenceBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.deleteSentenceBookmark(id)
            loadStatistics()
        }
    }

    fun removeGrammarPointBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.deleteGrammarPointById(id)
            loadStatistics()
        }
    }

    fun toggleArchiveBookmark(id: Int, isArchived: Boolean) {
        viewModelScope.launch {
            bookmarkRepository.updateArchivedStatus(id, isArchived)
            loadStatistics()
        }
    }

    fun toggleArchiveSentence(id: Int, isArchived: Boolean) {
        viewModelScope.launch {
            bookmarkRepository.setSentenceArchivedStatus(id, isArchived)
            loadStatistics()
        }
    }

    fun toggleArchiveGrammarPoint(id: Int, isArchived: Boolean) {
        viewModelScope.launch {
            bookmarkRepository.setGrammarPointArchivedStatus(id, isArchived)
            loadStatistics()
        }
    }

    fun updateWordBookmark(bookmark: com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain) {
        viewModelScope.launch {
            bookmarkRepository.updateWordBookmark(bookmark)
            loadStatistics()
        }
    }

    fun playTts(text: String) {
        ttsRepository.playText(text)
    }

    fun playSentenceTts(analysisResult: String?, originalText: String) {
        val kana = analysisResult?.let {
            val lines = it.split("\n")
            val kanaLine = lines.find { line -> line.startsWith("Reading:") || line.startsWith("Kana:") || line.startsWith("読み：") }
            kanaLine?.substringAfter(":")?.trim()
        }
        ttsRepository.playText(kana ?: originalText)
    }
}
