package com.example.japanesegrammarapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object BitmapHelper {
    private const val CROPPED_JPEG_QUALITY = 92
    private const val MIN_SAMPLED_TARGET_RATIO = 0.85f

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
        return rotateIfNeeded(bitmap, readExifRotation(context, uri))
    }

    fun loadRotatedBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        if (maxDimension <= 0) return loadRotatedBitmapFromUri(context, uri)

        return try {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, bounds)
            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return loadRotatedBitmapFromUri(context, uri)
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return null

            rotateIfNeeded(bitmap, readExifRotation(context, uri))
        } catch (e: Exception) {
            null
        }
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
        return rotateIfNeeded(bitmap, angle)
    }

    fun createTempCapturedFile(context: Context): File {
        return File(context.cacheDir, "capture_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg")
    }

    fun saveCroppedBitmap(context: Context, bitmap: Bitmap): Uri? {
        val file = File(context.cacheDir, "cropped_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, CROPPED_JPEG_QUALITY, out)
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

    private fun readExifRotation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        var sampledWidth = width
        var sampledHeight = height
        val minSampledDimension = maxDimension * MIN_SAMPLED_TARGET_RATIO

        while (maxOf(sampledWidth / 2, sampledHeight / 2) >= minSampledDimension) {
            inSampleSize *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }

        return inSampleSize
    }

    private fun rotateIfNeeded(source: Bitmap, angle: Float): Bitmap {
        if (angle == 0f) return source
        return rotateBitmap(source, angle)
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        source.recycle()
        return rotated
    }
}
