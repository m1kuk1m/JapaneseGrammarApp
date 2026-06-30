package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    fontScale: Float = 1.0f,
    spacingScale: Float = 1.0f,
    furiganaScale: Float = 1.0f,
    internalPaddingScale: Float = 1.0f,
    furiganaGapScale: Float = 1.0f,
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
            isSelected && isBookmarked -> goldColor
            isSelected -> sumiInk
            isBookmarked -> goldColor
            isLoading -> Color.Transparent
            else -> sumiInk.copy(alpha = 0.15f)
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "borderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = when {
            isSelected && isBookmarked -> 2.5.dp
            isBookmarked -> 2.dp
            else -> 1.5.dp
        },
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
    val baseBgColor = getChipColorForPos(segment)
    val hasDetails = !segment.meaning.isNullOrBlank() || !segment.role.isNullOrBlank()
    val targetBgColor = if (isLoading) {
        sumiInk.copy(alpha = shimmerAlpha)
    } else if (hasDetails) {
        baseBgColor
    } else {
        baseBgColor.copy(alpha = 0.35f)
    }
    
    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "bgColor"
    )
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
                .animateContentSize(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
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
                modifier = Modifier.padding(
                    horizontal = 8.dp * internalPaddingScale,
                    vertical = 4.dp * internalPaddingScale
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isPunctuation = segment.partOfSpeech?.contains("記号") == true || segment.posCategory == "symbol" || segment.text?.matches(Regex("^[\\p{Punct}、。！？「」『』（）]+$")) == true
                val displayReading = if (isPunctuation) "\u200B" else (segment.reading?.takeIf { it.isNotBlank() } ?: "\u200B")

                AnimatedContent(
                    targetState = Pair(displayReading, segment.text ?: ""),
                    transitionSpec = { 
                        (fadeIn(tween(150)) togetherWith fadeOut(tween(150))).using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "textReveal"
                ) { (targetReading, targetText) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = targetReading,
                            fontSize = 9.sp * furiganaScale,
                            lineHeight = 10.sp * furiganaScale,
                            color = chipTextColor.copy(
                                alpha = if (isLoading || targetReading == "\u200B") 0.0f else 0.6f
                            )
                        )
                        if (targetReading != "\u200B") {
                            Spacer(modifier = Modifier.height(2.dp * furiganaGapScale))
                        }
                        Text(
                            text = targetText,
                            fontSize = 16.sp * fontScale,
                            fontWeight = FontWeight.SemiBold,
                            color = chipTextColor.copy(alpha = if (isLoading) 0.4f else 1.0f)
                        )
                    }
                }
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
                    .matchParentSize()
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
    val category = segment.posCategory
    val resolvedCategory = if (category != null && category != "OTHER") {
        category
    } else {
        val pos = segment.partOfSpeech ?: ""
        val primaryPos = pos.split("-").firstOrNull() ?: ""
        when {
            primaryPos.contains("代名詞") -> "PRONOUN"
            primaryPos.contains("感動詞") -> "INTERJECTION"
            primaryPos.contains("助動詞") -> "AUXILIARY"
            primaryPos.contains("形容") || primaryPos.contains("形状") -> "ADJECTIVE"
            primaryPos.contains("名詞") || primaryPos.contains("数詞") -> "NOUN"
            primaryPos.contains("動詞") -> "VERB"
            primaryPos.contains("助詞") -> "PARTICLE"
            primaryPos.contains("副詞") || primaryPos.contains("擬態語") -> "ADVERB"
            primaryPos.contains("接続詞") -> "CONJUNCTION"
            primaryPos.contains("連体詞") -> "PRE_NOUN_ADJECTIVAL"
            primaryPos.contains("記号") -> "SYMBOL"
            primaryPos.contains("接尾辞") || primaryPos.contains("接頭辞") -> "AFFIX"
            primaryPos.contains("連語") || primaryPos.contains("慣用句") -> "PHRASE"
            else -> "OTHER"
        }
    }
    return ZenThemeColors.getChipColor(resolvedCategory)
}
