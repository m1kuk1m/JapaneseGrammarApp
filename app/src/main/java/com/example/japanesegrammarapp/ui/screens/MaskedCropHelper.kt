package com.example.japanesegrammarapp.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect

object MaskedCropHelper {

    fun createMaskedBitmap(originalBitmap: Bitmap, selectedBoxes: List<Rect>): Bitmap {
        if (selectedBoxes.isEmpty()) {
            val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            emptyBitmap.eraseColor(Color.WHITE)
            return emptyBitmap
        }

        var minLeft = Int.MAX_VALUE
        var minTop = Int.MAX_VALUE
        var maxRight = Int.MIN_VALUE
        var maxBottom = Int.MIN_VALUE

        for (box in selectedBoxes) {
            if (box.left < minLeft) minLeft = box.left
            if (box.top < minTop) minTop = box.top
            if (box.right > maxRight) maxRight = box.right
            if (box.bottom > maxBottom) maxBottom = box.bottom
        }

        // Ensure bounds are within the original bitmap
        minLeft = minLeft.coerceAtLeast(0)
        minTop = minTop.coerceAtLeast(0)
        maxRight = maxRight.coerceAtMost(originalBitmap.width)
        maxBottom = maxBottom.coerceAtMost(originalBitmap.height)

        val width = maxRight - minLeft
        val height = maxBottom - minTop

        if (width <= 0 || height <= 0) {
            val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            emptyBitmap.eraseColor(Color.WHITE)
            return emptyBitmap
        }

        val config = originalBitmap.config ?: Bitmap.Config.ARGB_8888
        val maskedBitmap = Bitmap.createBitmap(width, height, config)
        
        // Fill the background with pure white
        maskedBitmap.eraseColor(Color.WHITE)

        val canvas = Canvas(maskedBitmap)

        for (box in selectedBoxes) {
            val left = box.left.coerceAtLeast(0)
            val top = box.top.coerceAtLeast(0)
            val right = box.right.coerceAtMost(originalBitmap.width)
            val bottom = box.bottom.coerceAtMost(originalBitmap.height)

            if (left >= right || top >= bottom) continue

            val srcRect = Rect(left, top, right, bottom)
            val dstRect = Rect(left - minLeft, top - minTop, right - minLeft, bottom - minTop)

            // Copy only the selected box pixels from original to the new white bitmap
            canvas.drawBitmap(originalBitmap, srcRect, dstRect, null)
        }

        return maskedBitmap
    }
}
