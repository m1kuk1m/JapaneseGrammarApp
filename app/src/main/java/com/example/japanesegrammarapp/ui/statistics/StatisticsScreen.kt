package com.example.japanesegrammarapp.ui.statistics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.testTag
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.effectivePosCategory
import androidx.compose.ui.draw.rotate
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StatisticsModalType { ANALYZED_SENTENCES, BOOKMARKED_SENTENCES, WORDS, GRAMMAR }

private const val StatisticsSheetReturnRestoreDelayMs = 260L

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
            var activeModalType by rememberSaveable { mutableStateOf<StatisticsModalType?>(null) }
            var isNavigatingFromSheet by rememberSaveable { mutableStateOf(false) }
            var shouldRestoreSheetOnResume by rememberSaveable { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val sentencesListState = rememberLazyListState()
            val sentenceBookmarksListState = rememberLazyListState()
            val wordsListState = rememberLazyListState()
            val grammarListState = rememberLazyListState()
            
            LaunchedEffect(uiState) {
                val summary = uiState.summary
                if (!uiState.isLoading && summary != null) {
                    transitionTarget = Triple(uiState.timeRange, uiState.referenceDate, summary)
                }
            }

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
                    Box(modifier = Modifier.fillMaxSize()) {
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

                                    val allSentenceBookmarks = selectedDate?.let { date ->
                                        summary.bookmarkedSentences.filter {
                                            java.time.Instant.ofEpochMilli(it.bookmarkedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
                                        }
                                    } ?: summary.bookmarkedSentences
                                    val displaySentenceBookmarks = allSentenceBookmarks.take(3)

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

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(24.dp)
                                    ) {
                                        SummaryCards(summary = summary)

                                        if (targetTimeRange == StatisticsTimeRange.YEARLY) {
                                            HeatmapSection(
                                                heatmapData = summary.heatmapData,
                                                yearDate = uiState.referenceDate,
                                                selectedDetailDate = uiState.selectedDetailDate,
                                                onDateSelected = { viewModel.setSelectedDetailDate(it) }
                                            )
                                        } else if (targetTimeRange != StatisticsTimeRange.ALL_TIME) {
                                            ChartSection(
                                                chartData = summary.chartData,
                                                timeRange = targetTimeRange,
                                                selectedDetailDate = uiState.selectedDetailDate,
                                                onDateSelected = { viewModel.setSelectedDetailDate(it) }
                                            )
                                        }
                                        
                                        if (allSentences.isNotEmpty()) {
                                            Column {
                                                SectionHeader(
                                                    title = if (uiState.selectedDetailDate != null) {
                                                        uiState.selectedDetailDate!!.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " + stringResource(R.string.statistics_section_analyzed_sentences)
                                                    } else if (targetTimeRange == StatisticsTimeRange.DAILY) stringResource(R.string.statistics_section_analyzed_sentences) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.statistics_section_analyzed_sentences),
                                                    icon = Icons.Default.Analytics
                                                )
                                                displaySentences.forEach { record ->
                                                    CompactActivityItem(
                                                        text = record.originalText,
                                                        secondaryText = record.translation?.takeIf { it.isNotBlank() },
                                                        timestamp = record.bookmarkedAt,
                                                        canNavigate = record.recordId > 0,
                                                        onClick = { if (record.recordId > 0) onNavigateToRecord(record.recordId, record.id) }
                                                    )
                                                }
                                                if (allSentences.size > 3) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    ViewAllButton(
                                                        text = stringResource(R.string.view_all_sentences, allSentences.size),
                                                        onClick = { activeModalType = StatisticsModalType.ANALYZED_SENTENCES }
                                                    )
                                                }
                                            }
                                        }

                                        if (allSentenceBookmarks.isNotEmpty()) {
                                            Column {
                                                SectionHeader(
                                                    title = if (uiState.selectedDetailDate != null) {
                                                        uiState.selectedDetailDate!!.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " + stringResource(R.string.statistics_section_bookmarked_sentences)
                                                    } else if (targetTimeRange == StatisticsTimeRange.DAILY) stringResource(R.string.statistics_section_bookmarked_sentences) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.statistics_section_bookmarked_sentences),
                                                    icon = Icons.Default.Star
                                                )
                                                displaySentenceBookmarks.forEach { sentence ->
                                                    CompactActivityItem(
                                                        text = sentence.originalText,
                                                        secondaryText = sentence.translation?.takeIf { it.isNotBlank() },
                                                        timestamp = sentence.bookmarkedAt,
                                                        canNavigate = sentence.recordId > 0,
                                                        onClick = { if (sentence.recordId > 0) onNavigateToRecord(sentence.recordId, sentence.id) }
                                                    )
                                                }
                                                if (allSentenceBookmarks.size > 3) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    ViewAllButton(
                                                        text = stringResource(R.string.view_all_bookmarked_sentences, allSentenceBookmarks.size),
                                                        onClick = { activeModalType = StatisticsModalType.BOOKMARKED_SENTENCES }
                                                    )
                                                }
                                            }
                                        }

                                        if (allBookmarks.isNotEmpty()) {
                                            Column {
                                                SectionHeader(
                                                    title = if (uiState.selectedDetailDate != null) {
                                                        uiState.selectedDetailDate!!.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " + stringResource(R.string.statistics_section_bookmarked_vocabulary)
                                                    } else if (targetTimeRange == StatisticsTimeRange.DAILY) stringResource(R.string.statistics_section_bookmarked_vocabulary) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.statistics_section_bookmarked_vocabulary),
                                                    icon = Icons.Default.Bookmark
                                                )
                                                displayBookmarks.forEach { bookmark ->
                                                    CompactActivityItem(
                                                        text = bookmark.segmentText,
                                                        secondaryText = bookmark.meaning,
                                                        timestamp = bookmark.bookmarkedAt,
                                                        canNavigate = bookmark.recordId > 0,
                                                        onClick = { if (bookmark.recordId > 0) onNavigateToRecord(bookmark.recordId, bookmark.id) }
                                                    )
                                                }
                                                if (allBookmarks.size > 3) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    ViewAllButton(
                                                        text = stringResource(R.string.view_all_words, allBookmarks.size),
                                                        onClick = { activeModalType = StatisticsModalType.WORDS }
                                                    )
                                                }
                                            }
                                        }

                                        if (allGrammar.isNotEmpty()) {
                                            Column {
                                                SectionHeader(
                                                    title = if (uiState.selectedDetailDate != null) {
                                                        uiState.selectedDetailDate!!.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " + stringResource(R.string.grammar_label)
                                                    } else if (targetTimeRange == StatisticsTimeRange.DAILY) stringResource(R.string.grammar_label) else stringResource(R.string.statistics_recent_activity) + " - " + stringResource(R.string.grammar_label),
                                                    icon = Icons.Default.Lightbulb
                                                )
                                                displayGrammar.forEach { grammar ->
                                                    CompactActivityItem(
                                                        text = grammar.pattern,
                                                        secondaryText = grammar.explanation,
                                                        timestamp = grammar.bookmarkedAt,
                                                        canNavigate = grammar.recordId > 0,
                                                        onClick = { if (grammar.recordId > 0) onNavigateToRecord(grammar.recordId, -1) }
                                                    )
                                                }
                                                if (allGrammar.size > 3) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    ViewAllButton(
                                                        text = stringResource(R.string.view_all_grammar, allGrammar.size),
                                                        onClick = { activeModalType = StatisticsModalType.GRAMMAR }
                                                    )
                                                }
                                            }
                                        }
                                        
                                        if (allSentences.isEmpty() && allSentenceBookmarks.isEmpty() && allBookmarks.isEmpty() && allGrammar.isEmpty()) {
                                            EmptyStateView()
                                        }
                                    }
                                }
                            }
                        }

                        // Modal Bottom Sheet to show all items of a category
                        if (activeModalType != null) {
                            val selectedDate = uiState.selectedDetailDate
                            val allSentences = selectedDate?.let { date ->
                                targetSummary.analyzedSentences.filter { 
                                    java.time.Instant.ofEpochMilli(it.bookmarkedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
                                }
                            } ?: targetSummary.analyzedSentences

                            val allSentenceBookmarks = selectedDate?.let { date ->
                                targetSummary.bookmarkedSentences.filter {
                                    java.time.Instant.ofEpochMilli(it.bookmarkedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
                                }
                            } ?: targetSummary.bookmarkedSentences

                            val allBookmarks = selectedDate?.let { date ->
                                targetSummary.bookmarkedVocabulary.filter {
                                    java.time.Instant.ofEpochMilli(it.bookmarkedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
                                }
                            } ?: targetSummary.bookmarkedVocabulary

                            val allGrammar = selectedDate?.let { date ->
                                targetSummary.bookmarkedGrammar.filter {
                                    java.time.Instant.ofEpochMilli(it.bookmarkedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
                                }
                            } ?: targetSummary.bookmarkedGrammar

                            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                            var restoreSheetJob by remember { mutableStateOf<Job?>(null) }
                            val latestActiveModalType by rememberUpdatedState(activeModalType)
                            val navigateFromSheet: (Int, Int) -> Unit = { recordId, bookmarkId ->
                                if (!isNavigatingFromSheet) {
                                    isNavigatingFromSheet = true
                                    shouldRestoreSheetOnResume = true
                                    coroutineScope.launch {
                                        sheetState.hide()
                                    }
                                    onNavigateToRecord(recordId, bookmarkId)
                                }
                            }
                            
                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    when (event) {
                                        Lifecycle.Event.ON_RESUME -> {
                                            if (shouldRestoreSheetOnResume && latestActiveModalType != null) {
                                                restoreSheetJob?.cancel()
                                                restoreSheetJob = coroutineScope.launch {
                                                    delay(StatisticsSheetReturnRestoreDelayMs)
                                                    if (
                                                        shouldRestoreSheetOnResume &&
                                                        latestActiveModalType != null &&
                                                        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                                                    ) {
                                                        sheetState.show()
                                                        shouldRestoreSheetOnResume = false
                                                        isNavigatingFromSheet = false
                                                    } else if (latestActiveModalType == null) {
                                                        shouldRestoreSheetOnResume = false
                                                        isNavigatingFromSheet = false
                                                    }
                                                }
                                            } else if (shouldRestoreSheetOnResume && latestActiveModalType == null) {
                                                shouldRestoreSheetOnResume = false
                                                isNavigatingFromSheet = false
                                            }
                                        }
                                        Lifecycle.Event.ON_STOP,
                                        Lifecycle.Event.ON_DESTROY -> {
                                            restoreSheetJob?.cancel()
                                            restoreSheetJob = null
                                        }
                                        else -> Unit
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose {
                                    restoreSheetJob?.cancel()
                                    restoreSheetJob = null
                                    lifecycleOwner.lifecycle.removeObserver(observer)
                                }
                            }

                            ModalBottomSheet(
                                onDismissRequest = {
                                    if (!isNavigatingFromSheet) {
                                        activeModalType = null
                                    }
                                },
                                sheetState = sheetState,
                                containerColor = washiBg,
                                dragHandle = { BottomSheetDefaults.DragHandle(color = sumiInk.copy(alpha = 0.4f)) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .testTag("statistics-detail-sheet")
                                        .padding(horizontal = 16.dp)
                                ) {
                                    val title = when (activeModalType) {
                                        StatisticsModalType.ANALYZED_SENTENCES -> stringResource(R.string.all_analyzed_sentences, allSentences.size)
                                        StatisticsModalType.BOOKMARKED_SENTENCES -> stringResource(R.string.all_bookmarked_sentences, allSentenceBookmarks.size)
                                        StatisticsModalType.WORDS -> stringResource(R.string.all_bookmarked_words, allBookmarks.size)
                                        StatisticsModalType.GRAMMAR -> stringResource(R.string.all_learned_grammar, allGrammar.size)
                                        null -> ""
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = sumiInk
                                        )
                                        IconButton(onClick = { activeModalType = null }) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = sumiInk)
                                        }
                                    }
                                    
                                    val listState = when (activeModalType) {
                                        StatisticsModalType.ANALYZED_SENTENCES -> sentencesListState
                                        StatisticsModalType.BOOKMARKED_SENTENCES -> sentenceBookmarksListState
                                        StatisticsModalType.WORDS -> wordsListState
                                        StatisticsModalType.GRAMMAR -> grammarListState
                                        null -> rememberLazyListState()
                                    }

                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 32.dp)
                                    ) {
                                        when (activeModalType) {
                                            StatisticsModalType.ANALYZED_SENTENCES -> {
                                                items(allSentences) { record ->
                                                    DetailedSentenceItem(
                                                        record = record,
                                                        canNavigate = record.recordId > 0,
                                                        modifier = Modifier.testTag("statistics-sheet-analyzed-sentence"),
                                                        navigateButtonModifier = Modifier.testTag("statistics-sheet-analyzed-sentence-navigate"),
                                                        onClickNavigate = {
                                                            if (record.recordId > 0) {
                                                                navigateFromSheet(record.recordId, record.id)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                            StatisticsModalType.BOOKMARKED_SENTENCES -> {
                                                items(allSentenceBookmarks) { sentence ->
                                                    DetailedSentenceItem(
                                                        record = sentence,
                                                        canNavigate = sentence.recordId > 0,
                                                        modifier = Modifier.testTag("statistics-sheet-bookmarked-sentence"),
                                                        navigateButtonModifier = Modifier.testTag("statistics-sheet-bookmarked-sentence-navigate"),
                                                        onClickNavigate = {
                                                            if (sentence.recordId > 0) {
                                                                navigateFromSheet(sentence.recordId, sentence.id)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                            StatisticsModalType.WORDS -> {
                                                items(allBookmarks) { bookmark ->
                                                    ExpandableWordItem(
                                                        bookmark = bookmark,
                                                        canNavigate = bookmark.recordId > 0,
                                                        modifier = Modifier.testTag("statistics-sheet-word"),
                                                        navigateButtonModifier = Modifier.testTag("statistics-sheet-word-navigate"),
                                                        onClickNavigate = {
                                                            if (bookmark.recordId > 0) {
                                                                navigateFromSheet(bookmark.recordId, bookmark.id)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                            StatisticsModalType.GRAMMAR -> {
                                                items(allGrammar) { grammar ->
                                                    DetailedGrammarItem(
                                                        grammar = grammar,
                                                        canNavigate = grammar.recordId > 0,
                                                        modifier = Modifier.testTag("statistics-sheet-grammar"),
                                                        onClick = {
                                                            if (grammar.recordId > 0) {
                                                                navigateFromSheet(grammar.recordId, -1)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                            null -> {}
                                        }
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
        MarkerComponent(label, indicator, guideline)
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
        StatisticsTimeRange.YEARLY to R.string.statistics_tab_yearly,
        StatisticsTimeRange.ALL_TIME to R.string.statistics_tab_all_time
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
    val showArrows = uiState.timeRange != StatisticsTimeRange.ALL_TIME
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showArrows) {
            IconButton(onClick = onNavigatePrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.previous), tint = sumiInk)
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
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
            StatisticsTimeRange.ALL_TIME -> stringResource(R.string.statistics_tab_all_time)
        }

        Text(text = dateText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = sumiInk)

        if (showArrows) {
            IconButton(onClick = onNavigateNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.next), tint = sumiInk)
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
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
    selectedDetailDate: LocalDate? = null,
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
    
    val marker = rememberMarker { }

    val sumiInk = ZenThemeColors.sumiInk()
    val aizomeIndigo = ZenColors.AizomeIndigo

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BarChart, contentDescription = null, tint = aizomeIndigo, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.statistics_analysis_count), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = sumiInk)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
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
        ChartDateSelector(
            chartData = chartData,
            timeRange = timeRange,
            selectedDetailDate = selectedDetailDate,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
fun ChartDateSelector(
    chartData: List<com.example.japanesegrammarapp.domain.ChartDataPoint>,
    timeRange: StatisticsTimeRange,
    selectedDetailDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit
) {
    val selectablePoints = chartData.filter { it.date != null && it.analysisCount > 0 }
    if (selectablePoints.isEmpty()) return

    val formatter = when (timeRange) {
        StatisticsTimeRange.DAILY -> DateTimeFormatter.ofPattern("MMM dd")
        StatisticsTimeRange.WEEKLY -> DateTimeFormatter.ofPattern("EEE")
        StatisticsTimeRange.MONTHLY -> DateTimeFormatter.ofPattern("d")
        StatisticsTimeRange.YEARLY -> DateTimeFormatter.ofPattern("MMM")
        StatisticsTimeRange.ALL_TIME -> DateTimeFormatter.ofPattern("MMM dd")
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(selectablePoints) { point ->
            val date = point.date
            val selected = selectedDetailDate == date
            AssistChip(
                onClick = { onDateSelected(if (selected) null else date) },
                label = {
                    Text(
                        text = "${date!!.format(formatter)} (${point.analysisCount})",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected) ZenColors.AizomeIndigo.copy(alpha = 0.12f) else ZenThemeColors.pillBg(),
                    labelColor = ZenThemeColors.sumiInk(),
                    leadingIconContentColor = ZenColors.AizomeIndigo
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) ZenColors.AizomeIndigo else ZenThemeColors.divider()
                )
            )
        }
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
    canNavigate: Boolean = true,
    onClick: () -> Unit
) {
    val date = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canNavigate, onClick = onClick)
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

@Composable
fun DetailedSentenceItem(
    record: com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain,
    canNavigate: Boolean = true,
    modifier: Modifier = Modifier,
    navigateButtonModifier: Modifier = Modifier,
    onClickNavigate: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val cardBg = ZenThemeColors.cardBg()
    val sumiInk = ZenThemeColors.sumiInk()
    
    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "expandArrow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, sumiInk.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.originalText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = sumiInk,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!record.translation.isNullOrBlank()) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                                tint = sumiInk.copy(alpha = 0.4f),
                                modifier = Modifier.rotate(expandRotation)
                            )
                        }
                    }
                    IconButton(
                        onClick = onClickNavigate,
                        enabled = canNavigate,
                        modifier = navigateButtonModifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.view_record),
                            tint = if (canNavigate) ZenColors.AizomeIndigo else sumiInk.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!record.translation.isNullOrBlank()) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Surface(
                            color = ZenThemeColors.pillBg().copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = record.translation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = sumiInk.copy(alpha = 0.8f),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableWordItem(
    bookmark: com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain,
    canNavigate: Boolean = true,
    modifier: Modifier = Modifier,
    navigateButtonModifier: Modifier = Modifier,
    onClickNavigate: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val cardBg = ZenThemeColors.cardBg()
    val sumiInk = ZenThemeColors.sumiInk()
    
    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "expandArrow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, sumiInk.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = bookmark.segmentText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = sumiInk
                        )
                        if (!bookmark.reading.isNullOrBlank() && bookmark.reading != bookmark.segmentText) {
                            Text(
                                text = bookmark.reading,
                                style = MaterialTheme.typography.bodyMedium,
                                color = sumiInk.copy(alpha = 0.5f)
                            )
                        }
                    }
                    if (!bookmark.partOfSpeech.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val posCat = bookmark.effectivePosCategory
                        val chipBg = ZenThemeColors.getChipColor(posCat)
                        Surface(color = chipBg, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = bookmark.partOfSpeech,
                                style = MaterialTheme.typography.labelSmall,
                                color = sumiInk.copy(alpha = 0.75f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                            tint = sumiInk.copy(alpha = 0.4f),
                            modifier = Modifier.rotate(expandRotation)
                        )
                    }
                    IconButton(
                        onClick = onClickNavigate,
                        enabled = canNavigate,
                        modifier = navigateButtonModifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.view_related_record),
                            tint = if (canNavigate) ZenColors.AizomeIndigo else sumiInk.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    if (!bookmark.meaning.isNullOrBlank()) {
                        Surface(
                            color = ZenColors.KuriAmber.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = bookmark.meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = sumiInk,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!bookmark.dictionaryForm.isNullOrBlank()) {
                            DetailRow(stringResource(R.string.dictionary_form), bookmark.dictionaryForm, sumiInk)
                        }
                        if (!bookmark.inflection.isNullOrBlank()) {
                            DetailRow(stringResource(R.string.inflection), bookmark.inflection, sumiInk)
                        }
                        if (!bookmark.role.isNullOrBlank()) {
                            DetailRow(stringResource(R.string.role_in_sentence), bookmark.role, sumiInk)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedGrammarItem(
    grammar: com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain,
    canNavigate: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardBg = ZenThemeColors.cardBg()
    val sumiInk = ZenThemeColors.sumiInk()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = canNavigate, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, sumiInk.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = grammar.pattern,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = sumiInk
            )
            if (!grammar.explanation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = grammar.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = sumiInk.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.view_related_analysis),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (canNavigate) ZenColors.AizomeIndigo else sumiInk.copy(alpha = 0.35f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (canNavigate) ZenColors.AizomeIndigo else sumiInk.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, sumiInk: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = sumiInk.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = sumiInk,
            fontWeight = FontWeight.Medium
        )
    }
}

