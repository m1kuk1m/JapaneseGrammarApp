package com.example.japanesegrammarapp.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.*
import com.example.japanesegrammarapp.ui.BookmarkFilter
import com.example.japanesegrammarapp.ui.ArchiveFilter
import com.example.japanesegrammarapp.ui.BookmarkViewModel
import com.example.japanesegrammarapp.domain.model.ConflictStrategy
import com.example.japanesegrammarapp.domain.model.ImportResult
import com.example.japanesegrammarapp.domain.model.ExportFormat
import kotlinx.coroutines.launch

import android.net.Uri
import com.example.japanesegrammarapp.ui.screens.components.BookmarkSelectionDialog


private val GoldColor = Color(0xFFD4A017)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookmarksScreen(
    navController: NavController,
    viewModel: BookmarkViewModel,
    onNavigateToRecord: (recordId: Int, bookmarkId: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    val bookmarks by viewModel.filteredBookmarks.collectAsState()
    val allBookmarks by viewModel.allBookmarks.collectAsState()
    val filteredSentences by viewModel.filteredSentences.collectAsState()
    val bookmarkedSentences by viewModel.bookmarkedSentences.collectAsState()
    val filteredGrammarPoints by viewModel.filteredGrammarPoints.collectAsState()
    val grammarPoints by viewModel.grammarPoints.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val filterMode by viewModel.filterMode.collectAsState()
    val posCategories by viewModel.posCategories.collectAsState()
    val dateCategories by viewModel.dateCategories.collectAsState()
    val selectedPosCategory by viewModel.selectedPosCategory.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val archiveFilter by viewModel.archiveFilter.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 0 && filterMode == BookmarkFilter.BY_POS) {
            viewModel.setFilterMode(BookmarkFilter.ALL)
        }
    }

    // Track which card is in "confirm delete" mode
    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }

    // Track expanded card for detail view
    var expandedId by remember { mutableStateOf<Int?>(null) }

    // Track bookmark for source sentence dialog
    var sourceDialogBookmark by remember { mutableStateOf<BookmarkedSegmentDomain?>(null) }
    var editingBookmark by remember { mutableStateOf<BookmarkedSegmentDomain?>(null) }

    var showFlashcardSettings by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var pendingImportParams by remember { mutableStateOf<ImportParams?>(null) }
    var showImportSummaryDialog by remember { mutableStateOf(false) }
    var importSummaryResult by remember { mutableStateOf<ImportResult?>(null) }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportDialog = true
        }
    }

    val SumiInk = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bookmarks-screen")
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = SumiInk.copy(alpha = 0.88f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                    )
                }
            },
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GoldColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.bookmarks_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (pagerState.currentPage == 0 && allBookmarks.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        val countText = if (bookmarks.size < allBookmarks.size) {
                                            stringResource(
                                                R.string.bookmarks_count_filtered,
                                                bookmarks.size,
                                                allBookmarks.size
                                            )
                                        } else {
                                            stringResource(
                                                R.string.bookmarks_count_all,
                                                allBookmarks.size
                                            )
                                        }
                                        Text(
                                            text = countText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                } else if (pagerState.currentPage == 1 && bookmarkedSentences.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.bookmarks_count_all, bookmarkedSentences.size),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                } else if (pagerState.currentPage == 2 && grammarPoints.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.bookmarks_count_all, grammarPoints.size),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "text/csv", "text/tab-separated-values", "*/*"))
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.import_bookmarks))
                            }
                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.export_bookmarks))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { Divider(color = SumiInk.copy(alpha = 0.08f)) }
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                            text = { Text(stringResource(R.string.tab_words), fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                            text = { Text(stringResource(R.string.tab_sentences), fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = pagerState.currentPage == 2,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                            text = { Text(stringResource(R.string.tab_grammar), fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (pagerState.currentPage == 0 && allBookmarks.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { showFlashcardSettings = true },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        text = { Text(stringResource(R.string.flashcard_title), fontWeight = FontWeight.SemiBold) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { page ->
                if (page == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .bookmarkRightSwipeBack { navController.popBackStack() }
                    ) {
                        if (allBookmarks.isEmpty()) {
                            // Empty state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = SumiInk.copy(alpha = 0.2f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.bookmarks_empty_title),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SumiInk.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = stringResource(R.string.bookmarks_empty_hint),
                                        fontSize = 13.sp,
                                        color = SumiInk.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                BookmarkFilterChipsBar(
                                    filterMode = filterMode,
                                    archiveFilter = archiveFilter,
                                    posCategories = posCategories,
                                    dateCategories = dateCategories,
                                    selectedPosCategory = selectedPosCategory,
                                    selectedDateFilter = selectedDateFilter,
                                    onFilterModeChange = { viewModel.setFilterMode(it) },
                                    onArchiveFilterChange = { viewModel.setArchiveFilter(it) },
                                    onPosCategoryChange = { viewModel.setPosCategory(it) },
                                    onDateFilterChange = { viewModel.setDateFilter(it) },
                                    isDark = isDark
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                                ) {
                                    items(bookmarks, key = { it.id }) { bookmark ->
                                        val isExpanded = expandedId == bookmark.id
                                        BookmarkCard(
                                            bookmark = bookmark,
                                            isExpanded = isExpanded,
                                            isPendingDelete = pendingDeleteId == bookmark.id,
                                            isDark = isDark,
                                            uiPreferencesRepository = viewModel.uiPreferencesRepository,
                                            onClick = {
                                                if (pendingDeleteId == bookmark.id) {
                                                    pendingDeleteId = null
                                                } else {
                                                    expandedId = if (isExpanded) null else bookmark.id
                                                }
                                            },
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                pendingDeleteId = bookmark.id
                                            },
                                            onConfirmDelete = {
                                                viewModel.removeBookmark(bookmark.id)
                                                pendingDeleteId = null
                                                expandedId = null
                                            },
                                            onCancelDelete = {
                                                pendingDeleteId = null
                                            },
                                            onNavigateToSource = {
                                                pendingDeleteId = null
                                                expandedId = null
                                                onNavigateToRecord(bookmark.recordId, -1)
                                            },
                                            onToggleArchive = {
                                                val nextArchived = !bookmark.isArchived
                                                viewModel.toggleArchiveBookmark(bookmark.id, nextArchived)
                                                coroutineScope.launch {
                                                    val msgId = if (nextArchived) R.string.bookmark_archived_toast else R.string.bookmark_restored_toast
                                                    snackbarHostState.showSnackbar(context.getString(msgId))
                                                }
                                            },
                                            onPlayTts = {
                                                viewModel.playTts(bookmark.segmentText)
                                            },
                                            onEdit = {
                                                editingBookmark = bookmark
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (page == 1) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (bookmarkedSentences.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = SumiInk.copy(alpha = 0.2f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.bookmarks_empty_sentences_title),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SumiInk.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = stringResource(R.string.bookmarks_empty_sentences_hint),
                                        fontSize = 13.sp,
                                        color = SumiInk.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                BookmarkFilterChipsBar(
                                    filterMode = filterMode,
                                    archiveFilter = archiveFilter,
                                    posCategories = posCategories,
                                    dateCategories = dateCategories,
                                    selectedPosCategory = selectedPosCategory,
                                    selectedDateFilter = selectedDateFilter,
                                    onFilterModeChange = { viewModel.setFilterMode(it) },
                                    onArchiveFilterChange = { viewModel.setArchiveFilter(it) },
                                    onPosCategoryChange = { viewModel.setPosCategory(it) },
                                    onDateFilterChange = { viewModel.setDateFilter(it) },
                                    isDark = isDark,
                                    showPosFilter = false,
                                    showArchiveFilter = false
                                )
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                                ) {
                                    items(filteredSentences, key = { it.id }) { sentence ->
                                        SentenceBookmarkCard(
                                            sentence = sentence,
                                            onNavigateToDetails = {
                                                onNavigateToRecord(sentence.recordId, sentence.id)
                                            },
                                            onPlayTts = {
                                                viewModel.playSentenceTts(sentence.analysisResult, sentence.originalText)
                                            },
                                            onDelete = {
                                                viewModel.removeSentenceBookmark(sentence.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (page == 2) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (grammarPoints.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = SumiInk.copy(alpha = 0.2f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.bookmarks_empty_sentences_title),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SumiInk.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = stringResource(R.string.bookmarks_empty_sentences_hint),
                                        fontSize = 13.sp,
                                        color = SumiInk.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                BookmarkFilterChipsBar(
                                    filterMode = filterMode,
                                    archiveFilter = archiveFilter,
                                    posCategories = posCategories,
                                    dateCategories = dateCategories,
                                    selectedPosCategory = selectedPosCategory,
                                    selectedDateFilter = selectedDateFilter,
                                    onFilterModeChange = { viewModel.setFilterMode(it) },
                                    onArchiveFilterChange = { viewModel.setArchiveFilter(it) },
                                    onPosCategoryChange = { viewModel.setPosCategory(it) },
                                    onDateFilterChange = { viewModel.setDateFilter(it) },
                                    isDark = isDark,
                                    showPosFilter = false,
                                    showArchiveFilter = true
                                )
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                                ) {
                                    items(filteredGrammarPoints, key = { it.id }) { gp ->
                                        BookmarkGrammarCard(
                                            grammarPoint = gp,
                                            onNavigateToDetails = {
                                                onNavigateToRecord(gp.recordId, -1)
                                            },
                                            onDelete = {
                                                viewModel.removeGrammarPointBookmark(gp.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    sourceDialogBookmark?.let { bm ->
        SourceSentenceDialog(
            bookmark = bm,
            onDismiss = { sourceDialogBookmark = null }
        )
    }

    editingBookmark?.let { bm ->
        EditWordDialog(
            initialDictionaryForm = bm.dictionaryForm ?: "",
            initialReading = bm.reading ?: "",
            initialMeaning = bm.meaning ?: "",
            initialPartOfSpeech = bm.partOfSpeech ?: "",
            onDismiss = { editingBookmark = null },
            onSave = { dictForm, reading, meaning, pos ->
                viewModel.updateWordBookmark(
                    bm.copy(
                        dictionaryForm = dictForm,
                        reading = reading,
                        meaning = meaning,
                        partOfSpeech = pos
                    )
                )
                editingBookmark = null
            }
        )
    }

    if (showFlashcardSettings) {
        PracticeSettingsDialog(
            allBookmarks = allBookmarks,
            posCategories = posCategories,
            onDismiss = { showFlashcardSettings = false },
            onStartPractice = { mode, limit, pos, scope ->
                showFlashcardSettings = false
                navController.navigate("flashcard?mode=$mode&limit=$limit&pos=$pos&scope=$scope")
            }
        )
    }

    if (showExportDialog) {
        BookmarkSelectionDialog(
            titleResId = R.string.export_options_title,
            confirmResId = R.string.export,
            onDismiss = { showExportDialog = false },
            onConfirm = { words, sentences, grammarPoints, format ->
                showExportDialog = false
                viewModel.exportAndShare(context, format, words, sentences, grammarPoints)
            }
        )
    }

    if (showImportDialog) {
        BookmarkSelectionDialog(
            titleResId = R.string.import_options_title,
            confirmResId = R.string.import_history,
            onDismiss = {
                showImportDialog = false
                pendingImportUri = null
            },
            onConfirm = { words, sentences, grammarPoints, format ->
                showImportDialog = false
                val uri = pendingImportUri
                if (uri != null) {
                    coroutineScope.launch {
                        try {
                            val result = viewModel.importFromUri(uri, format, words, sentences, grammarPoints)
                            if (result != null) {
                                importSummaryResult = result
                                showImportSummaryDialog = true
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.import_failed_msg))
                            }
                        } catch (e: Exception) {
                            if (e.message == "CONFLICT") {
                                pendingImportParams = ImportParams(uri, format, words, sentences, grammarPoints)
                                showConflictDialog = true
                            } else {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.import_error_msg, e.localizedMessage ?: "")
                                )
                            }
                        }
                    }
                } else {
                    pendingImportUri = null
                }
            }
        )
    }
    
    if (showConflictDialog) {
        ConflictResolutionDialog(
            onDismiss = {
                showConflictDialog = false
                pendingImportParams = null
                pendingImportUri = null
            },
            onSkip = {
                showConflictDialog = false
                pendingImportParams?.let { params ->
                    coroutineScope.launch {
                        try {
                            val result = viewModel.importFromUri(params.uri, params.format, params.includeWords, params.includeSentences, params.includeGrammarPoints, com.example.japanesegrammarapp.domain.model.ConflictStrategy.SKIP)
                            if (result != null) {
                                importSummaryResult = result
                                showImportSummaryDialog = true
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.import_failed_msg))
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(context.getString(R.string.import_error_msg, e.localizedMessage ?: ""))
                        }
                        pendingImportUri = null
                        pendingImportParams = null
                    }
                }
            },
            onOverwrite = {
                showConflictDialog = false
                pendingImportParams?.let { params ->
                    coroutineScope.launch {
                        try {
                            val result = viewModel.importFromUri(params.uri, params.format, params.includeWords, params.includeSentences, params.includeGrammarPoints, com.example.japanesegrammarapp.domain.model.ConflictStrategy.OVERWRITE)
                            if (result != null) {
                                importSummaryResult = result
                                showImportSummaryDialog = true
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.import_failed_msg))
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(context.getString(R.string.import_error_msg, e.localizedMessage ?: ""))
                        }
                        pendingImportUri = null
                        pendingImportParams = null
                    }
                }
            }
        )
    }

    if (showImportSummaryDialog && importSummaryResult != null) {
        ImportSummaryDialog(
            result = importSummaryResult!!,
            onDismiss = {
                showImportSummaryDialog = false
                importSummaryResult = null
                pendingImportUri = null
            }
        )
    }
}

data class ImportParams(
    val uri: android.net.Uri,
    val format: com.example.japanesegrammarapp.domain.model.ExportFormat,
    val includeWords: Boolean,
    val includeSentences: Boolean,
    val includeGrammarPoints: Boolean
)

@Composable
fun ImportSummaryDialog(
    result: com.example.japanesegrammarapp.domain.model.ImportResult,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text(
                text = androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_title),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_success, result.successCount))
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_skipped, result.skippedCount))
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_failed, result.failedCount))
                if (result.failureReasons.isNotEmpty()) {
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.material3.Text(
                        text = androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_failure_reasons),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    androidx.compose.foundation.lazy.LazyColumn(modifier = androidx.compose.ui.Modifier.heightIn(max = 120.dp)) {
                        items(result.failureReasons.size) { index ->
                            androidx.compose.material3.Text("- ${result.failureReasons[index]}", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.ok))
            }
        }
    )
}


