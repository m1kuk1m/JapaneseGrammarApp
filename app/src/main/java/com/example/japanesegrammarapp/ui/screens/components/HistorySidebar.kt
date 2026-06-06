package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Upload
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.ui.theme.ZenColors.AizomeIndigo
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.MatchaGreen
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors
import androidx.compose.material.icons.filled.Delete
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import java.util.*
import com.example.japanesegrammarapp.ui.HistoryUiRecord
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor

@Composable
fun HistorySidebar(
    historyList: LazyPagingItems<HistoryUiRecord>,
    searchQuery: String,
    selectedRecord: AnalysisDomainRecord?,
    bookmarkedSentenceIds: Set<Int>,
    onSearchQueryChange: (String) -> Unit,
    onSelectRecord: (AnalysisDomainRecord) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteRecord: (AnalysisDomainRecord) -> Unit,
    onExportAll: () -> Unit,
    onExportRecord: (AnalysisDomainRecord) -> Unit,
    onCloseDrawer: () -> Unit,
    onImportHistory: (String) -> Unit,
    onToggleBookmarkSentence: (AnalysisDomainRecord) -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val WashiBg = MaterialTheme.colorScheme.background
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val firstRecordId = historyList.itemSnapshotList.items.firstOrNull()?.record?.id
    var previousFirstRecordId by remember { mutableStateOf<Int?>(null) }
    var shouldStickToTop by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }.collect { isAtTop ->
            shouldStickToTop = isAtTop
        }
    }

    LaunchedEffect(firstRecordId) {
        val oldFirstRecordId = previousFirstRecordId
        val visibleTopRecordId = historyList.itemSnapshotList.items
            .getOrNull(listState.firstVisibleItemIndex)
            ?.record
            ?.id
        val isAnchoredToOldTop = oldFirstRecordId != null && visibleTopRecordId == oldFirstRecordId
        if (oldFirstRecordId != null && firstRecordId != null && firstRecordId != oldFirstRecordId && (shouldStickToTop || isAnchoredToOldTop)) {
            listState.scrollToItem(0)
        }
        previousFirstRecordId = firstRecordId
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: ""
                    if (text.isNotBlank()) {
                        onImportHistory(text)
                    }
                } catch (e: Exception) {
                    // Failures are handled internally or logged
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(WashiBg)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SumiInk
            )
            IconButton(onClick = onCloseDrawer) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = SumiInk)
            }
        }
        
        Divider(color = SumiInk.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(modifier = Modifier.height(8.dp))

        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        val focusRequester = remember { FocusRequester() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .height(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isFocused) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                    } else {
                        SumiInk.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SumiInk.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = SumiInk
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        interactionSource = interactionSource
                    )

                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.history_search_hint),
                            fontSize = 13.sp,
                            color = SumiInk.copy(alpha = 0.45f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_history_search),
                            tint = SumiInk.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // New Analysis Button (Full-width)
        Button(
            onClick = {
                onClearSelection()
                onCloseDrawer()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .height(44.dp),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.new_analysis), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // Secondary Actions Row: Import & Export All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Import History Button
            Button(
                onClick = {
                    filePickerLauncher.launch("text/plain")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZenThemeColors.buttonBg(),
                    contentColor = SumiInk
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = stringResource(R.string.import_history), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.import_history), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (historyList.itemCount > 0) {
                // Export All Button
                Button(
                    onClick = onExportAll,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZenThemeColors.buttonBg(),
                        contentColor = SumiInk
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = stringResource(R.string.export_all), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.export_all), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Drawer History List
        if (historyList.itemCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(if (searchQuery.isBlank()) R.string.no_history else R.string.no_search_results),
                    color = SumiInk.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = historyList.itemCount,
                    key = historyList.itemKey { it.record.id },
                    contentType = historyList.itemContentType { "history_record_item" }
                ) { index ->
                    val uiRecord = historyList[index]
                    if (uiRecord != null) {
                        val record = uiRecord.record
                        val isSelected = selectedRecord?.id == record.id
                        val onSelect = remember(record.id) { {
                            onSelectRecord(record)
                            onCloseDrawer()
                        } }
                        val onDelete = remember(record.id) { { onDeleteRecord(record) } }
                        val onExport = remember(record.id) { { onExportRecord(record) } }
                        val onToggleBookmark = remember(record.id) { { onToggleBookmarkSentence(record) } }
    
                        val isBookmarked = bookmarkedSentenceIds.contains(record.id)
                        HistorySidebarItem(
                            uiRecord = uiRecord,
                            isSelected = isSelected,
                            isBookmarked = isBookmarked,
                            onClick = onSelect,
                            onDeleteClick = onDelete,
                            onExportClick = onExport,
                            onLongClick = onToggleBookmark
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistorySidebarItem(
    uiRecord: HistoryUiRecord,
    isSelected: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val record = uiRecord.record
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val PrimaryColor = MaterialTheme.colorScheme.primary
    val goldColor = Color(0xFFD4A017)
    val haptic = LocalHapticFeedback.current

    val ItemTextColor = SumiInk
    val ItemSubTextColor = SumiInk.copy(alpha = 0.5f)
    val ItemIconColor = SumiInk.copy(alpha = 0.4f)
    val ItemPrimaryIconColor = PrimaryColor.copy(alpha = 0.7f)

    // Star pulse animation when bookmark state changes
    var wasBookmarked by remember { mutableStateOf(isBookmarked) }
    var starScale by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    LaunchedEffect(isBookmarked) {
        if (isBookmarked && !wasBookmarked) {
            val pulse = androidx.compose.animation.core.Animatable(1f)
            pulse.animateTo(1.3f, animationSpec = tween(120, easing = FastOutSlowInEasing))
            pulse.animateTo(1.0f, animationSpec = tween(150, easing = FastOutSlowInEasing))
            starScale = 1f
        }
        wasBookmarked = isBookmarked
    }

    // Brief glow animation on bookmark
    var showGlowAnimation by remember { mutableStateOf(false) }
    val glow = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(isBookmarked) {
        if (isBookmarked && showGlowAnimation) {
            glow.snapTo(0.4f)
            glow.animateTo(0f, animationSpec = tween(600, easing = androidx.compose.animation.core.EaseInOutCubic))
            showGlowAnimation = false
        }
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> SumiInk
            isBookmarked -> goldColor
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "itemBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 1.5.dp else if (isBookmarked) 2.dp else 0.dp,
        label = "itemBorderWidth"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!isBookmarked) {
                            showGlowAnimation = true
                        }
                        onLongClick()
                    }
                ),
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) ZenThemeColors.selectedHistoryBg() else SurfaceColor,
            border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null,
            shadowElevation = if (isSelected) 0.dp else 0.5.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = record.originalText,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = ItemTextColor,
                    modifier = Modifier.padding(end = if (isBookmarked) 12.dp else 0.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Line 1: Status badge & Info (left-aligned)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when (record.status) {
                                    AnalysisStatus.PENDING -> KuriAmber
                                    AnalysisStatus.FAILED -> Color(0xFFD32F2F)
                                    else -> MatchaGreen
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    val statusText = when (record.status) {
                        AnalysisStatus.PENDING -> stringResource(R.string.history_status_pending)
                        AnalysisStatus.FAILED -> stringResource(R.string.history_status_error)
                        else -> uiRecord.modelText
                    }
                    val tokenText = if (uiRecord.formattedTokens != null) "  " + stringResource(R.string.history_record_tokens, uiRecord.formattedTokens) else ""
                    
                    Text(
                        text = "$statusText$tokenText",
                        fontSize = 11.sp,
                        color = ItemSubTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Line 2: Date & Action Buttons (right-aligned)
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = uiRecord.dateStr,
                        fontSize = 11.sp,
                        color = ItemSubTextColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onExportClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.export),
                            tint = ItemPrimaryIconColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = ItemIconColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Star badge with pulse animation when bookmarked
        if (isBookmarked) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = goldColor,
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                    }
            )
        }

        if (isBookmarked && glow.value > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(goldColor.copy(alpha = glow.value))
            )
        }
    }
}
