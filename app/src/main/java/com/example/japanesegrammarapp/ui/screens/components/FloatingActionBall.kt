package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class ActionMode {
    Text, Camera
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingActionBall(
    onTextClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val ballSizePx = with(density) { 56.dp.toPx() }

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("fab_prefs", android.content.Context.MODE_PRIVATE) }

    // Initial position (bottom right, avoiding typical bottom nav areas)
    var offsetX by remember { 
        val defaultX = screenWidthPx - ballSizePx - with(density) { 16.dp.toPx() }
        val savedX = prefs.getFloat("fab_x", defaultX)
        mutableFloatStateOf(savedX.coerceIn(0f, maxOf(0f, screenWidthPx - ballSizePx)))
    }
    var offsetY by remember { 
        val defaultY = screenHeightPx - ballSizePx - with(density) { 120.dp.toPx() }
        val savedY = prefs.getFloat("fab_y", defaultY)
        mutableFloatStateOf(savedY.coerceIn(0f, maxOf(0f, screenHeightPx - ballSizePx)))
    }
    
    var isDragging by remember { mutableStateOf(false) }
    var currentMode by remember { 
        val savedMode = prefs.getString("fab_mode", ActionMode.Text.name) ?: ActionMode.Text.name
        mutableStateOf(ActionMode.valueOf(savedMode)) 
    }

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

    Box(modifier = modifier.fillMaxSize()) {
        // Draggable Ball
        Surface(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                .size(56.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { 
                            isDragging = true 
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

                            prefs.edit().putFloat("fab_x", offsetX).putFloat("fab_y", offsetY).apply()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - ballSizePx)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx - ballSizePx)
                        }
                    )
                }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!isDragging) {
                            when (currentMode) {
                                ActionMode.Text -> onTextClick()
                                ActionMode.Camera -> onCameraClick()
                            }
                        }
                    },
                    onLongClick = {
                        if (!isDragging) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentMode = if (currentMode == ActionMode.Text) ActionMode.Camera else ActionMode.Text
                            prefs.edit().putString("fab_mode", currentMode.name).apply()
                        }
                    }
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = if (isDragging) 12.dp else 6.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (currentMode == ActionMode.Text) Icons.Default.Edit else Icons.Default.CameraAlt,
                    contentDescription = if (currentMode == ActionMode.Text) "Text Input" else "Camera",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}