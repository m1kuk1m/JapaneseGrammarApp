package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesegrammarapp.network.DetailedAnalysisResult
import com.example.japanesegrammarapp.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalysisResultScreen(navController: NavController, viewModel: AppViewModel) {
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val rawResult by viewModel.analysisResult.collectAsState()
    val detailedResult by viewModel.detailedResult.collectAsState()

    // Zen Palette
    val SumiInk = Color(0xFF2B2A28)
    val WashiBg = Color(0xFFFCF8F2)
    val AizomeIndigo = Color(0xFFBCCCD4)
    val SakuraPink = Color(0xFFFEDFE1)
    val MatchaGreen = Color(0xFFC5E2C6)
    val KuriAmber = Color(0xFFECCEB1)

    // Helper for Part of Speech color coding (Morandi Japanese Colors)
    fun getChipColorForPos(pos: String): Color {
        return when {
            pos.contains("名詞") -> Color(0xFFD3E0EA) // 蓝染蓝 (Aizome)
            pos.contains("動詞") -> Color(0xFFD4ECD5) // 抹茶绿 (Matcha)
            pos.contains("形容") || pos.contains("形状") -> Color(0xFFF6E2CD) // 栗色 (Kuri)
            pos.contains("助詞") -> Color(0xFFFDD4D8) // 樱花粉 (Sakura)
            pos.contains("助動詞") -> Color(0xFFE8D3EA) // 藤紫 (Fuji)
            else -> Color(0xFFEFEFEF) // 雾灰 (Hai)
        }
    }

    Scaffold(
        containerColor = WashiBg,
        topBar = {
            TopAppBar(
                title = { Text("分析結果", fontWeight = FontWeight.Bold, color = SumiInk) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = SumiInk)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("input") {
                            popUpTo("input") { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "再編集",
                            tint = SumiInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WashiBg,
                    titleContentColor = SumiInk
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SumiInk)
            } else if (detailedResult != null) {
                val data = detailedResult!!
                var selectedSegmentIndex by remember { mutableStateOf(0) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    
                    // 1. Target Sentence Header with clickable chips
                    Text(
                        text = "対象の例文",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    // Elegant flow row of segments
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            data.segments.forEachIndexed { index, segment ->
                                val isSelected = index == selectedSegmentIndex
                                Surface(
                                    color = getChipColorForPos(segment.partOfSpeech),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) SumiInk else SumiInk.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier
                                        .clickable { selectedSegmentIndex = index }
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = segment.reading,
                                            fontSize = 10.sp,
                                            color = SumiInk.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = segment.text,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SumiInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 2. Segment Details Explanation Box
                    val currentSegment = data.segments.getOrNull(selectedSegmentIndex)
                    if (currentSegment != null) {
                        Text(
                            text = "1. 単語の分解と分析",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = currentSegment.text,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SumiInk
                                    )
                                    Text(
                                        text = "（${currentSegment.reading}）",
                                        fontSize = 14.sp,
                                        color = SumiInk.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = SumiInk.copy(alpha = 0.1f)
                                )
                                
                                // Details Grid
                                DetailRow(label = "品詞", value = currentSegment.partOfSpeech)
                                
                                if (!currentSegment.dictionaryForm.isNullOrBlank()) {
                                    DetailRow(label = "辞書形", value = currentSegment.dictionaryForm)
                                }
                                
                                if (!currentSegment.inflection.isNullOrBlank()) {
                                    DetailRow(label = "構成/活用", value = currentSegment.inflection)
                                }
                                
                                DetailRow(label = "役割", value = currentSegment.role)
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = SumiInk.copy(alpha = 0.1f)
                                )
                                
                                // Meaning Callout
                                Surface(
                                    color = KuriAmber.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, KuriAmber.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "中国語の訳・意味",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SumiInk.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentSegment.meaning,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SumiInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 3. Overall Sentence Translation
                    Text(
                        text = "2. 全体の翻訳と文の種類",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SumiInk,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = data.translation,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = SumiInk,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 4. Sentence Clauses & Phrase Structure (文節構造)
                    if (data.clauses.isNotEmpty()) {
                        Text(
                            text = "3. 文節の解釈と構造",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                data.clauses.forEachIndexed { idx, clause ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "${clause.index}.",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SumiInk,
                                            modifier = Modifier.width(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    color = AizomeIndigo.copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = clause.role,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SumiInk,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = clause.text,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SumiInk
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = clause.explanation,
                                                fontSize = 13.sp,
                                                color = SumiInk.copy(alpha = 0.7f)
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
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // 5. Core Grammar Points summary
                    if (data.grammarPoints.isNotEmpty()) {
                        Text(
                            text = "4. 文法ポイントの分析",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                data.grammarPoints.forEachIndexed { idx, gp ->
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
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SumiInk,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = gp.pattern,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SumiInk
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = gp.explanation,
                                            fontSize = 14.sp,
                                            color = SumiInk.copy(alpha = 0.8f),
                                            lineHeight = 20.sp
                                        )
                                    }
                                    if (idx < data.grammarPoints.size - 1) {
                                        Divider(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            color = SumiInk.copy(alpha = 0.08f)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                // Robust Fallback: Show original plain text result if detailedResult is null (backward compatibility)
                ElevatedCard(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "分析結果 (テキスト表示)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Divider(color = SumiInk.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 12.dp))
                        Text(
                            text = rawResult ?: "分析結果はありません。",
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SumiInk,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    val SumiInk = Color(0xFF2B2A28)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = SumiInk.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SumiInk,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
