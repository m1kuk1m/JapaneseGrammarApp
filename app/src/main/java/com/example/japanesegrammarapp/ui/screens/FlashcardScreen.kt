package com.example.japanesegrammarapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.ui.BookmarkViewModel

private val GoldColor = Color(0xFFD4A017)
private val GoldLight = Color(0xFFFFF3C4)
private val GoldDark = Color(0xFF3D2E00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    navController: NavController,
    viewModel: BookmarkViewModel
) {
    val bookmarks by viewModel.allBookmarks.collectAsState()
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val WashiBg = MaterialTheme.colorScheme.background
    val isDark = WashiBg.red < 0.5f

    // Mastered tracking per session (SnapshotStateList gives reactive .size)
    val masteredIds = remember { mutableStateListOf<Int>() }
    val cards = remember(bookmarks) { bookmarks.shuffled() }

    var currentIndex by remember(cards) { mutableStateOf(0) }
    var isFlipped by remember(currentIndex) { mutableStateOf(false) }
    var sessionFinished by remember(cards) { mutableStateOf(false) }

    // 3D flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "flip"
    )

    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有可练习的收藏", color = SumiInk.copy(alpha = 0.5f), fontSize = 16.sp)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!sessionFinished) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentIndex + 1} / ${cards.size}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(12.dp))
                            // Progress bar
                            LinearProgressIndicator(
                                progress = (currentIndex + 1f) / cards.size,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .graphicsLayer { clip = true; shape = RoundedCornerShape(3.dp) },
                                color = GoldColor,
                                trackColor = SumiInk.copy(alpha = 0.1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GoldColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = " ${masteredIds.size}",
                                fontSize = 13.sp,
                                color = GoldColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text("练习完成 🎉", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WashiBg
                )
            )
        }
    ) { padding ->
        if (sessionFinished) {
            ResultSummary(
                total = cards.size,
                mastered = masteredIds.size,
                onRestart = {
                    masteredIds.clear()
                    currentIndex = 0
                    sessionFinished = false
                },
                onBack = { navController.popBackStack() },
                isDark = isDark
            )
        } else {
            val card = cards.getOrNull(currentIndex)
            if (card != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))

                    // Hint text
                    Text(
                        text = if (isFlipped) "点击再次翻转" else "点击翻转查看释义",
                        fontSize = 12.sp,
                        color = SumiInk.copy(alpha = 0.35f)
                    )

                    Spacer(Modifier.height(16.dp))

                    // Flashcard
                    FlipCard(
                        card = card,
                        rotation = rotation,
                        isFlipped = isFlipped,
                        isDark = isDark,
                        onClick = { isFlipped = !isFlipped },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(Modifier.height(24.dp))

                    // Action row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Previous
                        OutlinedButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    currentIndex--
                                    isFlipped = false
                                }
                            },
                            enabled = currentIndex > 0,
                            modifier = Modifier.size(52.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "上一张", modifier = Modifier.size(20.dp))
                        }

                        // Mastered toggle
                        val isMastered = masteredIds.contains(card.id)
                        Button(
                            onClick = {
                                if (isMastered) masteredIds.remove(card.id)
                                else masteredIds.add(card.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMastered) GoldColor else SumiInk.copy(alpha = 0.08f),
                                contentColor = if (isMastered) Color.White else SumiInk.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isMastered) "已掌握 ✓" else "标记掌握",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        // Next / Finish
                        val isLast = currentIndex == cards.lastIndex
                        Button(
                            onClick = {
                                if (isLast) {
                                    sessionFinished = true
                                } else {
                                    currentIndex++
                                    isFlipped = false
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLast) GoldColor else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = if (isLast) "完成" else "下一张",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(
    card: BookmarkedSegmentDomain,
    rotation: Float,
    isFlipped: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val goldBg = if (isDark) GoldDark else GoldLight

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Front face (rotation 0..90)
        if (!isFlipped) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 6.dp,
                color = SurfaceColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Star decoration
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    // Reading (furigana)
                    if (!card.reading.isNullOrBlank() && card.reading != card.segmentText) {
                        Text(
                            text = card.reading,
                            fontSize = 18.sp,
                            color = SumiInk.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Light
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    // Main word
                    Text(
                        text = card.segmentText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = SumiInk,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Back face (needs to be un-mirrored)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 6.dp,
                color = goldBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // POS badge
                    if (!card.partOfSpeech.isNullOrBlank()) {
                        Surface(
                            color = GoldColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = card.partOfSpeech,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) GoldColor else Color(0xFF7A4F00),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    // Meaning
                    Text(
                        text = card.meaning ?: "（暂无释义）",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) SumiInk else Color(0xFF3D2800),
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    // Extra info
                    val extras = buildList {
                        card.inflection?.let { add("活用：$it") }
                        card.role?.let { add("角色：$it") }
                        card.dictionaryForm?.takeIf { it != card.segmentText }?.let { add("原形：$it") }
                    }
                    if (extras.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Divider(color = GoldColor.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                        extras.forEach { line ->
                            Text(
                                text = line,
                                fontSize = 13.sp,
                                color = if (isDark) SumiInk.copy(0.6f) else Color(0xFF7A4F00).copy(0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummary(
    total: Int,
    mastered: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    isDark: Boolean
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🎉", fontSize = 56.sp)
            Text(
                "练习完成！",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = SumiInk
            )
            Surface(
                color = GoldColor.copy(alpha = if (isDark) 0.2f else 0.12f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("本次成绩", fontSize = 13.sp, color = SumiInk.copy(0.5f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatCell(label = "总卡数", value = "$total", color = SumiInk)
                        Text("·", fontSize = 24.sp, color = SumiInk.copy(0.3f))
                        StatCell(label = "已掌握", value = "$mastered", color = GoldColor)
                        Text("·", fontSize = 24.sp, color = SumiInk.copy(0.3f))
                        StatCell(
                            label = "正确率",
                            value = "${if (total > 0) mastered * 100 / total else 0}%",
                            color = if (mastered * 2 >= total) Color(0xFF2E7D32) else SumiInk
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = GoldColor, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("再练一遍", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("返回收藏", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
    }
}
