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
            TimeRangeTabs(
                selectedTimeRange = uiState.timeRange,
                onTimeRangeSelected = { viewModel.setTimeRange(it) }
            )

            TimeNavigator(
                uiState = uiState,
                onNavigatePrevious = { viewModel.navigatePrevious() },
                onNavigateNext = { viewModel.navigateNext() }
            )

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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            SummaryCards(summary = summary)
                        }

                        item {
                            ChartSection(
                                chartData = summary.chartData,
                                timeRange = uiState.timeRange
                            )
                        }
                        
                        if (summary.analyzedSentences.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.statistics_section_analyzed_sentences),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(summary.analyzedSentences) { record ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = record.originalText, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }

                        if (summary.bookmarkedVocabulary.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.statistics_section_bookmarked_vocabulary),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            items(summary.bookmarkedVocabulary) { bookmark ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = bookmark.segmentText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text(text = bookmark.reading ?: "", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = bookmark.meaning ?: "", style = MaterialTheme.typography.bodyMedium)
                                    }
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
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            Text(text = "Analysis Count", style = MaterialTheme.typography.titleSmall)
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
