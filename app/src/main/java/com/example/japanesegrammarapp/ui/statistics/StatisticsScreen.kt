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
import com.example.japanesegrammarapp.ui.screens.SentenceBookmarkCard
import com.example.japanesegrammarapp.ui.screens.BookmarkCard
import com.example.japanesegrammarapp.ui.screens.BookmarkGrammarCard
import com.example.japanesegrammarapp.ui.screens.EditWordDialog
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
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
import androidx.compose.foundation.border
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.marker.Marker
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.context.DrawContext
import android.graphics.RectF
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToRecord: (Int, Int) -> Unit,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var transitionTarget by remember { mutableStateOf<Triple<StatisticsTimeRange, LocalDate, com.example.japanesegrammarapp.domain.StatisticsSummary>?>(null) }
            
            LaunchedEffect(uiState) {
                val summary = uiState.summary
                if (!uiState.isLoading && summary != null) {
                    transitionTarget = Triple(uiState.timeRange, uiState.referenceDate, summary)
                }
            }

            var pendingDeleteId by remember { mutableStateOf<Int?>(null) }
            var expandedId by remember { mutableStateOf<Int?>(null) }
            var editingBookmark by remember { mutableStateOf<BookmarkedSegmentDomain?>(null) }

            if (uiState.isLoading && transitionTarget == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZenColors.AizomeIndigo)
                }
            } else if (uiState.error != null && transitionTarget == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            } else {
                transitionTarget?.let { (currentTargetTimeRange, currentTargetDate, targetSummary) ->
                    val isDaily = currentTargetTimeRange == StatisticsTimeRange.DAILY
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = transitionTarget!!,
                            transitionSpec = {
                                val (initialTimeRange, initialDate, _) = initialState
                                val (targetTimeRange, targetDate, _) = targetState
                                if (initialTimeRange != targetTimeRange) {
                                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                } else if (initialDate != targetDate) {
                                    val direction = uiState.navigateDirection
                                    slideInHorizontally(
                                        initialOffsetX = { fullWidth -> fullWidth * direction },
                                        animationSpec = tween(300)
                                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                                    slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> -fullWidth * direction },
                                        animationSpec = tween(300)
                                    ) + fadeOut(animationSpec = tween(300))
                                } else {
                                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                }
                            },
                            label = "TimeRangeTransition"
                        ) { (targetTimeRange, _, summary) ->
                            val selectedDate = uiState.selectedDetailDate
                            
                            val allSentences = selectedDate?.let { date ->
                                summary.analyzedSentences.filter { 
                                    java.time.Instant.ofEpochMilli(it.bookmarkedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
                                }
                            } ?: summary.analyzedSentences
                            val displaySentences = allSentences.take(3)

                            val allBookmarks = selectedDate?.let { date ->
                                summary.bookmarkedVocabulary.filter {
                                    java.time.Instant.ofEpochMilli(it.bookmarkedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
                                }
                            } ?: summary.bookmarkedVocabulary
                            val displayBookmarks = allBookmarks.take(3)

                            val allGrammar = selectedDate?.let { date ->
                                summary.bookmarkedGrammar.filter {
                                    java.time.Instant.ofEpochMilli(it.bookmarkedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
                                }
                            } ?: summary.bookmarkedGrammar
                            val displayGrammar = allGrammar.take(3)

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                item {
                                    Column(
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        TimeRangeSegmentedControl(
                                            selectedTimeRange = uiState.timeRange,
                                            onTimeRangeSelected = { viewModel.setTimeRange(it) }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TimeNavigator(
                                            uiState = uiState,
                                            onNavigatePrevious = { viewModel.navigatePrevious() },
                                            onNavigateNext = { viewModel.navigateNext() }
                                        )
                                    }
                                }

                                item {
                                    SummaryCards(summary = summary)
                                }

                                item {
                                    if (targetTimeRange == StatisticsTimeRange.YEARLY) {
                                        HeatmapSection(
                                            heatmapData = summary.heatmapData,
                                            yearDate = uiState.referenceDate,
                                            selectedDetailDate = uiState.selectedDetailDate,
                                            onDateSelected = { viewModel.setSelectedDetailDate(it) }
                                        )
                                    } else {
                                        ChartSection(
                                            chartData = summary.chartData,
                                            timeRange = targetTimeRange,
                                            onDateSelected = { viewModel.setSelectedDetailDate(it) }
                                        )
                                    }
                                }
                                
                                if (allSentences.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            title = if (uiState.selectedDetailDate != null) {
                                                uiState.selectedDetailDate!!.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " + stringResource(R.string.statistics_section_analyzed_sentences)
                                            } else if (targetTimeRange == StatisticsTimeRange.DAILY) stringResource(R.string.statistics_section_analyzed_sentences) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.statistics_section_analyzed_sentences),
                                            icon = Icons.Default.Analytics
                                        )
                                    }
                                    items(displaySentences) { record ->
                                        CompactActivityItem(
                                            text = record.originalText,
                                            secondaryText = record.translation?.takeIf { it.isNotBlank() },
                                            timestamp = record.bookmarkedAt,
                                            onClick = { onNavigateToRecord(record.recordId, record.id) }
                                        )
                                    }
                                    if (allSentences.size > 3) {
                                        item {
                                            ViewAllButton(
                                                text = "在历史记录中查看全部 ${allSentences.size} 个句子 →",
                                                onClick = onNavigateToHistory
                                            )
                                        }
                                    }
                                }

                                if (allBookmarks.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            title = if (uiState.selectedDetailDate != null) {
                                                uiState.selectedDetailDate!!.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " + stringResource(R.string.statistics_section_bookmarked_vocabulary)
                                            } else if (targetTimeRange == StatisticsTimeRange.DAILY) stringResource(R.string.statistics_section_bookmarked_vocabulary) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.statistics_section_bookmarked_vocabulary),
                                            icon = Icons.Default.Bookmark
                                        )
                                    }
                                    items(displayBookmarks) { bookmark ->
                                        CompactActivityItem(
                                            text = bookmark.segmentText,
                                            secondaryText = bookmark.meaning,
                                            timestamp = bookmark.bookmarkedAt,
                                            onClick = { onNavigateToRecord(bookmark.recordId, bookmark.id) }
                                        )
                                    }
                                    if (allBookmarks.size > 3) {
                                        item {
                                            ViewAllButton(
                                                text = "在历史记录中查看全部 ${allBookmarks.size} 个词汇 →",
                                                onClick = onNavigateToHistory
                                            )
                                        }
                                    }
                                }

                                if (allGrammar.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            title = if (uiState.selectedDetailDate != null) {
                                                uiState.selectedDetailDate!!.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " + stringResource(R.string.grammar_label)
                                            } else if (targetTimeRange == StatisticsTimeRange.DAILY) stringResource(R.string.grammar_label) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.grammar_label),
                                            icon = Icons.Default.Lightbulb
                                        )
                                    }
                                    items(displayGrammar) { grammar ->
                                        CompactActivityItem(
                                            text = grammar.pattern,
                                            secondaryText = grammar.explanation,
                                            timestamp = grammar.bookmarkedAt,
                                            onClick = { onNavigateToRecord(grammar.recordId, -1) }
                                        )
                                    }
                                    if (allGrammar.size > 3) {
                                        item {
                                            ViewAllButton(
                                                text = "在历史记录中查看全部 ${allGrammar.size} 个语法 →",
                                                onClick = onNavigateToHistory
                                            )
                                        }
                                    }
                                }
                                
                                if (allSentences.isEmpty() && allBookmarks.isEmpty() && allGrammar.isEmpty()) {
                                    item {
                                        EmptyStateView()
                                    }
                                }
                            }
                        }

                        editingBookmark?.let { bm ->
                            EditWordDialog(
                                initialDictionaryForm = bm.dictionaryForm ?: "",
                                initialReading = bm.reading ?: "",
                                initialMeaning = bm.meaning ?: "",
                                initialPartOfSpeech = bm.partOfSpeech ?: "",
                                onDismiss = { editingBookmark = null },
                                onSave = { dictionaryForm, reading, meaning, partOfSpeech ->
                                    val updatedBookmark = bm.copy(
                                        dictionaryForm = dictionaryForm.takeIf { it.isNotBlank() },
                                        reading = reading.takeIf { it.isNotBlank() },
                                        meaning = meaning.takeIf { it.isNotBlank() },
                                        partOfSpeech = partOfSpeech.takeIf { it.isNotBlank() }
                                    )
                                    viewModel.updateWordBookmark(updatedBookmark)
                                    editingBookmark = null
                                }
                            )
                        }
                    }

                    if (uiState.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                            color = ZenColors.AizomeIndigo,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = ZenThemeColors.sumiInk().copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.statistics_empty_state_title),
            style = MaterialTheme.typography.bodyLarge,
            color = ZenThemeColors.sumiInk().copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
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
fun rememberMarker(onEntrySelected: (Int) -> Unit): Marker {
    val labelBackgroundColor = ZenThemeColors.cardBg().toArgb()
    val sumiInkArgb = ZenThemeColors.sumiInk().toArgb()
    val aizomeIndigoArgb = ZenColors.AizomeIndigo.toArgb()

    val labelBackground = com.patrykandpatrick.vico.core.component.shape.ShapeComponent(
        shape = com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape,
        color = labelBackgroundColor
    ).apply {
        setShadow(radius = 4f, dy = 2f, color = android.graphics.Color.argb(50, 0, 0, 0))
    }
    
    val label = com.patrykandpatrick.vico.core.component.text.TextComponent.Builder().apply {
        color = sumiInkArgb
        background = labelBackground
        padding = com.patrykandpatrick.vico.core.dimensions.MutableDimensions(8f, 4f, 8f, 4f)
    }.build()

    val indicator = com.patrykandpatrick.vico.core.component.shape.ShapeComponent(
        shape = com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape,
        color = aizomeIndigoArgb
    )

    val guideline = com.patrykandpatrick.vico.core.component.shape.LineComponent(
        color = ZenThemeColors.sumiInk().copy(alpha = 0.1f).toArgb(),
        thicknessDp = 2f
    )

    return remember(label, indicator, guideline) {
        object : MarkerComponent(label, indicator, guideline) {
            override fun draw(
                context: DrawContext,
                bounds: RectF,
                markedEntries: List<Marker.EntryModel>,
                chartValuesProvider: com.patrykandpatrick.vico.core.chart.values.ChartValuesProvider
            ) {
                super.draw(context, bounds, markedEntries, chartValuesProvider)
                markedEntries.firstOrNull()?.let {
                    onEntrySelected(it.index)
                }
            }
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = summary.totalAnalyses.toString(),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = ZenColors.AizomeIndigo
        )
        Text(
            text = stringResource(R.string.statistics_total_analyses),
            style = MaterialTheme.typography.titleMedium,
            color = ZenThemeColors.sumiInk().copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryCardItem(
                title = stringResource(R.string.statistics_total_tokens),
                value = summary.totalTokens.toString(),
                icon = Icons.Default.Timeline,
                tint = ZenColors.MatchaGreen
            )
            Divider(
                modifier = Modifier.height(40.dp).width(1.dp).align(Alignment.CenterVertically),
                color = ZenThemeColors.sumiInk().copy(alpha = 0.1f)
            )
            SummaryCardItem(
                title = stringResource(R.string.statistics_total_bookmarks),
                value = summary.totalBookmarks.toString(),
                icon = Icons.Default.Bookmark,
                tint = ZenColors.KuriAmber
            )
        }
    }
}

