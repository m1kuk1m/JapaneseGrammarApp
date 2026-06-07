package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.domain.model.WordSegment
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors

@Composable
fun AlignedDetailRow(label: String, value: String) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = sumiInk.copy(alpha = 0.5f),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = sumiInk,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SegmentChip(
    segment: WordSegment,
    isSelected: Boolean,
    isLoading: Boolean = false,
    isBookmarked: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val haptic = LocalHapticFeedback.current

    var wasBookmarked by remember { mutableStateOf(isBookmarked) }
    var starScale by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(isBookmarked) {
        if (isBookmarked && !wasBookmarked) {
            val pulse = Animatable(1f)
            pulse.animateTo(1.3f, animationSpec = tween(120, easing = FastOutSlowInEasing))
            pulse.animateTo(1.0f, animationSpec = tween(150, easing = FastOutSlowInEasing))
            starScale = 1f
        }
        wasBookmarked = isBookmarked
    }

    val goldColor = Color(0xFFD4A017)
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> sumiInk
            isBookmarked -> goldColor
            isLoading -> Color.Transparent
            else -> sumiInk.copy(alpha = 0.15f)
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "borderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isBookmarked && !isSelected) 2.dp else 1.5.dp,
        label = "borderWidth"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "chipShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val bgColor = if (isLoading) sumiInk.copy(alpha = shimmerAlpha) else getChipColorForPos(segment)
    val chipTextColor = if (ZenThemeColors.isDark()) Color(0xFFE0E0E0) else Color(0xFF1E1E1E)

    var showGlowAnimation by remember { mutableStateOf(false) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(isBookmarked) {
        if (isBookmarked && showGlowAnimation) {
            glow.snapTo(0.4f)
            glow.animateTo(0f, animationSpec = tween(600, easing = EaseInOutCubic))
            showGlowAnimation = false
        }
    }

    Box {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(width = borderWidth, color = borderColor),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!isBookmarked) {
                            showGlowAnimation = true
                        }
                        onLongClick()
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = segment.reading ?: "",
                    fontSize = 9.sp,
                    color = chipTextColor.copy(alpha = if (isLoading) 0.0f else 0.6f)
                )
                Text(
                    text = segment.text ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = chipTextColor.copy(alpha = if (isLoading) 0.4f else 1.0f)
                )
            }
        }

        if (isBookmarked) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = goldColor,
                modifier = Modifier
                    .size(11.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                    }
            )
        }

        if (isBookmarked && glow.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(goldColor.copy(alpha = glow.value))
            )
        }
    }
}

@Composable
fun ShimmerSkeleton(modifier: Modifier = Modifier, cornerRadius: Dp = 4.dp) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = modifier.background(sumiInk.copy(alpha = alpha), RoundedCornerShape(cornerRadius))
    )
}

@Composable
private fun getChipColorForPos(segment: WordSegment): Color {
    val isDark = ZenThemeColors.isDark()
    val category = segment.posCategory
    if (category != null) {
        return when (category) {
            "NOUN" -> if (isDark) Color(0xFF1E2D3D) else Color(0xFFD3E0EA)
            "VERB" -> if (isDark) Color(0xFF1E3D24) else Color(0xFFD4ECD5)
            "ADJECTIVE" -> if (isDark) Color(0xFF3D2A1E) else Color(0xFFF6E2CD)
            "AUXILIARY" -> if (isDark) Color(0xFF2D1E3D) else Color(0xFFE8D3EA)
            "PARTICLE" -> if (isDark) Color(0xFF3D1E25) else Color(0xFFFDD4D8)
            else -> if (isDark) Color(0xFF2D2D2D) else Color(0xFFEFEFEF)
        }
    }

    val pos = segment.partOfSpeech ?: ""
    val primaryPos = pos.split("-").firstOrNull() ?: ""
    return when {
        primaryPos.contains("助動詞") -> if (isDark) Color(0xFF2D1E3D) else Color(0xFFE8D3EA)
        primaryPos.contains("形容") || primaryPos.contains("形状") -> if (isDark) Color(0xFF3D2A1E) else Color(0xFFF6E2CD)
        primaryPos.contains("名詞") -> if (isDark) Color(0xFF1E2D3D) else Color(0xFFD3E0EA)
        primaryPos.contains("動詞") -> if (isDark) Color(0xFF1E3D24) else Color(0xFFD4ECD5)
        primaryPos.contains("助詞") -> if (isDark) Color(0xFF3D1E25) else Color(0xFFFDD4D8)
        else -> if (isDark) Color(0xFF2D2D2D) else Color(0xFFEFEFEF)
    }
}
