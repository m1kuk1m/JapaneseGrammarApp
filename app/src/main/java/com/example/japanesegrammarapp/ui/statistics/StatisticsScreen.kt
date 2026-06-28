package com.example.japanesegrammarapp.ui.statistics

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.StatisticsTimeRange
import com.example.japanesegrammarapp.ui.theme.ZenColors
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val washiBg = ZenThemeColors.washiBg()
    val sumiInk = ZenThemeColors.sumiInk()
    val cardBg = ZenThemeColors.cardBg()

    Scaffold(
        containerColor = washiBg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title), color = sumiInk, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = sumiInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
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
                color = cardBg,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimeRangeSegmentedControl(
                        selectedTimeRange = uiState.timeRange,
                        onTimeRangeSelected = { viewModel.setTimeRange(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeNavigator(
                        uiState = uiState,
                        onNavigatePrevious = { viewModel.navigatePrevious() },
                        onNavigateNext = { viewModel.navigateNext() }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZenColors.AizomeIndigo)
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            } else {
                uiState.summary?.let { summary ->
                    val isDaily = uiState.timeRange == StatisticsTimeRange.DAILY
                    
                    AnimatedContent(
                        targetState = uiState.timeRange,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "TimeRangeTransition"
                    ) { targetTimeRange ->
                        val displaySentences = if (targetTimeRange == StatisticsTimeRange.DAILY) summary.analyzedSentences else summary.analyzedSentences.take(5)
                        val displayBookmarks = if (targetTimeRange == StatisticsTimeRange.DAILY) summary.bookmarkedVocabulary else summary.bookmarkedVocabulary.take(5)

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                SummaryCards(summary = summary)
                            }

                            item {
                                if (targetTimeRange == StatisticsTimeRange.YEARLY) {
                                    HeatmapSection(
                                        heatmapData = summary.heatmapData,
                                        yearDate = uiState.referenceDate
                                    )
                                } else {
                                    ChartSection(
                                        chartData = summary.chartData,
                                        timeRange = targetTimeRange
                                    )
                                }
                            }
                            
                            if (displaySentences.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = if (targetTimeRange == StatisticsTimeRange.DAILY) stringResource(R.string.statistics_section_analyzed_sentences) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.statistics_section_analyzed_sentences),
                                        icon = Icons.Default.Analytics
                                    )
                                }
                                items(displaySentences) { record ->
                                    ActivityCard {
                                        Text(text = record.originalText, style = MaterialTheme.typography.bodyLarge, color = sumiInk)
                                    }
                                }
                            }

                            if (displayBookmarks.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = if (targetTimeRange == StatisticsTimeRange.DAILY) stringResource(R.string.statistics_section_bookmarked_vocabulary) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.statistics_section_bookmarked_vocabulary),
                                        icon = Icons.Default.Bookmark
                                    )
                                }
                                items(displayBookmarks) { bookmark ->
                                    ActivityCard {
                                        Text(text = bookmark.segmentText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = sumiInk)
                                        if (!bookmark.reading.isNullOrEmpty()) {
                                            Text(text = bookmark.reading, style = MaterialTheme.typography.bodyMedium, color = sumiInk.copy(alpha = 0.7f))
                                        }
                                        if (!bookmark.meaning.isNullOrEmpty()) {
                                            Text(text = bookmark.meaning, style = MaterialTheme.typography.bodyMedium, color = sumiInk.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                            
                            if (targetTimeRange != StatisticsTimeRange.DAILY && (summary.analyzedSentences.size > 5 || summary.bookmarkedVocabulary.size > 5)) {
                                item {
                                    OutlinedButton(
                                        onClick = onNavigateBack,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = sumiInk)
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
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Icon(icon, contentDescription = null, tint = ZenColors.AizomeIndigo, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ZenThemeColors.sumiInk()
        )
    }
}

@Composable
fun ActivityCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenThemeColors.cardBg()),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun TimeRangeSegmentedControl(
    selectedTimeRange: StatisticsTimeRange,
    onTimeRangeSelected: (StatisticsTimeRange) -> Unit
) {
    val items = listOf(
        StatisticsTimeRange.DAILY to R.string.statistics_tab_daily,
        StatisticsTimeRange.WEEKLY to R.string.statistics_tab_weekly,
        StatisticsTimeRange.MONTHLY to R.string.statistics_tab_monthly,
        StatisticsTimeRange.YEARLY to R.string.statistics_tab_yearly
    )

    Surface(
        shape = CircleShape,
        color = ZenThemeColors.pillBg(),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            items.forEach { (range, stringRes) ->
                val isSelected = selectedTimeRange == range
                val backgroundColor = if (isSelected) ZenThemeColors.cardBg() else Color.Transparent
                val textColor = if (isSelected) ZenThemeColors.sumiInk() else ZenThemeColors.sumiInk().copy(alpha = 0.6f)
                val elevation = if (isSelected) 2.dp else 0.dp

                Surface(
                    shape = CircleShape,
                    color = backgroundColor,
                    shadowElevation = elevation,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable { onTimeRangeSelected(range) }
                ) {
                    Text(
                        text = stringResource(stringRes),
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun TimeNavigator(
    uiState: StatisticsUiState,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit
) {
    val sumiInk = ZenThemeColors.sumiInk()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigatePrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = sumiInk)
        }

        val dateText = when (uiState.timeRange) {
            StatisticsTimeRange.DAILY -> uiState.referenceDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            StatisticsTimeRange.WEEKLY -> {
                val dayOfWeek = uiState.referenceDate.dayOfWeek.value
                val start = uiState.referenceDate.minusDays(dayOfWeek - 1L)
                val end = start.plusDays(6)
                val formatter = DateTimeFormatter.ofPattern("MMM dd")
                "${start.format(formatter)} - ${end.format(formatter)}"
            }
            StatisticsTimeRange.MONTHLY -> uiState.referenceDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            StatisticsTimeRange.YEARLY -> uiState.referenceDate.year.toString()
        }

        Text(text = dateText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = sumiInk)

        IconButton(onClick = onNavigateNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = sumiInk)
        }
    }
}

@Composable
fun SummaryCards(summary: com.example.japanesegrammarapp.domain.StatisticsSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.statistics_total_analyses),
            value = summary.totalAnalyses.toString(),
            icon = Icons.Default.Analytics,
            tint = ZenColors.AizomeIndigo
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.statistics_total_tokens),
            value = summary.totalTokens.toString(),
            icon = Icons.Default.Timeline,
            tint = ZenColors.MatchaGreen
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.statistics_total_bookmarks),
            value = summary.totalBookmarks.toString(),
            icon = Icons.Default.Bookmark,
            tint = ZenColors.KuriAmber
        )
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ZenThemeColors.cardBg()),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ZenThemeColors.sumiInk())
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = ZenThemeColors.sumiInk().copy(alpha = 0.7f), textAlign = TextAlign.Center)
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
    
    val sumiInk = ZenThemeColors.sumiInk()
    val aizomeIndigo = ZenColors.AizomeIndigo

    Card(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        colors = CardDefaults.cardColors(containerColor = ZenThemeColors.cardBg()),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = aizomeIndigo, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.statistics_analysis_count), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = sumiInk)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Chart(
                chart = columnChart(
                    columns = listOf(
                        LineComponent(
                            color = aizomeIndigo.toArgb(),
                            thicknessDp = 12f,
                            shape = com.patrykandpatrick.vico.core.component.shape.Shapes.roundedCornerShape(topLeftPercent = 50, topRightPercent = 50)
                        )
                    )
                ),
                model = chartEntryModel,
                startAxis = rememberStartAxis(
                    label = com.patrykandpatrick.vico.compose.component.textComponent(color = sumiInk.copy(alpha = 0.7f)),
                    axis = null,
                    tick = null,
                    guideline = com.patrykandpatrick.vico.compose.component.lineComponent(color = sumiInk.copy(alpha = 0.1f))
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = bottomAxisFormatter,
                    label = com.patrykandpatrick.vico.compose.component.textComponent(color = sumiInk.copy(alpha = 0.7f)),
                    axis = com.patrykandpatrick.vico.compose.component.lineComponent(color = sumiInk.copy(alpha = 0.1f)),
                    tick = null
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun HeatmapSection(heatmapData: Map<LocalDate, Int>, yearDate: LocalDate) {
    val sumiInk = ZenThemeColors.sumiInk()
    val matchaGreen = ZenColors.MatchaGreen

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenThemeColors.cardBg()),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarViewMonth, contentDescription = null, tint = matchaGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.statistics_heatmap_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = sumiInk)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val startDate = yearDate.withDayOfYear(1)
            val endDate = yearDate.withDayOfYear(yearDate.lengthOfYear())
            val daysCount = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            
            val startDayOfWeek = startDate.dayOfWeek.value
            val weeks = (daysCount + startDayOfWeek - 1) / 7 + 1
            
            val emptyColor = ZenThemeColors.divider()
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(weeks) { weekIndex ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (dayOfWeek in 1..7) {
                            val dayOffset = weekIndex * 7 + dayOfWeek - startDayOfWeek
                            if (dayOffset in 0 until daysCount) {
                                val currentDate = startDate.plusDays(dayOffset.toLong())
                                val count = heatmapData[currentDate] ?: 0
                                val color = when {
                                    count == 0 -> emptyColor
                                    count < 5 -> matchaGreen.copy(alpha = 0.3f)
                                    count < 10 -> matchaGreen.copy(alpha = 0.6f)
                                    count < 20 -> matchaGreen.copy(alpha = 0.8f)
                                    else -> matchaGreen
                                }
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(color, RoundedCornerShape(4.dp))
                                )
                            } else {
                                Box(modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

