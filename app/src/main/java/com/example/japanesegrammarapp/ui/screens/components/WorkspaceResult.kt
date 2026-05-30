package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.network.WordSegment
import com.example.japanesegrammarapp.ui.WorkspaceUiState
import com.example.japanesegrammarapp.ui.theme.ZenColors.AizomeIndigo
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.MatchaGreen
import com.example.japanesegrammarapp.ui.theme.ZenColors.SakuraPink

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceResultContent(
    uiState: WorkspaceUiState,
    isPlayingTts: Boolean = false,
    onPlayTts: () -> Unit = {},
    onStopTts: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val detailedResult = uiState.detailedResult
    val rawResult = uiState.analysisResult

    val data = detailedResult ?: com.example.japanesegrammarapp.network.DetailedAnalysisResult()
    val progress = uiState.selectedRecordProgress
    val isPending = uiState.selectedRecord?.status == "PENDING"

    if (detailedResult == null && !isPending && !rawResult.isNullOrBlank()) {
        // Robust Fallback: Show original plain text result if detailedResult is null (backward compatibility)
        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = SurfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(8.dp)
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
                Text(
                    text = rawResult ?: stringResource(R.string.no_analysis_result),
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SumiInk,
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        var selectedSegmentIndex by remember(data) { mutableStateOf(-1) }

            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    // 1. Target Sentence Header with clickable chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
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
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        SegmentChip(
                                            segment = segment,
                                            isSelected = index == selectedSegmentIndex,
                                            isLoading = isLoadingExplain,
                                            onClick = {
                                                if (!isLoadingExplain) {
                                                    selectedSegmentIndex = if (selectedSegmentIndex == index) -1 else index
                                                }
                                            }
                                        )
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
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.1f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                            Text(
                                                text = currentSegment.text ?: "",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SumiInk
                                            )
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
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // 2. Overall Sentence Translation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
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
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF3D1E1E) else SakuraPink,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.translation_label),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else Color(0xFF1E1E1E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (data.translation.isNullOrBlank() && isPending) {
                                Column(modifier = Modifier.weight(1f).padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShimmerSkeleton(modifier = Modifier.fillMaxWidth().height(16.dp))
                                    ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
                                }
                            } else {
                                Text(
                                    text = data.translation ?: "",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SumiInk,
                                    modifier = Modifier.weight(1f),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // 3. Sentence Clauses
                    if (!data.clauses.isNullOrEmpty() || (isPending && progress?.clausesCompleted != true)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
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
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (data.clauses.isNullOrEmpty() && isPending) {
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
                                    data.clauses?.forEachIndexed { idx, clause ->
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
                                                Text(
                                                    text = clause.explanation ?: "",
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
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    
                    // 4. Core Grammar Points
                    if (!data.grammarPoints.isNullOrEmpty() || (isPending && progress?.grammarCompleted != true)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
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
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (data.grammarPoints.isNullOrEmpty() && isPending) {
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
                                    data.grammarPoints?.forEachIndexed { idx, gp ->
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E3D1E) else MatchaGreen,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.grammar_label),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else Color(0xFF1E1E1E),
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
                                            Text(
                                                text = gp.explanation ?: "",
                                                fontSize = 13.sp,
                                                color = SumiInk.copy(alpha = 0.8f),
                                                lineHeight = 20.sp
                                            )
                                        }
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
                        Spacer(modifier = Modifier.height(20.dp))
                    }
            }
        }
    }
}


// Morandi color coding helper
@Composable
private fun getChipColorForPos(segment: WordSegment): Color {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val category = segment.posCategory
    if (category != null) {
        return when (category) {
            "NOUN" -> if (isDark) Color(0xFF1E2D3D) else Color(0xFFD3E0EA)       // 蓝染蓝 (Aizome)
            "VERB" -> if (isDark) Color(0xFF1E3D24) else Color(0xFFD4ECD5)       // 抹茶绿 (Matcha)
            "ADJECTIVE" -> if (isDark) Color(0xFF3D2A1E) else Color(0xFFF6E2CD)  // 栗色 (Kuri)
            "AUXILIARY" -> if (isDark) Color(0xFF2D1E3D) else Color(0xFFE8D3EA)  // 藤紫 (Fuji)
            "PARTICLE" -> if (isDark) Color(0xFF3D1E25) else Color(0xFFFDD4D8)   // 樱花粉 (Sakura)
            else -> if (isDark) Color(0xFF2D2D2D) else Color(0xFFEFEFEF)         // 雾灰 (Hai)
        }
    }

    // 向下兼容：如果历史数据没有 posCategory，回退到基于 partOfSpeech 的模糊匹配
    val pos = segment.partOfSpeech ?: ""
    val primaryPos = pos.split("-").firstOrNull() ?: ""
    return when {
        primaryPos.contains("助動詞") -> if (isDark) Color(0xFF2D1E3D) else Color(0xFFE8D3EA) // 藤紫 (Fuji)
        primaryPos.contains("形容") || primaryPos.contains("形状") -> if (isDark) Color(0xFF3D2A1E) else Color(0xFFF6E2CD) // 栗色 (Kuri)
        primaryPos.contains("名詞") -> if (isDark) Color(0xFF1E2D3D) else Color(0xFFD3E0EA) // 蓝染蓝 (Aizome)
        primaryPos.contains("動詞") -> if (isDark) Color(0xFF1E3D24) else Color(0xFFD4ECD5) // 抹茶绿 (Matcha)
        primaryPos.contains("助詞") -> if (isDark) Color(0xFF3D1E25) else Color(0xFFFDD4D8) // 樱花粉 (Sakura)
        else -> if (isDark) Color(0xFF2D2D2D) else Color(0xFFEFEFEF) // 雾灰 (Hai)
    }
}

@Composable
fun AlignedDetailRow(label: String, value: String) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SumiInk.copy(alpha = 0.5f),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SumiInk,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SegmentChip(
    segment: WordSegment,
    isSelected: Boolean,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) SumiInk else SumiInk.copy(alpha = if (isLoading) 0.0f else 0.15f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "borderColor"
    )
    val bgColor = if (isLoading) Color(0xFFF3F3F3) else getChipColorForPos(segment)
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val ChipTextColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1E1E1E)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = 1.5.dp, color = borderColor),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = segment.reading ?: "",
                fontSize = 9.sp,
                color = ChipTextColor.copy(alpha = if (isLoading) 0.0f else 0.6f)
            )
            Text(
                text = segment.text ?: "",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChipTextColor.copy(alpha = if (isLoading) 0.4f else 1.0f)
            )
        }
    }
}
@Composable
fun ShimmerSkeleton(modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 4.dp) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .background(SumiInk.copy(alpha = alpha), RoundedCornerShape(cornerRadius))
    )
}
