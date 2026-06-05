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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onNavigateToRecord: (recordId: Int) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    val bookmarks by viewModel.filteredBookmarks.collectAsState()
    val allBookmarks by viewModel.allBookmarks.collectAsState()
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
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull()
                        if (change != null) {
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
                            if (isDecided && isRightSwipe) {
                                change.consume()
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
                                fontSize = 18.sp
                            )
                            if (allBookmarks.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.bookmarks_count, allBookmarks.size),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
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
                        IconButton(onClick = { viewModel.exportAndShare(context) }) {
                            Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.export_bookmarks))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                if (allBookmarks.isNotEmpty()) {
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
                    // ── Filter chips bar ────────────────────────────────────────
                    FilterChipsBar(
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

                    // ── Bookmark list ────────────────────────────────────────────
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
                                    onNavigateToRecord(bookmark.recordId)
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
    }

    // ── Source sentence dialog ────────────────────────────────────────
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

// ── Source Sentence Dialog ──────────────────────────────────────────────

@Composable
private fun SourceSentenceDialog(
    bookmark: BookmarkedSegmentDomain,
    onDismiss: () -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bookmark.segmentText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = SumiInk.copy(alpha = 0.5f))
                    }
                }

                if (!bookmark.reading.isNullOrBlank() && bookmark.reading != bookmark.segmentText) {
                    Text(bookmark.reading, fontSize = 14.sp, color = SumiInk.copy(alpha = 0.5f))
                }

                if (!bookmark.meaning.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = ZenColors.KuriAmber.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Text(bookmark.meaning, fontSize = 14.sp, color = SumiInk, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.target_sentence_header), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SumiInk.copy(alpha = 0.5f))
                Spacer(Modifier.height(6.dp))
                Surface(color = SumiInk.copy(alpha = 0.04f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(bookmark.sourceText.ifBlank { "（无原句）" }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SumiInk, lineHeight = 22.sp, modifier = Modifier.padding(14.dp))
                }

                if (!bookmark.partOfSpeech.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text(bookmark.partOfSpeech, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        bookmark.inflection?.let {
                            Text(it, fontSize = 12.sp, color = SumiInk.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.close), fontSize = 14.sp, color = SumiInk)
                }
            }
        }
    }
}

// ── Filter Chips ──────────────────────────────────────────────────────────

