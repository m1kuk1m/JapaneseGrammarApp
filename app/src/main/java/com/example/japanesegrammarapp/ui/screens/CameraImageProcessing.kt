package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.japanesegrammarapp.utils.BitmapHelper
import java.io.File

data class CameraCaptureProcessingResult(
    val bitmap: Bitmap,
    val savedUri: Uri?,
    val wasRotatedToPortrait: Boolean = false
)

fun processCapturedImageFile(context: Context, file: File): CameraCaptureProcessingResult? {
    val bitmap = BitmapHelper.loadRotatedBitmap(file) ?: return null
    
    val wasLandscape = bitmap.width > bitmap.height
    val finalBitmap = if (wasLandscape) {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(90f)
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        rotated
    } else {
        bitmap
    }

    val savedUri = BitmapHelper.saveCroppedBitmap(context, finalBitmap)
    return CameraCaptureProcessingResult(bitmap = finalBitmap, savedUri = savedUri, wasRotatedToPortrait = wasLandscape)
}

fun createCameraCaptureFile(context: Context): File {
    return BitmapHelper.createTempCapturedFile(context)
}

data class CameraReviewBitmapResult(
    val bitmap: Bitmap,
    val wasRotatedToPortrait: Boolean = false
)

fun loadCameraReviewBitmap(context: Context, uri: Uri): CameraReviewBitmapResult? {
    val bitmap = BitmapHelper.loadRotatedBitmapFromUri(context, uri) ?: return null
    val wasLandscape = bitmap.width > bitmap.height
    val finalBitmap = if (wasLandscape) {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(90f)
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        rotated
    } else {
        bitmap
    }
    return CameraReviewBitmapResult(bitmap = finalBitmap, wasRotatedToPortrait = wasLandscape)
}

fun saveConfirmedCrop(context: Context, croppedBitmap: Bitmap, inverseRotate: Boolean = false): Uri? {
    val finalBitmap = if (inverseRotate) {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(-90f)
        val rotated = Bitmap.createBitmap(croppedBitmap, 0, 0, croppedBitmap.width, croppedBitmap.height, matrix, true)
        rotated
    } else {
        croppedBitmap
    }
    
    val uri = BitmapHelper.saveCroppedBitmap(context, finalBitmap)
    
    if (inverseRotate && finalBitmap !== croppedBitmap) {
        finalBitmap.recycle()
    }
    
    return uri
}
