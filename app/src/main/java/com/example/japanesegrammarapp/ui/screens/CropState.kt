package com.example.japanesegrammarapp.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

enum class DragHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}

@Stable
class CropState(
    private val bitmapWidth: Float,
    private val bitmapHeight: Float
) {
    var containerWidth by mutableStateOf(0f)
    var containerHeight by mutableStateOf(0f)

    var cropLeft by mutableStateOf(0f)
    var cropTop by mutableStateOf(0f)
    var cropRight by mutableStateOf(0f)
    var cropBottom by mutableStateOf(0f)

    var isInitialized by mutableStateOf(false)

    var imgDispWidth by mutableStateOf(0f)
    var imgDispHeight by mutableStateOf(0f)
    var scaleFactor by mutableStateOf(1f)
    var imgOffsetX by mutableStateOf(0f)
    var imgOffsetY by mutableStateOf(0f)

    var activeHandle by mutableStateOf(DragHandle.NONE)
    var visualRotationAngle by mutableStateOf(0f)

    fun initializeCropBox(width: Float, height: Float) {
        if (width <= 0f || height <= 0f || isInitialized) return
        containerWidth = width
        containerHeight = height

        val scaleX = containerWidth / bitmapWidth
        val scaleY = containerHeight / bitmapHeight
        scaleFactor = minOf(scaleX, scaleY)

        imgDispWidth = bitmapWidth * scaleFactor
        imgDispHeight = bitmapHeight * scaleFactor

        imgOffsetX = (containerWidth - imgDispWidth) / 2
        imgOffsetY = (containerHeight - imgDispHeight) / 2

        val initialW = imgDispWidth * 0.8f
        val initialH = imgDispHeight * 0.4f

        cropLeft = imgOffsetX + (imgDispWidth - initialW) / 2
        cropTop = imgOffsetY + (imgDispHeight - initialH) / 2
        cropRight = cropLeft + initialW
        cropBottom = cropTop + initialH

        isInitialized = true
    }

    fun startDrag(offset: Offset, minTolerancePx: Float) {
        val x = offset.x
        val y = offset.y

        val distTL = distance(x, y, cropLeft, cropTop)
        val distTR = distance(x, y, cropRight, cropTop)
        val distBL = distance(x, y, cropLeft, cropBottom)
        val distBR = distance(x, y, cropRight, cropBottom)

        var bestHandle = DragHandle.NONE
        var minDist = minTolerancePx

        if (distTL < minDist) {
            minDist = distTL
            bestHandle = DragHandle.TOP_LEFT
        }
        if (distTR < minDist) {
            minDist = distTR
            bestHandle = DragHandle.TOP_RIGHT
        }
        if (distBL < minDist) {
            minDist = distBL
            bestHandle = DragHandle.BOTTOM_LEFT
        }
        if (distBR < minDist) {
            minDist = distBR
            bestHandle = DragHandle.BOTTOM_RIGHT
        }

        activeHandle = if (bestHandle != DragHandle.NONE) {
            bestHandle
        } else {
            DragHandle.NONE
        }
    }

    fun onDrag(dragAmount: Offset, minSizePx: Float) {
        val dx = dragAmount.x
        val dy = dragAmount.y

        when (activeHandle) {
            DragHandle.CENTER -> {
                val w = cropRight - cropLeft
                val h = cropBottom - cropTop

                cropLeft = (cropLeft + dx).coerceIn(imgOffsetX, imgOffsetX + imgDispWidth - w)
                cropRight = cropLeft + w
                cropTop = (cropTop + dy).coerceIn(imgOffsetY, imgOffsetY + imgDispHeight - h)
                cropBottom = cropTop + h
            }
            DragHandle.TOP_LEFT -> {
                cropLeft = (cropLeft + dx).coerceIn(imgOffsetX, cropRight - minSizePx)
                cropTop = (cropTop + dy).coerceIn(imgOffsetY, cropBottom - minSizePx)
            }
            DragHandle.TOP_RIGHT -> {
                cropRight = (cropRight + dx).coerceIn(cropLeft + minSizePx, imgOffsetX + imgDispWidth)
                cropTop = (cropTop + dy).coerceIn(imgOffsetY, cropBottom - minSizePx)
            }
            DragHandle.BOTTOM_LEFT -> {
                cropLeft = (cropLeft + dx).coerceIn(imgOffsetX, cropRight - minSizePx)
                cropBottom = (cropBottom + dy).coerceIn(cropTop + minSizePx, imgOffsetY + imgDispHeight)
            }
            DragHandle.BOTTOM_RIGHT -> {
                cropRight = (cropRight + dx).coerceIn(cropLeft + minSizePx, imgOffsetX + imgDispWidth)
                cropBottom = (cropBottom + dy).coerceIn(cropTop + minSizePx, imgOffsetY + imgDispHeight)
            }
            DragHandle.NONE -> {}
        }
    }

    fun stopDrag() {
        activeHandle = DragHandle.NONE
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
