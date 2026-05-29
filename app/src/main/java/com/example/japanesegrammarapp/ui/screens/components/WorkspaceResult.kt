package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.network.WordSegment
import com.example.japanesegrammarapp.ui.WorkspaceUiState
import com.example.japanesegrammarapp.ui.theme.ZenColors.AizomeIndigo
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.MatchaGreen
import com.example.japanesegrammarapp.ui.theme.ZenColors.SakuraPink
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceResultContent(uiState: WorkspaceUiState) {
    val detailedResult = uiState.detailedResult
    val rawResult = uiState.analysisResult

    AnimatedContent(
        targetState = detailedResult,
        transitionSpec = {
            fadeIn(animationSpec = tween(400, easing = EaseInOutCubic))
                .togetherWith(fadeOut(animationSpec = tween(400, easing = EaseInOutCubic)))
        },
        label = "ResultViewTypeTransition",
        modifier = Modifier.fillMaxSize()
    ) { data ->
        if (data != null) {
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
                            text = "対象の例文",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk
                        )
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            data.segments?.forEachIndexed { index, segment ->
                                SegmentChip(
                                    segment = segment,
                                    isSelected = index == selectedSegmentIndex,
                                    onClick = {
                                        selectedSegmentIndex = if (selectedSegmentIndex == index) -1 else index
                                    }
                                )
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
                                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
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
                                                    color = SumiInk.copy(alpha = 0.6f),
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = { selectedSegmentIndex = -1 },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close details",
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
                                        AlignedDetailRow(label = "品詞", value = currentSegment.partOfSpeech ?: "")
                                        
                                        if (!currentSegment.dictionaryForm.isNullOrBlank() && currentSegment.dictionaryForm != currentSegment.text) {
                                            AlignedDetailRow(label = "辞書形", value = currentSegment.dictionaryForm)
                                        }
                                        
                                        if (!currentSegment.inflection.isNullOrBlank()) {
                                            AlignedDetailRow(label = "構成・活用", value = currentSegment.inflection)
                                        }
                                        
                                        if (!currentSegment.role.isNullOrBlank()) {
                                            AlignedDetailRow(label = "文中の役割", value = currentSegment.role)
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
                                                    text = "中国語の訳・意味",
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
                            text = "全体の翻訳",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk
                        )
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SakuraPink,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "翻訳",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SumiInk,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
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
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // 3. Sentence Clauses
                    if (!data.clauses.isNullOrEmpty()) {
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
                                text = "文の構造と解釈",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SumiInk
                            )
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                                    if (idx < data.clauses.size - 1) {
                                        Divider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = SumiInk.copy(alpha = 0.08f)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    
                    // 4. Core Grammar Points
                    if (!data.grammarPoints.isNullOrEmpty()) {
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
                                text = "文法ポイント",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SumiInk
                            )
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                data.grammarPoints?.forEachIndexed { idx, gp ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = MatchaGreen,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "文法",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SumiInk,
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
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        } else {
            // Robust Fallback: Show original plain text result if detailedResult is null (backward compatibility)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "分析結果 (テキスト表示)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Divider(color = SumiInk.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = rawResult ?: "分析結果はありません。",
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
    }
}

// Morandi color coding helper
private fun getChipColorForPos(segment: WordSegment): Color {
    val category = segment.posCategory
    if (category != null) {
        return when (category) {
            "NOUN" -> Color(0xFFD3E0EA)       // 蓝染蓝 (Aizome)
            "VERB" -> Color(0xFFD4ECD5)       // 抹茶绿 (Matcha)
            "ADJECTIVE" -> Color(0xFFF6E2CD)  // 栗色 (Kuri)
            "AUXILIARY" -> Color(0xFFE8D3EA)  // 藤紫 (Fuji)
            "PARTICLE" -> Color(0xFFFDD4D8)   // 樱花粉 (Sakura)
            else -> Color(0xFFEFEFEF)         // 雾灰 (Hai)
        }
    }

    // 向下兼容：如果历史数据没有 posCategory，回退到基于 partOfSpeech 的模糊匹配
    val pos = segment.partOfSpeech ?: ""
    val primaryPos = pos.split("-").firstOrNull() ?: ""
    return when {
        primaryPos.contains("助動詞") -> Color(0xFFE8D3EA) // 藤紫 (Fuji)
        primaryPos.contains("形容") || primaryPos.contains("形状") -> Color(0xFFF6E2CD) // 栗色 (Kuri)
        primaryPos.contains("名詞") -> Color(0xFFD3E0EA) // 蓝染蓝 (Aizome)
        primaryPos.contains("動詞") -> Color(0xFFD4ECD5) // 抹茶绿 (Matcha)
        primaryPos.contains("助詞") -> Color(0xFFFDD4D8) // 樱花粉 (Sakura)
        else -> Color(0xFFEFEFEF) // 雾灰 (Hai)
    }
}

@Composable
fun AlignedDetailRow(label: String, value: String) {
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
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) SumiInk else SumiInk.copy(alpha = 0.15f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "borderColor"
    )
    Surface(
        color = getChipColorForPos(segment),
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
                color = SumiInk.copy(alpha = 0.5f)
            )
            Text(
                text = segment.text ?: "",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SumiInk
            )
        }
    }
}
