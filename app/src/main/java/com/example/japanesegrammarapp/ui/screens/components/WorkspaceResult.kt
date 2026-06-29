package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.domain.model.WordSegment
import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.model.dictionaryQueryWord
import com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
import com.example.japanesegrammarapp.ui.WorkspaceUiState
import com.example.japanesegrammarapp.ui.theme.ZenColors.AizomeIndigo
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.MatchaGreen
import com.example.japanesegrammarapp.ui.theme.ZenColors.SakuraPink
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkspaceResultContent(
    uiState: WorkspaceUiState,
    isPlayingTts: Boolean = false,
    onPlayTts: () -> Unit = {},
    onStopTts: () -> Unit = {},
    onToggleBookmark: (WordSegment) -> Unit = {},
    onEditWordSegment: (WordSegment) -> Unit = {},
    onToggleGrammarBookmark: (pattern: String, explanation: String?, sourceText: String) -> Unit = { _, _, _ -> },
    onLoadNewer: () -> Unit = {},
    onLoadOlder: () -> Unit = {},
    uiPreferencesRepository: UiPreferencesRepository,
    onUserInteracted: () -> Unit = {},
    onScrollStateChange: (Boolean) -> Unit = {},
    topPadding: Dp = 0.dp
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val detailedResult = uiState.detailedResult
    val rawResult = uiState.analysisResult

    val data = detailedResult ?: DetailedAnalysisResult()
    val progress = uiState.selectedRecordProgress
    val isPending = uiState.selectedRecord?.status == AnalysisStatus.PENDING

    val scrollState = rememberScrollState()
    LaunchedEffect(uiState.selectedRecord?.id) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            onUserInteracted()
        }
    }

    LaunchedEffect(scrollState) {
        androidx.compose.runtime.snapshotFlow { scrollState.value }
            .collect { scrollValue ->
                onScrollStateChange(scrollValue > 10)
            }
    }

    if (detailedResult == null && !isPending && !rawResult.isNullOrBlank()) {
        // Robust Fallback: Show original plain text result if detailedResult is null (backward compatibility)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding + 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
            color = SurfaceColor,
            shadowElevation = 3.dp,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.analysis_result_text_view),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SumiInk.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Divider(color = SumiInk.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 8.dp))
                SelectionContainer {
                    Text(
                        text = rawResult,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SumiInk,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    } else {
        var selectedSegmentIndex by remember(uiState.selectedRecord?.id) { mutableStateOf(-1) }

        val overscrollTop = remember { Animatable(0f) }
        val overscrollBottom = remember { Animatable(0f) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(uiState.selectedRecord?.id) {
            overscrollTop.snapTo(0f)
            overscrollBottom.snapTo(0f)
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scrollState) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var totalDx = 0f
                        var totalDy = 0f
                        var isDirectionLocked = false
                        var isVerticalOverscroll = false
                        var isDragging = false
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                val positionChange = change.positionChange()
                                totalDx += positionChange.x
                                totalDy += positionChange.y

                                if (!isDirectionLocked &&
                                    (kotlin.math.abs(totalDx) > 20f || kotlin.math.abs(totalDy) > 20f)
                                ) {
                                    isDirectionLocked = true
                                    isVerticalOverscroll = kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx) * 1.2f
                                }

                                if (isDirectionLocked && isVerticalOverscroll) {
                                    val dragY = positionChange.y
                                    val atTop = scrollState.value == 0
                                    val atBottom = scrollState.value == scrollState.maxValue
                                    val isReturningTopOverscroll = overscrollTop.value > 0f && dragY < 0
                                    val isReturningBottomOverscroll = overscrollBottom.value > 0f && dragY > 0
                                    val shouldPullTop = atTop && dragY > 0
                                    val shouldPullBottom = atBottom && dragY < 0

                                    if (kotlin.math.abs(dragY) > 0f &&
                                        (isReturningTopOverscroll || isReturningBottomOverscroll || shouldPullTop || shouldPullBottom)
                                    ) {
                                        isDragging = true

                                        if (isReturningTopOverscroll) {
                                            coroutineScope.launch { overscrollTop.snapTo((overscrollTop.value + dragY * 0.5f).coerceAtLeast(0f)) }
                                            change.consume()
                                        } else if (isReturningBottomOverscroll) {
                                            coroutineScope.launch { overscrollBottom.snapTo((overscrollBottom.value - dragY * 0.5f).coerceAtLeast(0f)) }
                                            change.consume()
                                        } else if (shouldPullTop) {
                                            coroutineScope.launch { overscrollTop.snapTo((overscrollTop.value + dragY * 0.5f).coerceIn(0f, 300f)) }
                                            change.consume()
                                        } else if (shouldPullBottom) {
                                            coroutineScope.launch { overscrollBottom.snapTo((overscrollBottom.value - dragY * 0.5f).coerceIn(0f, 300f)) }
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (isDragging) {
                            if (overscrollTop.value > 150f) {
                                onLoadOlder()
                            }
                            if (overscrollBottom.value > 150f) {
                                onLoadNewer()
                            }
                            coroutineScope.launch { overscrollTop.animateTo(0f) }
                            coroutineScope.launch { overscrollBottom.animateTo(0f) }
                        }
                    }
                }
                .testTag("workspace-result-content")
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(0.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (overscrollTop.value > 0f) {
                    Text(
                        text = stringResource(if (overscrollTop.value > 150f) R.string.release_to_load_older else R.string.loading_older),
                        modifier = Modifier.graphicsLayer { translationY = overscrollTop.value / 2f + 20f },
                        color = SumiInk.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { translationY = overscrollTop.value / 2f - overscrollBottom.value / 2f }
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp)
            ) {
                    Spacer(modifier = Modifier.height(topPadding + 10.dp))
                    // 1. Target Sentence Header with clickable chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp, top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(SumiInk.copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.target_sentence_header),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Crossfade(
                            targetState = isPlayingTts,
                            animationSpec = tween(250),
                            label = "ttsIcon"
                        ) { playing ->
                            if (playing) {
                                IconButton(onClick = onStopTts, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.stop_tts), tint = SumiInk.copy(alpha = 0.7f))
                                }
                            } else {
                                IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_tts), tint = SumiInk.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        color = SurfaceColor,
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 3.dp,
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            val segmentError = progress?.stepErrors?.get("DETAILED_GRAMMAR")
                                ?: progress?.stepErrors?.get("TOKENIZATION")
                            val isLoadingExplain = isPending && progress?.segmentsCompleted != true && !data.segments.isNullOrEmpty() && segmentError == null
                            
                            AnimatedVisibility(
                                visible = isLoadingExplain,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = SumiInk.copy(alpha = 0.4f),
                                        trackColor = SumiInk.copy(alpha = 0.05f)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(stringResource(R.string.analyzing_word_attributes), color = SumiInk.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            if (segmentError != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = segmentError,
                                        color = Color.Red,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (data.segments.isNullOrEmpty()) {
                                    if (segmentError == null && isPending) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp).align(Alignment.CenterVertically),
                                            color = SumiInk.copy(alpha = 0.5f),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.segmenting_text), color = SumiInk.copy(alpha = 0.5f), fontSize = 14.sp)
                                    }
                                } else {
                                    data.segments.forEachIndexed { index, segment ->
                                        val isThisSegmentLoading = isLoadingExplain && index == data.segments.lastIndex
                                        key("${uiState.selectedRecord?.id}_$index") {
                                            val isHistory = !isPending
                                            val visibleState = remember(uiState.selectedRecord?.id) { MutableTransitionState(isHistory) }.apply { targetState = true }
                                            
                                            AnimatedVisibility(
                                                visibleState = visibleState,
                                                enter = if (isHistory) androidx.compose.animation.EnterTransition.None else (scaleIn(initialScale = 0.8f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 800f)) + fadeIn(animationSpec = tween(150)))
                                            ) {
                                                SegmentChip(
                                                    segment = segment,
                                                    isSelected = index == selectedSegmentIndex,
                                                    isLoading = isThisSegmentLoading,
                                                    isBookmarked = uiState.bookmarkedSegmentTexts.contains(segment.text) || 
                                                            uiState.bookmarkedSegmentTexts.contains(segment.dictionaryForm ?: ""),
                                                    onClick = {
                                                        onUserInteracted()
                                                        if (!isThisSegmentLoading) {
                                                            selectedSegmentIndex = if (selectedSegmentIndex == index) -1 else index
                                                        }
                                                    },
                                                    onLongClick = { onToggleBookmark(segment) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Inline Details Card
                    val hasSelectedSegment = selectedSegmentIndex in 0 until (data.segments?.size ?: 0)
                    AnimatedVisibility(
                        visible = hasSelectedSegment,
                        enter = expandVertically(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 350)
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 300)
                        )
                    ) {
                        if (hasSelectedSegment) {
                            val currentSegment = data.segments!![selectedSegmentIndex]
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
                                color = SurfaceColor,
                                shape = RoundedCornerShape(24.dp),
                                shadowElevation = 3.dp,
                                tonalElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = currentSegment.text ?: "",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SumiInk
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                val isCurrentBookmarked = uiState.bookmarkedSegmentTexts.contains(currentSegment.text) || 
                                                        uiState.bookmarkedSegmentTexts.contains(currentSegment.dictionaryForm ?: "")
                                                IconButton(
                                                    onClick = { onToggleBookmark(currentSegment) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCurrentBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                                        contentDescription = stringResource(if (isCurrentBookmarked) R.string.delete_bookmark else R.string.long_press_to_bookmark),
                                                        tint = if (isCurrentBookmarked) Color(0xFFD4A017) else SumiInk.copy(alpha = 0.35f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            if (!currentSegment.reading.isNullOrBlank() && currentSegment.reading != currentSegment.text) {
                                                Text(
                                                    text = "（${currentSegment.reading}）",
                                                    fontSize = 13.sp,
                                                    color = SumiInk.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = { selectedSegmentIndex = -1 },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.close_details),
                                                tint = SumiInk.copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    
                                    Divider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = SumiInk.copy(alpha = 0.08f)
                                    )
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AlignedDetailRow(label = stringResource(R.string.pos), value = currentSegment.partOfSpeech ?: "")
                                        
                                        if (!currentSegment.dictionaryForm.isNullOrBlank() && currentSegment.dictionaryForm != currentSegment.text) {
                                            AlignedDetailRow(label = stringResource(R.string.dictionary_form), value = currentSegment.dictionaryForm)
                                        }
                                        
                                        if (!currentSegment.inflection.isNullOrBlank()) {
                                            AlignedDetailRow(label = stringResource(R.string.inflection), value = currentSegment.inflection)
                                        }
                                        
                                        if (!currentSegment.role.isNullOrBlank()) {
                                            AlignedDetailRow(label = stringResource(R.string.role_in_sentence), value = currentSegment.role)
                                        }
                                    }
                                    
                                    if (!currentSegment.meaning.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            color = KuriAmber.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            SelectionContainer {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.meaning),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = KuriAmber.copy(alpha = 1.0f)
                                                        )
                                                        IconButton(
                                                            onClick = { onEditWordSegment(currentSegment) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = stringResource(R.string.edit),
                                                                tint = SumiInk.copy(alpha = 0.45f),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = currentSegment.meaning,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = SumiInk,
                                                        lineHeight = 20.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val queryWord = currentSegment.dictionaryQueryWord()
                                        
                                        if (queryWord.isNotBlank()) {
                                            DictionarySearchControls(
                                                queryWord = queryWord,
                                                uiPreferencesRepository = uiPreferencesRepository,
                                                sumiInk = SumiInk,
                                                surfaceColor = SurfaceColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // 2. Overall Sentence Translation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp, top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(SakuraPink)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.overall_translation),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk
                        )
                    }
                    
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            color = SurfaceColor,
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 3.dp,
                            tonalElevation = 0.dp
                        ) {
                            SelectionContainer {
                                Row(
                                    modifier = Modifier.animateContentSize().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                     Surface(
                                        color = if (ZenThemeColors.isDark()) Color(0xFF3D1E1E) else SakuraPink,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.translation_label),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ZenThemeColors.isDark()) Color.White else Color(0xFF1E1E1E),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    val translationError = progress?.stepErrors?.get("TRANSLATION")
                                    AnimatedContent(
                                        targetState = data.translation.isNullOrBlank() && isPending && translationError == null,
                                        label = "TranslationAnimation",
                                        modifier = Modifier.weight(1f)
                                    ) { isLoading ->
                                        if (isLoading) {
                                            Column(modifier = Modifier.padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                ShimmerSkeleton(modifier = Modifier.fillMaxWidth().height(16.dp))
                                                ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
                                            }
                                        } else if (translationError != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Error",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = translationError,
                                                    color = Color.Red,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        } else {
                                            StreamingText(
                                                text = data.translation ?: "",
                                                isStreaming = isPending,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SumiInk,
                                                modifier = Modifier.fillMaxWidth(),
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // 3. Sentence Clauses
                    if (!data.clauses.isNullOrEmpty() || (isPending && progress != null && progress.clausesCompleted != true)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp, top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AizomeIndigo)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.sentence_clauses),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SumiInk
                            )
                        }
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            color = SurfaceColor,
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 3.dp,
                            tonalElevation = 0.dp
                        ) {
                            SelectionContainer {
                                Column(modifier = Modifier.animateContentSize().padding(16.dp)) {
                                    val clausesError = progress?.stepErrors?.get("CLAUSE_ANALYSIS")
                                    AnimatedContent(
                                        targetState = data.clauses.isNullOrEmpty() && isPending && clausesError == null,
                                        label = "ClausesAnimation"
                                    ) { isLoading ->
                                        if (isLoading) {
                                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                                repeat(2) {
                                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                                        ShimmerSkeleton(modifier = Modifier.width(14.dp).height(14.dp).padding(top = 4.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                                ShimmerSkeleton(modifier = Modifier.width(36.dp).height(18.dp))
                                                                ShimmerSkeleton(modifier = Modifier.width(60.dp).height(18.dp))
                                                            }
                                                            ShimmerSkeleton(modifier = Modifier.fillMaxWidth().height(12.dp))
                                                            ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (clausesError != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Error",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = clausesError,
                                                    color = Color.Red,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        } else {
                                            Column {
                                                data.clauses?.forEachIndexed { idx, clause ->
                                                    key("clause_${clause.index ?: idx}") {
                                                        val visibleState = remember(clause) { androidx.compose.animation.core.MutableTransitionState(!isPending) }.apply { targetState = true }
                                                        AnimatedVisibility(
                                                            visibleState = visibleState,
                                                            enter = if (!isPending) androidx.compose.animation.EnterTransition.None else fadeIn() + slideInVertically(initialOffsetY = { 20 })
                                                        ) {
                                                            Column {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalAlignment = Alignment.Top
                                                                ) {
                                                                    Text(
                                                                        text = "${clause.index}.",
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = SumiInk,
                                                                        modifier = Modifier.width(22.dp)
                                                                    )
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Row(
                                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Surface(
                                                                                color = AizomeIndigo.copy(alpha = 0.35f),
                                                                                shape = RoundedCornerShape(4.dp)
                                                                            ) {
                                                                                Text(
                                                                                    text = clause.role ?: "",
                                                                                    fontSize = 10.sp,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = SumiInk,
                                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                                )
                                                                            }
                                                                            Text(
                                                                                text = clause.text ?: "",
                                                                                fontSize = 14.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = SumiInk
                                                                            )
                                                                        }
                                                                        Spacer(modifier = Modifier.height(4.dp))
                                                                        StreamingText(
                                                                            text = clause.explanation ?: "",
                                                                            isStreaming = isPending,
                                                                            fontSize = 13.sp,
                                                                            color = SumiInk.copy(alpha = 0.7f),
                                                                            lineHeight = 18.sp
                                                                        )
                                                                    }
                                                                }
                                                                if (idx < (data.clauses?.size ?: 0) - 1) {
                                                                    Divider(
                                                                        modifier = Modifier.padding(vertical = 12.dp),
                                                                        color = SumiInk.copy(alpha = 0.08f)
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
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    
                    // 4. Core Grammar Points
                    if (!data.grammarPoints.isNullOrEmpty() || (isPending && progress != null && progress.grammarCompleted != true)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp, top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MatchaGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.grammar_points),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SumiInk
                            )
                        }
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            color = SurfaceColor,
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 3.dp,
                            tonalElevation = 0.dp
                        ) {
                            SelectionContainer {
                                Column(modifier = Modifier.animateContentSize().padding(16.dp)) {
                                    val grammarError = progress?.stepErrors?.get("GRAMMAR_EXPLANATION")
                                    AnimatedContent(
                                        targetState = data.grammarPoints.isNullOrEmpty() && isPending && grammarError == null,
                                        label = "GrammarAnimation"
                                    ) { isLoading ->
                                        if (isLoading) {
                                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                                repeat(2) {
                                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            ShimmerSkeleton(modifier = Modifier.width(32.dp).height(18.dp))
                                                            ShimmerSkeleton(modifier = Modifier.width(100.dp).height(18.dp))
                                                        }
                                                        ShimmerSkeleton(modifier = Modifier.fillMaxWidth().height(12.dp))
                                                        ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp))
                                                    }
                                                }
                                            }
                                        } else if (grammarError != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Error",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = grammarError,
                                                    color = Color.Red,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        } else {
                                            Column {
                                                data.grammarPoints?.forEachIndexed { idx, gp ->
                                                    key("grammar_$idx") {
                                                        val visibleState = remember(gp) { androidx.compose.animation.core.MutableTransitionState(!isPending) }.apply { targetState = true }
                                                        AnimatedVisibility(
                                                            visibleState = visibleState,
                                                            enter = if (!isPending) androidx.compose.animation.EnterTransition.None else fadeIn() + slideInVertically(initialOffsetY = { 20 })
                                                        ) {
                                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                     Surface(
                                                                         color = if (ZenThemeColors.isDark()) Color(0xFF1E3D1E) else MatchaGreen,
                                                                         shape = RoundedCornerShape(4.dp)
                                                                     ) {
                                                                         Text(
                                                                             text = stringResource(R.string.grammar_label),
                                                                             fontSize = 11.sp,
                                                                             fontWeight = FontWeight.Bold,
                                                                             color = if (ZenThemeColors.isDark()) Color.White else Color(0xFF1E1E1E),
                                                                             modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                         )
                                                                     }
                                                                    Text(
                                                                        text = gp.pattern ?: "",
                                                                        fontSize = 15.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = SumiInk,
                                                                        modifier = Modifier.weight(1f)
                                                                    )
                                                                    val isGrammarBookmarked = uiState.bookmarkedGrammarPointPatterns.contains(gp.pattern ?: "")
                                                                    IconButton(
                                                                        onClick = {
                                                                            if (gp.pattern != null) {
                                                                                onToggleGrammarBookmark(gp.pattern, gp.explanation, uiState.selectedRecord?.originalText ?: "")
                                                                            }
                                                                        },
                                                                        modifier = Modifier.size(32.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = if (isGrammarBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                                                            contentDescription = "Bookmark",
                                                                            tint = if (isGrammarBookmarked) Color(0xFFD4A017) else SumiInk.copy(alpha = 0.4f),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.height(6.dp))
                                                                StreamingText(
                                                                    text = gp.explanation ?: "",
                                                                    isStreaming = isPending,
                                                                    fontSize = 13.sp,
                                                                    color = SumiInk.copy(alpha = 0.8f),
                                                                    lineHeight = 20.sp
                                                                )
                                                                if (idx < (data.grammarPoints?.size ?: 0) - 1) {
                                                                    Divider(
                                                                        modifier = Modifier.padding(vertical = 14.dp),
                                                                        color = SumiInk.copy(alpha = 0.08f)
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
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            Box(
                modifier = Modifier.fillMaxWidth().height(0.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (overscrollBottom.value > 0f) {
                    Text(
                        text = stringResource(if (overscrollBottom.value > 150f) R.string.release_to_load_newer else R.string.loading_newer),
                        modifier = Modifier.graphicsLayer { translationY = -overscrollBottom.value / 2f - 20f },
                        color = SumiInk.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


