package com.example.japanesegrammarapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object BitmapHelper {
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun loadRotatedBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        val bitmap = getBitmapFromUri(context, uri) ?: return null
        val stream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exif = ExifInterface(stream)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val angle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        stream.close()
        return if (angle != 0f) rotateBitmap(bitmap, angle) else bitmap
    }

    fun loadRotatedBitmap(file: File): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val angle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        return if (angle != 0f) rotateBitmap(bitmap, angle) else bitmap
    }

    fun createTempCapturedFile(context: Context): File {
        return File(context.cacheDir, "capture_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg")
    }

    fun saveCroppedBitmap(context: Context, bitmap: Bitmap): Uri? {
        val file = File(context.cacheDir, "cropped_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
        return Uri.fromFile(file)
    }

    fun copyUriToCache(context: Context, uri: Uri): Uri? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val uniqueName = "copied_temp_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
            val file = File(context.cacheDir, uniqueName)
            val out = FileOutputStream(file)
            stream.copyTo(out)
            stream.close()
            out.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    fun scaleDown(realImage: Bitmap, maxImageSize: Float): Bitmap {
        val ratio = Math.min(
            maxImageSize / realImage.width,
            maxImageSize / realImage.height
        )
        if (ratio >= 1.0) return realImage
        val width = Math.round(ratio * realImage.width)
        val height = Math.round(ratio * realImage.height)
        return Bitmap.createScaledBitmap(realImage, width, height, true)
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
