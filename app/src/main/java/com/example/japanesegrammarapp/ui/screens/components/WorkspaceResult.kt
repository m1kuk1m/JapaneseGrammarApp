package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkspaceResultContent(
    uiState: WorkspaceUiState,
    isPlayingTts: Boolean = false,
    onPlayTts: () -> Unit = {},
    onStopTts: () -> Unit = {},
    onToggleBookmark: (WordSegment) -> Unit = {},
    uiPreferencesRepository: UiPreferencesRepository
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val detailedResult = uiState.detailedResult
    val rawResult = uiState.analysisResult

    val data = detailedResult ?: DetailedAnalysisResult()
    val progress = uiState.selectedRecordProgress
    val isPending = uiState.selectedRecord?.status == AnalysisStatus.PENDING

    if (detailedResult == null && !isPending && !rawResult.isNullOrBlank()) {
        // Robust Fallback: Show original plain text result if detailedResult is null (backward compatibility)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
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
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SumiInk,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    } else {
        var selectedSegmentIndex by remember(uiState.selectedRecord?.id) { mutableStateOf(-1) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("workspace-result-content")
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                    Spacer(modifier = Modifier.height(10.dp))
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
                        if (isPlayingTts) {
                            IconButton(onClick = onStopTts, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.stop_tts), tint = SumiInk.copy(alpha = 0.7f))
                            }
                        } else {
                            IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_tts), tint = SumiInk.copy(alpha = 0.7f))
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
                            val isLoadingExplain = isPending && progress?.segmentsCompleted != true && !data.segments.isNullOrEmpty()
                            
                            if (isLoadingExplain) {
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
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (data.segments.isNullOrEmpty()) {
                                    if (isPending) {
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
                                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
                                color = SurfaceColor,
                                shape = RoundedCornerShape(24.dp),
                                shadowElevation = 3.dp,
                                tonalElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                                    Text(
                                                        text = stringResource(R.string.meaning),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = KuriAmber.copy(alpha = 1.0f)
                                                    )
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
                                    AnimatedContent(
                                        targetState = data.translation.isNullOrBlank() && isPending,
                                        label = "TranslationAnimation",
                                        modifier = Modifier.weight(1f)
                                    ) { isLoading ->
                                        if (isLoading) {
                                            Column(modifier = Modifier.padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                ShimmerSkeleton(modifier = Modifier.fillMaxWidth().height(16.dp))
                                                ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
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
                                    AnimatedContent(
                                        targetState = data.clauses.isNullOrEmpty() && isPending,
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
                                    AnimatedContent(
                                        targetState = data.grammarPoints.isNullOrEmpty() && isPending,
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
                                                                        color = SumiInk
                                                                    )
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
        }
    }
}


