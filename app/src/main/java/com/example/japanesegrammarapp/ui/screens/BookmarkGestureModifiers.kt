package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

fun Modifier.bookmarkRightSwipeBack(onBack: () -> Unit): Modifier = pointerInput(onBack) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var totalDx = 0f
        var totalDy = 0f
        var isDecided = false
        var isRightSwipe = false
        var event: PointerEvent

        do {
            // Let child components such as horizontal chips handle their own gestures first.
            event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull()
            if (change != null && !change.isConsumed) {
                if (!isDecided) {
                    totalDx += change.positionChange().x
                    totalDy += change.positionChange().y
                    if (abs(totalDx) > 40f || abs(totalDy) > 40f) {
                        isDecided = true
                        if (totalDx > 0f && abs(totalDx) > abs(totalDy) * 1.5f) {
                            isRightSwipe = true
                        }
                    }
                }
                
                if (isDecided && isRightSwipe) {
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })

        if (isDecided && isRightSwipe) {
            onBack()
        }
    }
}