@Composable
private fun FilterChipsBar(
    filterMode: BookmarkFilter,
    archiveFilter: ArchiveFilter,
    posCategories: List<String>,
    selectedPosCategory: String?,
    selectedDateFilter: String?,
    onFilterModeChange: (BookmarkFilter) -> Unit,
    onArchiveFilterChange: (ArchiveFilter) -> Unit,
    onPosCategoryChange: (String?) -> Unit,
    onDateFilterChange: (String?) -> Unit,
    isDark: Boolean
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Mode chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChipItem(
                    label = stringResource(R.string.filter_all),
                    isSelected = filterMode == BookmarkFilter.ALL,
                    onClick = { onFilterModeChange(BookmarkFilter.ALL) }
                )
            }
            item {
                FilterChipItem(
                    label = stringResource(R.string.filter_by_pos),
                    isSelected = filterMode == BookmarkFilter.BY_POS,
                    onClick = { onFilterModeChange(BookmarkFilter.BY_POS) }
                )
            }
            item {
                FilterChipItem(
                    label = stringResource(R.string.filter_by_date),
                    isSelected = filterMode == BookmarkFilter.BY_DATE,
                    onClick = { onFilterModeChange(BookmarkFilter.BY_DATE) }
                )
            }
        }

        // Archive Filter row
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChipItem(
                    label = stringResource(R.string.filter_all),
                    isSelected = archiveFilter == ArchiveFilter.ALL,
                    onClick = { onArchiveFilterChange(ArchiveFilter.ALL) }
                )
            }
            item {
                FilterChipItem(
                    label = stringResource(R.string.archive_filter_unarchived),
                    isSelected = archiveFilter == ArchiveFilter.UNARCHIVED,
                    onClick = { onArchiveFilterChange(ArchiveFilter.UNARCHIVED) }
                )
            }
            item {
                FilterChipItem(
                    label = stringResource(R.string.archive_filter_archived),
                    isSelected = archiveFilter == ArchiveFilter.ARCHIVED,
                    onClick = { onArchiveFilterChange(ArchiveFilter.ARCHIVED) }
                )
            }
        }

        // POS sub-filter
        AnimatedVisibility(visible = filterMode == BookmarkFilter.BY_POS && posCategories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                item {
                    FilterChipItem(
                        label = stringResource(R.string.filter_all),
                        isSelected = selectedPosCategory == null,
                        onClick = { onPosCategoryChange(null) }
                    )
                }
                items(posCategories) { cat ->
                    val chipBg = (if (isDark) PosColorsDark[cat] else PosColors[cat])
                        ?: if (isDark) Color(0xFF2D2D2D) else Color(0xFFEFEFEF)
                    @OptIn(ExperimentalMaterial3Api::class)
                    FilterChip(
                        selected = selectedPosCategory == cat,
                        onClick = { onPosCategoryChange(cat) },
                        label = { Text(getPosDisplayName(cat), fontSize = 12.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipBg,
                            containerColor = chipBg.copy(alpha = 0.4f),
                            labelColor = SumiInk.copy(alpha = 0.7f),
                            selectedLabelColor = SumiInk
                        )
                    )
                }
            }
        }

        // Date sub-filter
        AnimatedVisibility(visible = filterMode == BookmarkFilter.BY_DATE) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                item {
                    FilterChipItem(
                        label = stringResource(R.string.filter_all),
                        isSelected = selectedDateFilter == null,
                        onClick = { onDateFilterChange(null) }
                    )
                }
                item {
                    FilterChipItem(
                        label = stringResource(R.string.filter_today),
                        isSelected = selectedDateFilter == "today",
                        onClick = { onDateFilterChange("today") }
                    )
                }
                item {
                    FilterChipItem(
                        label = stringResource(R.string.filter_week),
                        isSelected = selectedDateFilter == "week",
                        onClick = { onDateFilterChange("week") }
                    )
                }
                item {
                    FilterChipItem(
                        label = stringResource(R.string.filter_older),
                        isSelected = selectedDateFilter == "older",
                        onClick = { onDateFilterChange("older") }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SumiInk.copy(alpha = 0.12f)
        )
    )
}

// ── Bookmark Card ──────────────────────────────────────────────────────────

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
            // ── Header row ────────────────────────────────────────────────
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
                            contentDescription = "朗读单词",
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
                            contentDescription = if (isExpanded) "收起" else "展开",
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

            // ── Meaning preview (visible ONLY when expanded) ──────────────────────────
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

            // ── Expanded detail section ────────────────────────────────────
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
                                label = stringResource(R.string.dictionary_form) + " 読み",
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

            // ── Delete confirmation row ────────────────────────────────────
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

// ── POS Display Name Mapping ─────────────────────────────────────────────

private val PosNameKeys = mapOf(
    "NOUN"       to R.string.pos_NOUN,
    "VERB"       to R.string.pos_VERB,
    "ADJECTIVE"  to R.string.pos_ADJECTIVE,
    "AUXILIARY"  to R.string.pos_AUXILIARY,
    "PARTICLE"   to R.string.pos_PARTICLE,
    "ADVERB"     to R.string.pos_ADVERB,
    "CONJUNCTION" to R.string.pos_CONJUNCTION,
    "PRONOUN"    to R.string.pos_PRONOUN,
    "INTERJECTION" to R.string.pos_INTERJECTION
)

