package com.example.japanesegrammarapp.ui.screens

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun detectCameraOcrBoxes(bitmap: Bitmap): List<Rect> = suspendCancellableCoroutine { continuation ->
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    val image = InputImage.fromBitmap(bitmap, 0)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val lineBoxes = visionText.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line -> line.boundingBox }
            }
            if (continuation.isActive) {
                continuation.resume(mergeNearbyOcrBoxes(lineBoxes, bitmap.width, bitmap.height))
            }
        }
        .addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
        .addOnCompleteListener {
            recognizer.close()
        }

    continuation.invokeOnCancellation {
        recognizer.close()
    }
}

private fun mergeNearbyOcrBoxes(lines: List<Rect>, bitmapWidth: Int, bitmapHeight: Int): List<Rect> {
    val clusters = lines.map { mutableListOf(it) }.toMutableList()

    var changed = true
    while (changed) {
        changed = false
        for (i in clusters.indices) {
            for (j in i + 1 until clusters.size) {
                val isClose = clusters[i].any { lineI ->
                    clusters[j].any { lineJ -> shouldMergeOcrLines(lineI, lineJ) }
                }
                if (isClose) {
                    clusters[i].addAll(clusters[j])
                    clusters.removeAt(j)
                    changed = true
                    break
                }
            }
            if (changed) break
        }
    }

    return clusters.map { cluster ->
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE

        for (rect in cluster) {
            left = minOf(left, rect.left)
            top = minOf(top, rect.top)
            right = maxOf(right, rect.right)
            bottom = maxOf(bottom, rect.bottom)
        }

        val width = right - left
        val height = bottom - top
        val paddingX = (width * 0.15f).toInt().coerceAtLeast(20)
        val paddingY = (height * 0.15f).toInt().coerceAtLeast(20)

        Rect(
            maxOf(0, left - paddingX),
            maxOf(0, top - paddingY),
            minOf(bitmapWidth, right + paddingX),
            minOf(bitmapHeight, bottom + paddingY)
        )
    }
}

private fun shouldMergeOcrLines(lineI: Rect, lineJ: Rect): Boolean {
    val sizeI = minOf(lineI.width(), lineI.height())
    val sizeJ = minOf(lineJ.width(), lineJ.height())
    val maxDist = maxOf(sizeI, sizeJ) * 0.8f
    val isSizeSimilar = maxOf(sizeI, sizeJ).toFloat() / minOf(sizeI, sizeJ).toFloat() < 2.5f
    val overlapX = maxOf(0, minOf(lineI.right, lineJ.right) - maxOf(lineI.left, lineJ.left))
    val overlapY = maxOf(0, minOf(lineI.bottom, lineJ.bottom) - maxOf(lineI.top, lineJ.top))
    val isAligned = overlapX > 0 || overlapY > 0

    if (!isSizeSimilar || !isAligned) {
        return false
    }

    val expandedLine = Rect(
        (lineI.left - maxDist).toInt(),
        (lineI.top - maxDist).toInt(),
        (lineI.right + maxDist).toInt(),
        (lineI.bottom + maxDist).toInt()
    )
    return Rect.intersects(expandedLine, lineJ)
}
