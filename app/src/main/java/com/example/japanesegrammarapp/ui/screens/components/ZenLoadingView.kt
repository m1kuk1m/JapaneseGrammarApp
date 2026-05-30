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
import androidx.compose.ui.res.stringResource
import com.example.japanesegrammarapp.R
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
import com.example.japanesegrammarapp.domain.usecase.AnalysisProgress
import kotlinx.coroutines.delay

@Composable
fun ZenLoadingView(
    progress: AnalysisProgress?,
    onCancel: () -> Unit
) {
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
            text = stringResource(R.string.loading_title),
            fontWeight = FontWeight.Bold,
            color = SumiInk,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.loading_subtitle),
            color = SumiInk.copy(alpha = 0.4f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 2. Intelligent Thinking Stepper Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AnalysisStepTimeline(progress)
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
                contentDescription = stringResource(R.string.cancel),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.cancel_analysis),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AnalysisStepTimeline(progress: AnalysisProgress?) {
    val steps = listOf(
        stringResource(R.string.step_1_title) to stringResource(R.string.step_1_desc),
        stringResource(R.string.step_2_title) to stringResource(R.string.step_2_desc),
        stringResource(R.string.step_3_title) to stringResource(R.string.step_3_desc),
        stringResource(R.string.step_4_title) to stringResource(R.string.step_4_desc)
    )

    // Fallback simulated progress when progress flow is null (e.g. initial loading/local parsing)
    var currentStepIndex by remember { mutableStateOf(0) }
    if (progress == null) {
        LaunchedEffect(Unit) {
            delay(1200L)
            currentStepIndex = 1
            delay(2400L)
            currentStepIndex = 2
            delay(2600L)
            currentStepIndex = 3
        }
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

    val firstUncompletedIndex = if (progress != null) {
        val list = listOf(
            progress.segmentsCompleted,
            progress.clausesCompleted,
            progress.translationCompleted,
            progress.grammarCompleted
        )
        list.indexOfFirst { !it }
    } else {
        currentStepIndex
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        steps.forEachIndexed { index, (title, description) ->
            val isCompleted = if (progress != null) {
                when (index) {
                    0 -> progress.segmentsCompleted
                    1 -> progress.clausesCompleted
                    2 -> progress.translationCompleted
                    3 -> progress.grammarCompleted
                    else -> false
                }
            } else {
                index < currentStepIndex
            }

            val isRunning = if (progress != null) {
                index == firstUncompletedIndex
            } else {
                index == currentStepIndex
            }

            val isWaiting = if (progress != null) {
                !isCompleted && index != firstUncompletedIndex
            } else {
                index > currentStepIndex
            }

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
                                contentDescription = stringResource(R.string.completed),
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