@Composable
fun SummaryCardItem(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ZenThemeColors.sumiInk())
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = ZenThemeColors.sumiInk().copy(alpha = 0.7f))
    }
}

@Composable
fun ChartSection(
    chartData: List<com.example.japanesegrammarapp.domain.ChartDataPoint>,
    timeRange: StatisticsTimeRange,
    onDateSelected: (LocalDate?) -> Unit = {}
) {
    if (chartData.isEmpty()) return

    val entries = chartData.mapIndexed { index, point ->
        FloatEntry(x = index.toFloat(), y = point.analysisCount.toFloat())
    }
    val chartEntryModel = entryModelOf(entries)

    val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        chartData.getOrNull(value.toInt())?.label ?: ""
    }
    
    val marker = rememberMarker { index ->
        val date = chartData.getOrNull(index)?.date
        onDateSelected(date)
    }

    val sumiInk = ZenThemeColors.sumiInk()
    val aizomeIndigo = ZenColors.AizomeIndigo

    Column(modifier = Modifier.fillMaxWidth().height(260.dp).padding(vertical = 16.dp)) {
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
            marker = marker,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun HeatmapSection(
    heatmapData: Map<LocalDate, Int>, 
    yearDate: LocalDate,
    selectedDetailDate: LocalDate? = null,
    onDateSelected: (LocalDate?) -> Unit = {}
) {
    val sumiInk = ZenThemeColors.sumiInk()
    val matchaGreen = ZenColors.MatchaGreen

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
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
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(weeks) { weekIndex ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (dayOfWeek in 1..7) {
                        val dayOffset = weekIndex * 7 + dayOfWeek - startDayOfWeek
                        if (dayOffset in 0 until daysCount) {
                            val currentDate = startDate.plusDays(dayOffset.toLong())
                            val count = heatmapData[currentDate] ?: 0
                            val color = when {
                                count == 0 -> emptyColor
                                count < 3 -> matchaGreen.copy(alpha = 0.25f)
                                count < 7 -> matchaGreen.copy(alpha = 0.5f)
                                count < 15 -> matchaGreen.copy(alpha = 0.75f)
                                else -> matchaGreen
                            }
                            val animatedColor by animateColorAsState(
                                targetValue = color,
                                animationSpec = tween(500),
                                label = "HeatmapColor"
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(animatedColor, RoundedCornerShape(4.dp))
                                    .let {
                                        if (selectedDetailDate == currentDate) {
                                            it.border(2.dp, ZenColors.AizomeIndigo, RoundedCornerShape(4.dp))
                                        } else it
                                    }
                                    .clickable {
                                        onDateSelected(if (selectedDetailDate == currentDate) null else currentDate)
                                    }
                            )
                        } else {
                            Box(modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactActivityItem(
    text: String,
    secondaryText: String?,
    timestamp: Long,
    onClick: () -> Unit
) {
    val date = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ZenColors.AizomeIndigo.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ZenThemeColors.sumiInk(),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!secondaryText.isNullOrBlank()) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenThemeColors.sumiInk().copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = date.format(formatter),
                style = MaterialTheme.typography.labelSmall,
                color = ZenThemeColors.sumiInk().copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun ViewAllButton(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = ZenColors.AizomeIndigo,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        textAlign = TextAlign.Center
    )
}

