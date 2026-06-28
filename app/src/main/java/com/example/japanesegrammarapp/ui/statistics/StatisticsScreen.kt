package com.example.japanesegrammarapp.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.StatisticsTimeRange
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    TimeRangeTabs(
                        selectedTimeRange = uiState.timeRange,
                        onTimeRangeSelected = { viewModel.setTimeRange(it) }
                    )
                    TimeNavigator(
                        uiState = uiState,
                        onNavigatePrevious = { viewModel.navigatePrevious() },
                        onNavigateNext = { viewModel.navigateNext() }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            } else {
                uiState.summary?.let { summary ->
                    val isDaily = uiState.timeRange == StatisticsTimeRange.DAILY
                    val displaySentences = if (isDaily) summary.analyzedSentences else summary.analyzedSentences.take(5)
                    val displayBookmarks = if (isDaily) summary.bookmarkedVocabulary else summary.bookmarkedVocabulary.take(5)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            SummaryCards(summary = summary)
                        }

                        item {
                            if (uiState.timeRange == StatisticsTimeRange.YEARLY) {
                                HeatmapSection(
                                    heatmapData = summary.heatmapData,
                                    yearDate = uiState.referenceDate
                                )
                            } else {
                                ChartSection(
                                    chartData = summary.chartData,
                                    timeRange = uiState.timeRange
                                )
                            }
                        }
                        
                        if (displaySentences.isNotEmpty()) {
                            item {
                                Text(
                                    text = if (isDaily) stringResource(R.string.statistics_section_analyzed_sentences) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.statistics_section_analyzed_sentences),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(displaySentences) { record ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = record.originalText, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }

                        if (displayBookmarks.isNotEmpty()) {
                            item {
                                Text(
                                    text = if (isDaily) stringResource(R.string.statistics_section_bookmarked_vocabulary) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.statistics_section_bookmarked_vocabulary),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            items(displayBookmarks) { bookmark ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = bookmark.segmentText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text(text = bookmark.reading ?: "", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = bookmark.meaning ?: "", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                        
                        if (!isDaily && (summary.analyzedSentences.size > 5 || summary.bookmarkedVocabulary.size > 5)) {
                            item {
                                OutlinedButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(stringResource(R.string.statistics_view_full_history))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeRangeTabs(
    selectedTimeRange: StatisticsTimeRange,
    onTimeRangeSelected: (StatisticsTimeRange) -> Unit
) {
    TabRow(selectedTabIndex = selectedTimeRange.ordinal) {
        Tab(
            selected = selectedTimeRange == StatisticsTimeRange.DAILY,
            onClick = { onTimeRangeSelected(StatisticsTimeRange.DAILY) },
            text = { Text(stringResource(R.string.statistics_tab_daily)) }
        )
        Tab(
            selected = selectedTimeRange == StatisticsTimeRange.WEEKLY,
            onClick = { onTimeRangeSelected(StatisticsTimeRange.WEEKLY) },
            text = { Text(stringResource(R.string.statistics_tab_weekly)) }
        )
        Tab(
            selected = selectedTimeRange == StatisticsTimeRange.MONTHLY,
            onClick = { onTimeRangeSelected(StatisticsTimeRange.MONTHLY) },
            text = { Text(stringResource(R.string.statistics_tab_monthly)) }
        )
        Tab(
            selected = selectedTimeRange == StatisticsTimeRange.YEARLY,
            onClick = { onTimeRangeSelected(StatisticsTimeRange.YEARLY) },
            text = { Text(stringResource(R.string.statistics_tab_yearly)) }
        )
    }
}

@Composable
fun TimeNavigator(
    uiState: StatisticsUiState,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigatePrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
        }

        val dateText = when (uiState.timeRange) {
            StatisticsTimeRange.DAILY -> uiState.referenceDate.toString()
            StatisticsTimeRange.WEEKLY -> {
                val dayOfWeek = uiState.referenceDate.dayOfWeek.value
                val start = uiState.referenceDate.minusDays(dayOfWeek - 1L)
                val end = start.plusDays(6)
                "${start} - ${end}"
            }
            StatisticsTimeRange.MONTHLY -> uiState.referenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            StatisticsTimeRange.YEARLY -> uiState.referenceDate.year.toString()
        }

        Text(text = dateText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        IconButton(onClick = onNavigateNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
        }
    }
}

@Composable
fun SummaryCards(summary: com.example.japanesegrammarapp.domain.StatisticsSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.statistics_total_analyses),
            value = summary.totalAnalyses.toString()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.statistics_total_tokens),
            value = summary.totalTokens.toString()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.statistics_total_bookmarks),
            value = summary.totalBookmarks.toString()
        )
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChartSection(
    chartData: List<com.example.japanesegrammarapp.domain.ChartDataPoint>,
    timeRange: StatisticsTimeRange
) {
    if (chartData.isEmpty()) return

    val entries = chartData.mapIndexed { index, point ->
        FloatEntry(x = index.toFloat(), y = point.analysisCount.toFloat())
    }
    val chartEntryModel = entryModelOf(entries)

    val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        chartData.getOrNull(value.toInt())?.label ?: ""
    }

    Card(modifier = Modifier.fillMaxWidth().height(250.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.statistics_analysis_count), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Chart(
                chart = columnChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun HeatmapSection(heatmapData: Map<LocalDate, Int>, yearDate: LocalDate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.statistics_heatmap_title), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            val startDate = yearDate.withDayOfYear(1)
            val endDate = yearDate.withDayOfYear(yearDate.lengthOfYear())
            val daysCount = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            
            val startDayOfWeek = startDate.dayOfWeek.value
            val weeks = (daysCount + startDayOfWeek - 1) / 7 + 1
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(weeks) { weekIndex ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (dayOfWeek in 1..7) {
                            val dayOffset = weekIndex * 7 + dayOfWeek - startDayOfWeek
                            if (dayOffset in 0 until daysCount) {
                                val currentDate = startDate.plusDays(dayOffset.toLong())
                                val count = heatmapData[currentDate] ?: 0
                                val color = when {
                                    count == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                    count < 5 -> MaterialTheme.colorScheme.primaryContainer
                                    count < 10 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(color, RoundedCornerShape(2.dp))
                                )
                            } else {
                                Box(modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

