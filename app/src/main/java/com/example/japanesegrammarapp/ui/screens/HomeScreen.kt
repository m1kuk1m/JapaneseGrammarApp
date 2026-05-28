package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

// Traditional Japanese Zen Color Palette
val SumiInk = Color(0xFF2B2A28)          // 墨色 (Deep charcoal black)
val WashiBg = Color(0xFFFCF8F2)          // 和纸色 (Soft warm white cream)
val SakuraPink = Color(0xFFFEDFE1)       // 樱花粉 (Soft warm blush pink)
val MatchaGreen = Color(0xFFC5E2C6)      // 抹茶绿 (Soft sage green)
val AizomeIndigo = Color(0xFFBCCCD4)     // 蓝染蓝 (Soft slate blue)
val KuriAmber = Color(0xFFECCEB1)        // 栗色 (Soft amber/chestnut)
val HaiMist = Color(0xFFE5E4E2)          // 雾灰色 (Soft grey)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: AppViewModel) {
    val history by viewModel.history.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = WashiBg,
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "文法分析履歴", 
                        fontWeight = FontWeight.Bold,
                        color = SumiInk
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "戻る",
                            tint = SumiInk
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "設定",
                            tint = SumiInk
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = WashiBg,
                    titleContentColor = SumiInk
                )
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), 
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "分析履歴はありません。", 
                    color = SumiInk.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), 
                contentPadding = PaddingValues(16.dp)
            ) {
                items(history, key = { it.id }) { record ->
                    HistoryItem(record, viewModel, navController)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItem(record: AnalysisRecord, viewModel: AppViewModel, navController: NavController) {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(record.timestamp))

    val isPending = record.status == "PENDING"
    val isFailed = record.status == "FAILED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPending || isFailed) Modifier
                else Modifier.clickable {
                    viewModel.selectRecord(record)
                    navController.navigate("result")
                }
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isFailed -> Color(0xFFD32F2F).copy(alpha = 0.4f)
                isPending -> AizomeIndigo
                else -> SumiInk.copy(alpha = 0.15f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = record.originalText, 
                style = MaterialTheme.typography.bodyLarge, 
                maxLines = 2, 
                overflow = TextOverflow.Ellipsis,
                color = SumiInk,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isPending -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp), 
                            color = SumiInk,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "分析中...", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = SumiInk.copy(alpha = 0.6f)
                        )
                    }
                }
                isFailed -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "エラー: ${record.errorMessage ?: "ネットワークエラー"}", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = Color(0xFFD32F2F),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { viewModel.retryAnalysis(record.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = SumiInk)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "再試行", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("再試行", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { viewModel.deleteRecord(record) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "削除", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("削除", fontSize = 13.sp)
                            }
                        }
                    }
                }
                else -> {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AizomeIndigo.copy(alpha = 0.4f), 
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = record.modelUsed, 
                                style = MaterialTheme.typography.labelSmall, 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), 
                                color = SumiInk,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                text = dateStr, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = SumiInk.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { viewModel.deleteRecord(record) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "削除", 
                                    tint = SumiInk.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
