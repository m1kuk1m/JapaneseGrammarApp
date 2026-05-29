package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.ui.theme.ZenColors.AizomeIndigo
import com.example.japanesegrammarapp.ui.theme.ZenColors.HaiMist
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.MatchaGreen
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import kotlinx.coroutines.delay

@Composable
fun ZenLoadingView(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Double Breathing Zen Orb Indicator
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(90.dp)
        ) {
            // Outer breathing circle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .alpha(1f - alpha)
                    .clip(RoundedCornerShape(45.dp))
                    .background(AizomeIndigo)
            )
            // Inner breathing circle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .scale(2f - scale)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(25.dp))
                    .background(KuriAmber.copy(alpha = 0.8f))
            )
            // Center solid elegant core
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SumiInk)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "AIによる日本語文法解析中",
            fontWeight = FontWeight.Bold,
            color = SumiInk,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "文法の構造を分析しています。少々お待ちください。",
            color = SumiInk.copy(alpha = 0.4f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 2. Intelligent Thinking Stepper Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AnalysisStepTimeline()
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 3. Cancel Button
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "キャンセル",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "解析をキャンセル",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AnalysisStepTimeline() {
    val steps = listOf(
        "形態素解析・単語トークンの抽出" to "和文テキストの形態素解析を行っています",
        "統語役割・文節の関係性識別" to "文節の役割と相互関係を分类しています",
        "日本語文脈翻訳・スマート対訳" to "適切な翻訳と対照データを生成しています",
        "詳細な文法解説・アドバイスの構築" to "重要文法の詳細な解説カードを作成しています"
    )

    var currentStepIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(1200L)
        currentStepIndex = 1
        delay(2400L)
        currentStepIndex = 2
        delay(2600L)
        currentStepIndex = 3
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseDot")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        steps.forEachIndexed { index, (title, description) ->
            val isCompleted = index < currentStepIndex
            val isRunning = index == currentStepIndex
            val isWaiting = index > currentStepIndex

            val textColor = when {
                isCompleted -> SumiInk.copy(alpha = 0.9f)
                isRunning -> SumiInk
                else -> SumiInk.copy(alpha = 0.3f)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Step Indicator Left
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MatchaGreen.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "完了",
                                tint = SumiInk.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else if (isRunning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(KuriAmber.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(dotScale)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SumiInk)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(HaiMist.copy(alpha = 0.5f))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text Description Right
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isRunning) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = if (isWaiting) SumiInk.copy(alpha = 0.2f) else SumiInk.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