@Composable
private fun getPosDisplayName(category: String): String {
    val resId = PosNameKeys[category] ?: R.string.pos_other
    return stringResource(resId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeSettingsDialog(
    allBookmarks: List<BookmarkedSegmentDomain>,
    posCategories: List<String>,
    onDismiss: () -> Unit,
    onStartPractice: (mode: String, limit: Int, pos: String, scope: String) -> Unit
) {
    var studyMode by remember { mutableStateOf("ja_to_zh") }
    var cardLimit by remember { mutableStateOf(-1) }
    var selectedPos by remember { mutableStateOf("ALL") }
    var practiceScope by remember { mutableStateOf("unarchived") } // unarchived, archived, all

    val filteredCount = remember(practiceScope, allBookmarks) {
        when (practiceScope) {
            "archived" -> allBookmarks.count { it.isArchived }
            "all" -> allBookmarks.size
            else -> allBookmarks.count { !it.isArchived }
        }
    }

    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.flashcard_settings_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = SumiInk
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 0. Review Scope
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.flashcard_scope_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "unarchived" to R.string.flashcard_scope_unarchived,
                            "archived" to R.string.flashcard_scope_archived,
                            "all" to R.string.flashcard_scope_all
                        ).forEach { (scopeVal, labelRes) ->
                            val isSel = practiceScope == scopeVal
                            FilterChip(
                                selected = isSel,
                                onClick = { practiceScope = scopeVal },
                                label = { Text(stringResource(labelRes), fontSize = 10.sp, maxLines = 1) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SumiInk,
                                    selectedLabelColor = SurfaceColor,
                                    labelColor = SumiInk.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSel) SumiInk else SumiInk.copy(alpha = 0.12f),
                                    borderWidth = if (isSel) 1.5.dp else 1.dp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 1. Study Mode
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.flashcard_mode_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ja_to_zh" to R.string.flashcard_mode_ja_to_zh,
                            "zh_to_ja" to R.string.flashcard_mode_zh_to_ja
                        ).forEach { (modeVal, labelRes) ->
                            val isSel = studyMode == modeVal
                            FilterChip(
                                selected = isSel,
                                onClick = { studyMode = modeVal },
                                label = { Text(stringResource(labelRes), fontSize = 12.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SumiInk,
                                    selectedLabelColor = SurfaceColor,
                                    labelColor = SumiInk.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSel) SumiInk else SumiInk.copy(alpha = 0.12f),
                                    borderWidth = if (isSel) 1.5.dp else 1.dp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2. Card Limit
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.flashcard_count_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            10 to "10",
                            20 to "20",
                            50 to "50",
                            -1 to stringResource(R.string.flashcard_count_all, filteredCount)
                        ).forEach { (limitVal, label) ->
                            val isSel = cardLimit == limitVal
                            FilterChip(
                                selected = isSel,
                                onClick = { cardLimit = limitVal },
                                label = { Text(label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SumiInk,
                                    selectedLabelColor = SurfaceColor,
                                    labelColor = SumiInk.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSel) SumiInk else SumiInk.copy(alpha = 0.12f),
                                    borderWidth = if (isSel) 1.5.dp else 1.dp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. POS Filter
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.flashcard_pos_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk.copy(alpha = 0.5f)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isSel = selectedPos == "ALL"
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedPos = "ALL" },
                                label = { Text(stringResource(R.string.flashcard_pos_all), fontSize = 12.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SumiInk,
                                    selectedLabelColor = SurfaceColor,
                                    labelColor = SumiInk.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSel) SumiInk else SumiInk.copy(alpha = 0.12f),
                                    borderWidth = if (isSel) 1.5.dp else 1.dp
                                )
                            )
                        }
                        items(posCategories) { cat ->
                            val isSel = selectedPos == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedPos = cat },
                                label = { Text(getPosDisplayName(cat), fontSize = 12.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SumiInk,
                                    selectedLabelColor = SurfaceColor,
                                    labelColor = SumiInk.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSel) SumiInk else SumiInk.copy(alpha = 0.12f),
                                    borderWidth = if (isSel) 1.5.dp else 1.dp
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartPractice(studyMode, cardLimit, selectedPos, practiceScope) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SumiInk,
                    contentColor = SurfaceColor
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.flashcard_start_btn),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = SumiInk.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        },
        containerColor = SurfaceColor,
        tonalElevation = 6.dp
    )
}
