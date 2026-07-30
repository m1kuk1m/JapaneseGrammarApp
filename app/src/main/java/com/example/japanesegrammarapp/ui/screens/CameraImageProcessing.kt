package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import com.example.japanesegrammarapp.utils.BitmapHelper
import java.io.File

data class CameraCaptureProcessingResult(
    val bitmap: Bitmap,
    val savedUri: Uri?,
    val wasRotatedToPortrait: Boolean = false // Kept for backwards compatibility but always false
)

fun processCapturedImageFile(context: Context, file: File): CameraCaptureProcessingResult? {
    val bitmap = BitmapHelper.loadRotatedBitmap(file) ?: return null
    val savedUri = BitmapHelper.saveCroppedBitmap(context, bitmap)
    return CameraCaptureProcessingResult(bitmap = bitmap, savedUri = savedUri, wasRotatedToPortrait = false)
}

fun createCameraCaptureFile(context: Context): File {
    return BitmapHelper.createTempCapturedFile(context)
}

data class CameraReviewBitmapResult(
    val bitmap: Bitmap,
    val wasRotatedToPortrait: Boolean = false
)

sealed interface DeskewOutcome {
    data object Disabled : DeskewOutcome
    data class Corrected(val angle: Float) : DeskewOutcome
    data class NotNeeded(val angle: Float) : DeskewOutcome
    data object NoText : DeskewOutcome
    data class Failed(val cause: Throwable) : DeskewOutcome
    data object TimedOut : DeskewOutcome
}

data class DeskewProcessingResult(
    val bitmap: Bitmap,
    val outcome: DeskewOutcome
)

fun rotateBitmapPreservingContent(bitmap: Bitmap, angle: Float): Bitmap {
    if (angle == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(angle) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun loadCameraReviewBitmap(context: Context, uri: Uri): CameraReviewBitmapResult? {
    val bitmap = BitmapHelper.loadRotatedBitmapFromUri(context, uri) ?: return null
    return CameraReviewBitmapResult(bitmap = bitmap, wasRotatedToPortrait = false)
}

fun saveConfirmedCrop(context: Context, croppedBitmap: Bitmap, inverseRotate: Boolean = false): Uri? {
    return BitmapHelper.saveCroppedBitmap(context, croppedBitmap)
}
