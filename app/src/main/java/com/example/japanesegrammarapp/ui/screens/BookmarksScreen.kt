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
    val bookmarkedSentences by viewModel.bookmarkedSentences.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val filterMode by viewModel.filterMode.collectAsState()
    val posCategories by viewModel.posCategories.collectAsState()
    val selectedPosCategory by viewModel.selectedPosCategory.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val archiveFilter by viewModel.archiveFilter.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Track which card is in "confirm delete" mode
    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }

    // Track expanded card for detail view
    var expandedId by remember { mutableStateOf<Int?>(null) }

    // Track bookmark for source sentence dialog
    var sourceDialogBookmark by remember { mutableStateOf<BookmarkedSegmentDomain?>(null) }

    var showFlashcardSettings by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

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
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
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
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
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
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                        ) {
                            items(bookmarkedSentences, key = { it.id }) { sentence ->
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
        }
    }

    sourceDialogBookmark?.let { bm ->
        SourceSentenceDialog(
            bookmark = bm,
            onDismiss = { sourceDialogBookmark = null }
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
            onConfirm = { words, sentences ->
                showExportDialog = false
                viewModel.exportAndShare(context, words, sentences)
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
            onConfirm = { words, sentences ->
                showImportDialog = false
                val uri = pendingImportUri
                if (uri != null) {
                    coroutineScope.launch {
                        try {
                            val count = viewModel.importFromUri(uri, words, sentences)
                            val msg = if (count >= 0) {
                                context.getString(R.string.import_success_msg, count)
                            } else {
                                context.getString(R.string.import_failed_msg)
                            }
                            snackbarHostState.showSnackbar(msg)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.import_error_msg, e.localizedMessage ?: "")
                            )
                        }
                    }
                }
                pendingImportUri = null
            }
        )
    }
}
