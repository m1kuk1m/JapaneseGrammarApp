package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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

class CollapsingFilterState(
    val isVisible: Boolean,
    val nestedScrollConnection: NestedScrollConnection,
    val reset: () -> Unit
)

@Composable
fun rememberCollapsingFilterState(
    lazyListState: LazyListState,
    thresholdPx: Float = 15f
): CollapsingFilterState {
    var isScrolledVisible by rememberSaveable { mutableStateOf(true) }
    val isAtTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset <= 10
        }
    }
    val isVisible = isAtTop || isScrolledVisible

    val nestedScrollConnection = remember(thresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy < -thresholdPx) {
                    // Scrolling down (finger moves up, content moves up) -> collapse
                    isScrolledVisible = false
                } else if (dy > thresholdPx) {
                    // Scrolling up (finger moves down, content moves down) -> expand
                    isScrolledVisible = true
                }
                return Offset.Zero
            }
        }
    }

    return remember(isVisible, nestedScrollConnection) {
        CollapsingFilterState(
            isVisible = isVisible,
            nestedScrollConnection = nestedScrollConnection,
            reset = { isScrolledVisible = true }
        )
    }
}
