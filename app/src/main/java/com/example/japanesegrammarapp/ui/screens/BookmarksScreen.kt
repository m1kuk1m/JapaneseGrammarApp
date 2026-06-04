package com.example.japanesegrammarapp.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.ui.BookmarkViewModel
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    navController: NavController,
    viewModel: BookmarkViewModel,
    onNavigateToRecord: (recordId: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    val bookmarks by viewModel.allBookmarks.collectAsState()

    // Track which card is in "confirm delete" mode (long-pressed)
    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }

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
                    val msg = if (count >= 0) "成功导入 $count 条收藏" else "导入失败，请检查文件格式"
                    snackbarHostState.showSnackbar(msg)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("导入失败：${e.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
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
                            tint = Color(0xFFD4A017),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "收藏の単語",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (bookmarks.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFD4A017).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${bookmarks.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD4A017),
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
                            contentDescription = "返回",
                            modifier = Modifier
                                .size(22.dp)
                                // Mirror horizontally to create a back arrow feel
                        )
                    }
                },
                actions = {
                    // Import button
                    IconButton(onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "导入收藏")
                    }
                    // Export button
                    IconButton(onClick = { viewModel.exportAndShare(context) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "导出收藏")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (bookmarks.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("flashcard") },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("开始闪卡练习", fontWeight = FontWeight.SemiBold) },
                    containerColor = Color(0xFFD4A017),
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->
        if (bookmarks.isEmpty()) {
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
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "还没有收藏",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "在分析页面长按分词卡片即可收藏",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
            ) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    BookmarkCard(
                        bookmark = bookmark,
                        isPendingDelete = pendingDeleteId == bookmark.id,
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pendingDeleteId = bookmark.id
                        },
                        onConfirmDelete = {
                            viewModel.removeBookmark(bookmark.id)
                            pendingDeleteId = null
                        },
                        onCancelDelete = { pendingDeleteId = null },
                        onNavigateToSource = {
                            pendingDeleteId = null
                            onNavigateToRecord(bookmark.recordId)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkCard(
    bookmark: BookmarkedSegmentDomain,
    isPendingDelete: Boolean,
    onLongPress: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onNavigateToSource: () -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val posCat = bookmark.posCategory ?: ""
    val chipBg = (if (isDark) PosColorsDark[posCat] else PosColors[posCat])
        ?: if (isDark) Color(0xFF2D2D2D) else Color(0xFFEFEFEF)

    val borderColor by animateColorAsState(
        targetValue = if (isPendingDelete) Color(0xFFD32F2F).copy(alpha = 0.7f) else Color(0xFFD4A017).copy(alpha = 0.5f),
        animationSpec = tween(250),
        label = "cardBorder"
    )

    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isPendingDelete) onCancelDelete() },
                onLongClick = onLongPress
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Word info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = bookmark.segmentText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk
                        )
                        if (!bookmark.reading.isNullOrBlank() && bookmark.reading != bookmark.segmentText) {
                            Text(
                                text = bookmark.reading,
                                fontSize = 13.sp,
                                color = SumiInk.copy(alpha = 0.55f),
                                modifier = Modifier.padding(bottom = 2.dp)
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

                // Navigate to source button
                IconButton(
                    onClick = onNavigateToSource,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "查看原句",
                        tint = SumiInk.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Meaning
            if (!bookmark.meaning.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFD4A017).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bookmark.meaning,
                        fontSize = 13.sp,
                        color = SumiInk,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Source sentence preview
            if (bookmark.sourceText.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "来自：${bookmark.sourceText.take(40)}${if (bookmark.sourceText.length > 40) "…" else ""}",
                    fontSize = 11.sp,
                    color = SumiInk.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Delete confirmation row
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
                        Text("取消", fontSize = 13.sp)
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
                        Text("删除收藏", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
