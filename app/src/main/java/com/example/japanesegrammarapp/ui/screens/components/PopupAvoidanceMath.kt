package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun calculateSmartPopupOffset(
    anchorBounds: IntRect,
    windowSize: IntSize,
    popupContentSize: IntSize,
    gapPx: Int
): IntOffset {
    val x = (windowSize.width - popupContentSize.width) / 2
    val spaceBelow = windowSize.height - anchorBounds.bottom
    val spaceAbove = anchorBounds.top

    val y = if (spaceBelow >= popupContentSize.height + gapPx || spaceBelow >= spaceAbove) {
        anchorBounds.bottom + gapPx
    } else {
        anchorBounds.top - popupContentSize.height - gapPx
    }

    return IntOffset(
        x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
    )
}

internal fun calculateSmartPopupBounds(
    anchorBounds: IntRect,
    windowSize: IntSize,
    popupContentSize: IntSize,
    gapPx: Int
): IntRect {
    val offset = calculateSmartPopupOffset(anchorBounds, windowSize, popupContentSize, gapPx)
    return IntRect(
        left = offset.x,
        top = offset.y,
        right = offset.x + popupContentSize.width,
        bottom = offset.y + popupContentSize.height
    )
}

internal fun calculatePopupAvoidanceScrollDelta(
    anchorBounds: IntRect,
    windowSize: IntSize,
    popupContentSize: IntSize,
    currentScroll: Int,
    maxScroll: Int,
    gapPx: Int
): Int {
    if (popupContentSize.width <= 0 || popupContentSize.height <= 0 || windowSize.height <= 0) {
        return 0
    }

    val minDelta = -currentScroll
    val maxDelta = (maxScroll - currentScroll).coerceAtLeast(minDelta)

    fun shiftedAnchor(delta: Int): IntRect = IntRect(
        left = anchorBounds.left,
        top = anchorBounds.top - delta,
        right = anchorBounds.right,
        bottom = anchorBounds.bottom - delta
    )

    fun overlapAfter(delta: Int): Int {
        val shifted = shiftedAnchor(delta)
        val popup = calculateSmartPopupBounds(shifted, windowSize, popupContentSize, gapPx)
        return overlapArea(popup, shifted)
    }

    if (overlapAfter(0) == 0) return 0

    val deltaToFitBelow = anchorBounds.bottom - (windowSize.height - popupContentSize.height - gapPx)
    val deltaToFitAbove = anchorBounds.top - (popupContentSize.height + gapPx)
    val rawCandidates = buildSet {
        add(0)
        add(minDelta)
        add(maxDelta)
        add(deltaToFitBelow)
        add(deltaToFitAbove)
        add(deltaToFitBelow - 1)
        add(deltaToFitBelow + 1)
        add(deltaToFitAbove - 1)
        add(deltaToFitAbove + 1)
    }

    return rawCandidates
        .map { it.coerceIn(minDelta, maxDelta) }
        .toSet()
        .map { delta -> AvoidanceCandidate(delta, overlapAfter(delta)) }
        .minWith(
            compareBy<AvoidanceCandidate> { if (it.overlapArea == 0) 0 else 1 }
                .thenBy { if (it.overlapArea == 0) abs(it.delta) else it.overlapArea }
                .thenBy { abs(it.delta) }
        )
        .delta
}

private data class AvoidanceCandidate(
    val delta: Int,
    val overlapArea: Int
)

private fun overlapArea(a: IntRect, b: IntRect): Int {
    val overlapWidth = min(a.right, b.right) - max(a.left, b.left)
    val overlapHeight = min(a.bottom, b.bottom) - max(a.top, b.top)
    if (overlapWidth <= 0 || overlapHeight <= 0) return 0
    return overlapWidth * overlapHeight
}
