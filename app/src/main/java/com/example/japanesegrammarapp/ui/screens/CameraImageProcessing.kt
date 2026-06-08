package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.japanesegrammarapp.utils.BitmapHelper
import java.io.File

data class CameraCaptureProcessingResult(
    val bitmap: Bitmap,
    val savedUri: Uri?
)

fun processCapturedImageFile(context: Context, file: File): CameraCaptureProcessingResult? {
    val bitmap = BitmapHelper.loadRotatedBitmap(file) ?: return null
    val savedUri = BitmapHelper.saveCroppedBitmap(context, bitmap)
    return CameraCaptureProcessingResult(bitmap = bitmap, savedUri = savedUri)
}

fun createCameraCaptureFile(context: Context): File {
    return BitmapHelper.createTempCapturedFile(context)
}

fun loadCameraReviewBitmap(context: Context, uri: Uri): Bitmap? {
    return BitmapHelper.loadRotatedBitmapFromUri(context, uri)
}

fun saveConfirmedCrop(context: Context, croppedBitmap: Bitmap): Uri? {
    return BitmapHelper.saveCroppedBitmap(context, croppedBitmap)
}
