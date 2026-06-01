package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FloatingActionBall(
    onTextClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val ballSizePx = with(density) { 56.dp.toPx() }

    // Initial position (bottom right, avoiding typical bottom nav areas)
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - ballSizePx - with(density) { 16.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx - ballSizePx - with(density) { 120.dp.toPx() }) }
    
    var isDragging by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "offsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "offsetY"
    )
    
    val menuAlpha by animateFloatAsState(targetValue = if (isExpanded) 1f else 0f, label = "menuAlpha")
    val menuScale by animateFloatAsState(targetValue = if (isExpanded) 1f else 0.8f, label = "menuScale")

    Box(modifier = modifier.fillMaxSize()) {
        
        // Expanded Menu
        if (isExpanded || menuAlpha > 0f) {
            // Invisible touch interceptor to close menu when clicking outside
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = false }
            )
            
            // Calculate where to pop out the menu based on the ball's position
            val isLeftHalf = animatedOffsetX < screenWidthPx / 2
            val isTopHalf = animatedOffsetY < screenHeightPx / 2
            
            val menuOffsetX = if (isLeftHalf) animatedOffsetX + ballSizePx + with(density) { 16.dp.toPx() } else animatedOffsetX - with(density) { 140.dp.toPx() }
            val menuOffsetY = if (isTopHalf) animatedOffsetY else animatedOffsetY - with(density) { 60.dp.toPx() }

            Surface(
                modifier = Modifier
                    .offset { IntOffset(menuOffsetX.roundToInt(), menuOffsetY.roundToInt()) }
                    .alpha(menuAlpha)
                    .scale(menuScale),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isExpanded = false
                                onTextClick()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Text Input", tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("文字入力", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Divider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isExpanded = false
                                onCameraClick()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("写真分析", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Draggable Ball
        Surface(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                .size(56.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { 
                            isDragging = true 
                            isExpanded = false
                        },
                        onDragEnd = {
                            isDragging = false
                            // Snap to edge
                            val edgeMargin = with(density) { 8.dp.toPx() }
                            offsetX = if (offsetX < screenWidthPx / 2) edgeMargin else screenWidthPx - ballSizePx - edgeMargin
                            
                            // Constrain Y
                            offsetY = offsetY.coerceIn(
                                with(density) { 32.dp.toPx() },
                                screenHeightPx - ballSizePx - with(density) { 32.dp.toPx() }
                            )
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - ballSizePx)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx - ballSizePx)
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isDragging) {
                        isExpanded = !isExpanded
                    }
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = if (isDragging) 12.dp else 6.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = "Floating Action",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}