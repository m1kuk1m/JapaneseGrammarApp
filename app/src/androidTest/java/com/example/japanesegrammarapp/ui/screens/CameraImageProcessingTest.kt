package com.example.japanesegrammarapp.ui.screens

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraImageProcessingTest {
    @Test
    fun rotationUsesFullNaturalBoundsWithoutDownsamplingSource() {
        val source = Bitmap.createBitmap(120, 60, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }

        val rotated = rotateBitmapPreservingContent(source, 12f)

        assertTrue(rotated.width > source.width)
        assertTrue(rotated.height > source.height)
        assertFalse(source.isRecycled)
        assertTrue(rotated.pixelsContain(Color.RED))

        rotated.recycle()
        source.recycle()
    }

    private fun Bitmap.pixelsContain(color: Int): Boolean {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels.any { pixel ->
            Color.red(pixel) == Color.red(color) &&
                Color.green(pixel) == Color.green(color) &&
                Color.blue(pixel) == Color.blue(color)
        }
    }
}
