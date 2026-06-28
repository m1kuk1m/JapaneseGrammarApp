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
    val uiPreferencesRepository: com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
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
        val newDate = when (current.timeRange) {
            StatisticsTimeRange.DAILY -> current.referenceDate.minusDays(1)
            StatisticsTimeRange.WEEKLY -> current.referenceDate.minusWeeks(1)
            StatisticsTimeRange.MONTHLY -> current.referenceDate.minusMonths(1)
            StatisticsTimeRange.YEARLY -> current.referenceDate.minusYears(1)
        }
        _uiState.update { it.copy(referenceDate = newDate, selectedDetailDate = null, navigateDirection = -1) }
        loadStatistics()
    }

    fun navigateNext() {
        val current = _uiState.value
        val newDate = when (current.timeRange) {
            StatisticsTimeRange.DAILY -> current.referenceDate.plusDays(1)
            StatisticsTimeRange.WEEKLY -> current.referenceDate.plusWeeks(1)
            StatisticsTimeRange.MONTHLY -> current.referenceDate.plusMonths(1)
            StatisticsTimeRange.YEARLY -> current.referenceDate.plusYears(1)
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
}
