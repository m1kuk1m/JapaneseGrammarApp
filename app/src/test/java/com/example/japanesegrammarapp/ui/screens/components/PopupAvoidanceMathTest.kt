package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupAvoidanceMathTest {
    private val window = IntSize(width = 400, height = 800)
    private val popup = IntSize(width = 320, height = 500)
    private val gap = 8

    @Test
    fun overlappingPopupChoosesUpwardScrollWhenCloser() {
        val delta = calculatePopupAvoidanceScrollDelta(
            anchorBounds = IntRect(80, 360, 220, 420),
            windowSize = window,
            popupContentSize = popup,
            currentScroll = 300,
            maxScroll = 1000,
            gapPx = gap
        )

        assertEquals(127, delta)
    }

    @Test
    fun overlappingPopupChoosesDownwardScrollWhenCloser() {
        val delta = calculatePopupAvoidanceScrollDelta(
            anchorBounds = IntRect(80, 380, 220, 440),
            windowSize = window,
            popupContentSize = popup,
            currentScroll = 300,
            maxScroll = 1000,
            gapPx = gap
        )

        assertEquals(-127, delta)
    }

    @Test
    fun choosesShortestClearPathWhenBothDirectionsWork() {
        val delta = calculatePopupAvoidanceScrollDelta(
            anchorBounds = IntRect(80, 365, 220, 425),
            windowSize = window,
            popupContentSize = popup,
            currentScroll = 300,
            maxScroll = 1000,
            gapPx = gap
        )

        assertEquals(132, delta)
    }

    @Test
    fun clampsAtTopScrollBoundary() {
        val delta = calculatePopupAvoidanceScrollDelta(
            anchorBounds = IntRect(80, 390, 220, 450),
            windowSize = window,
            popupContentSize = popup,
            currentScroll = 40,
            maxScroll = 80,
            gapPx = gap
        )

        assertTrue(delta in -40..40)
    }

    @Test
    fun oversizedPopupReturnsBoundedDelta() {
        val delta = calculatePopupAvoidanceScrollDelta(
            anchorBounds = IntRect(80, 360, 220, 420),
            windowSize = window,
            popupContentSize = IntSize(width = 380, height = 900),
            currentScroll = 200,
            maxScroll = 500,
            gapPx = gap
        )

        assertTrue(delta in -200..300)
    }

    @Test
    fun returnsZeroWhenPopupAlreadyAvoidsAnchor() {
        val delta = calculatePopupAvoidanceScrollDelta(
            anchorBounds = IntRect(80, 720, 220, 760),
            windowSize = window,
            popupContentSize = IntSize(width = 320, height = 200),
            currentScroll = 200,
            maxScroll = 1000,
            gapPx = gap
        )

        assertEquals(0, delta)
    }
}
