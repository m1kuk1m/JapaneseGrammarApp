package com.example.japanesegrammarapp.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.*
import com.example.japanesegrammarapp.ui.BookmarkFilter
import com.example.japanesegrammarapp.ui.ArchiveFilter
import com.example.japanesegrammarapp.ui.BookmarkViewModel
import com.example.japanesegrammarapp.ui.theme.ZenColors
import kotlinx.coroutines.launch

private val PosColors = mapOf(
    "NOUN"      to Color(0xFFD3E0EA),
    "VERB"      to Color(0xFFD4ECD5),
    "ADJECTIVE" to Color(0xFFF6E2CD),
    "AUXILIARY" to Color(0xFFE8D3EA),
    "PARTICLE"  to Color(0xFFFDD4D8)
)
private val PosColorsDark = mapOf(
    "NOUN"      to Color(0xFF1E2D3D),
    "VERB"      to Color(0xFF1E3D24),
    "ADJECTIVE" to Color(0xFF3D2A1E),
    "AUXILIARY" to Color(0xFF2D1E3D),
    "PARTICLE"  to Color(0xFF3D1E25)
)

private val GoldColor = Color(0xFFD4A017)
private val GoldLight = Color(0xFFFFF3C4)

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
    var selectedTab by rememberSaveable { mutableStateOf(0) }
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

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.readText() ?: return@launch
                    val count = viewModel.importFromJson(json)
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
    }

    val SumiInk = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalDx = 0f
                    var totalDy = 0f
                    var isDecided = false
                    var isRightSwipe = false
                    var event: PointerEvent
                    do {
                        // 浣跨敤 Main 闃舵锛氳瀛愮粍浠讹紙LazyRow 璇嶆€ф粴鍔ㄦ潯绛夛級浼樺厛澶勭悊浜嬩欢
                        event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull()
                        // 鑻ヤ簨浠跺凡琚瓙缁勪欢娑堣垂锛堝妯悜婊氬姩璇嶆€ф潯锛夛紝鍒欒烦杩囧彸婊戝垽瀹?
                        if (change != null && !change.isConsumed) {
                            if (!isDecided) {
                                totalDx += change.positionChange().x
                                totalDy += change.positionChange().y
                                if (kotlin.math.abs(totalDx) > 40f || kotlin.math.abs(totalDy) > 40f) {
                                    isDecided = true
                                    if (totalDx > 0f && kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy) * 1.5f) {
                                        isRightSwipe = true
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (isDecided && isRightSwipe) {
                        navController.popBackStack()
                    }
                }
            }
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
                                if (selectedTab == 0 && allBookmarks.isNotEmpty()) {
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
                                } else if (selectedTab == 1 && bookmarkedSentences.isNotEmpty()) {
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
                            if (selectedTab == 0) {
                                IconButton(onClick = {
                                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                }) {
                                    Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.import_bookmarks))
                                }
                                IconButton(onClick = { viewModel.exportAndShare(context) }) {
                                    Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.export_bookmarks))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { Divider(color = SumiInk.copy(alpha = 0.08f)) }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.tab_words), fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.tab_sentences), fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 0 && allBookmarks.isNotEmpty()) {
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
            if (selectedTab == 0) {
                if (allBookmarks.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // 鈹€鈹€ Filter chips bar 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
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

                        // 鈹€鈹€ Bookmark list 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
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
            } else {
                if (bookmarkedSentences.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
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
                            .padding(horizontal = 16.dp)
                            .padding(paddingValues),
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

    // 鈹€鈹€ Source sentence dialog 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
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
}

// 鈹€鈹€ Source Sentence Dialog 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€


// 鈹€鈹€ Bookmark Card 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun BookmarkCard(
    bookmark: BookmarkedSegmentDomain,
    isExpanded: Boolean,
    isPendingDelete: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onNavigateToSource: () -> Unit,
    onToggleArchive: () -> Unit,
    onPlayTts: () -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface

    val posCat = bookmark.effectivePosCategory
    val chipBg = (if (isDark) PosColorsDark[posCat] else PosColors[posCat])
        ?: if (isDark) Color(0xFF2D2D2D) else Color(0xFFEFEFEF)

    val borderColor by animateColorAsState(
        targetValue = when {
            isPendingDelete -> Color(0xFFD32F2F).copy(alpha = 0.7f)
            isExpanded -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else -> SumiInk.copy(alpha = 0.08f)
        },
        animationSpec = tween(250),
        label = "cardBorder"
    )

    val cardElevation by animateDpAsState(
        targetValue = if (isExpanded) 4.dp else 1.dp,
        label = "cardElevation"
    )

    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "expandArrow"
    )

    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = cardElevation,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 鈹€鈹€ Header row 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Word info
                Column(modifier = Modifier.weight(1f)) {
                    // Dictionary form (main text)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = bookmark.segmentText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk,
                            modifier = Modifier.align(Alignment.Bottom)
                        )
                        
                        val displayReading = bookmark.reading
                        if (!displayReading.isNullOrBlank() && displayReading != bookmark.segmentText) {
                            Text(
                                text = displayReading,
                                fontSize = 12.sp,
                                color = SumiInk.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .align(Alignment.Bottom)
                            )
                        }
                    }
                    // POS badge
                    if (!bookmark.partOfSpeech.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = chipBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = bookmark.partOfSpeech,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SumiInk.copy(alpha = 0.75f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Right-side controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play TTS Button
                    IconButton(
                        onClick = onPlayTts,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = stringResource(R.string.flashcard_speak),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // Expand chevron
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                            tint = SumiInk.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(expandRotation)
                        )
                    }
                    // Navigate to source
                    IconButton(
                        onClick = onNavigateToSource,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.view_source),
                            tint = SumiInk.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 鈹€鈹€ Meaning preview (visible ONLY when expanded) 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
            if (isExpanded && !bookmark.meaning.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = ZenColors.KuriAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bookmark.meaning,
                        fontSize = 13.sp,
                        color = SumiInk,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            // 鈹€鈹€ Expanded detail section 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(10.dp))
                    Divider(color = SumiInk.copy(alpha = 0.08f))
                    Spacer(Modifier.height(10.dp))

                    // Detailed attributes
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailRow(
                            label = stringResource(R.string.dictionary_form),
                            value = bookmark.dictionaryForm
                        )
                        if (!bookmark.dictionaryFormReading.isNullOrBlank() &&
                            bookmark.dictionaryFormReading != bookmark.reading) {
                            DetailRow(
                                label = stringResource(R.string.dict_form_reading),
                                value = bookmark.dictionaryFormReading
                            )
                        }
                        DetailRow(
                            label = stringResource(R.string.inflection),
                            value = bookmark.inflection
                        )
                        DetailRow(
                            label = stringResource(R.string.role_in_sentence),
                            value = bookmark.role
                        )
                    }

                    // Full source sentence
                    if (bookmark.sourceText.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = SumiInk.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.source_sentence_prefix, bookmark.sourceText),
                                fontSize = 12.sp,
                                color = SumiInk.copy(alpha = 0.5f),
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Bookmarked date & Archive Toggle Button Row
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(bookmark.bookmarkedAt))
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = SumiInk.copy(alpha = 0.3f)
                        )

                        TextButton(
                            onClick = onToggleArchive,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (bookmark.isArchived) stringResource(R.string.bookmark_restore) else stringResource(R.string.bookmark_archive),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 鈹€鈹€ Delete confirmation row 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
            AnimatedVisibility(
                visible = isPendingDelete,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.cancel_delete), fontSize = 13.sp)
                    }
                    Button(
                        onClick = onConfirmDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.delete_bookmark), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    val SumiInk = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = SumiInk.copy(alpha = 0.45f),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SumiInk,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SentenceBookmarkCard(
    sentence: BookmarkedSentenceDomain,
    onNavigateToDetails: () -> Unit,
    onPlayTts: () -> Unit,
    onDelete: () -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val borderColor = if (showDeleteConfirm) {
        Color(0xFFD32F2F).copy(alpha = 0.7f)
    } else {
        SumiInk.copy(alpha = 0.08f)
    }

    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Main content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Original text
                    Text(
                        text = sentence.originalText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk,
                        lineHeight = 26.sp
                    )
                    
                    // Translation
                    if (!sentence.translation.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = sentence.translation,
                            fontSize = 14.sp,
                            color = SumiInk.copy(alpha = 0.65f),
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(Modifier.width(8.dp))

                // Actions row on the right or top-right
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayTts,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = stringResource(R.string.flashcard_speak),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToDetails,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.view_details),
                            tint = SumiInk.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.unfavorite),
                            tint = SumiInk.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Timestamp and footer
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(sentence.bookmarkedAt))
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = SumiInk.copy(alpha = 0.3f)
                )
            }

            // Inline delete confirmation
            AnimatedVisibility(
                visible = showDeleteConfirm,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.cancel_delete), fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            onDelete()
                            showDeleteConfirm = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.delete_bookmark), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
