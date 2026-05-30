package com.example.japanesegrammarapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object BitmapHelper {

    private const val TAG = "BitmapHelper"

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // Loads a photo from file and corrects rotation according to EXIF with safe downsampling
    fun loadRotatedBitmap(file: File): Bitmap? {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            options.inSampleSize = calculateInSampleSize(options, 1200, 1200)
            options.inJustDecodeBounds = false
            
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            return rotated
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading rotated bitmap from file", e)
            return null
        }
    }

    // Load rotated bitmap from ContentProvider Uri (Gallery selection) with safe downsampling
    fun loadRotatedBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            options.inSampleSize = calculateInSampleSize(options, 1200, 1200)
            options.inJustDecodeBounds = false
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return null
                
                var rotation = 0
                context.contentResolver.openInputStream(uri)?.use { exifInputStream ->
                    try {
                        val exif = ExifInterface(exifInputStream)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        rotation = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading EXIF from uri input stream", e)
                    }
                }
                
                if (rotation == 0) return bitmap
                
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                return rotated
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading bitmap from URI", e)
        }
        return null
    }

    // Creates temp capture file
    fun createTempCapturedFile(context: Context): File {
        val imagesDir = File(context.filesDir, "images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        return File(imagesDir, "temp_captured_${System.currentTimeMillis()}.jpg")
    }

    // Saves the cropped bitmap to interior storage and returns Uri
    fun saveCroppedBitmap(context: Context, croppedBitmap: Bitmap): Uri? {
        try {
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val outFile = File(imagesDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            return Uri.fromFile(outFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cropped bitmap", e)
            return null
        }
    }

    // Copies media resource Uri into internal cache/image directory and returns provider Uri
    fun copyUriToCache(context: Context, sourceUri: Uri): Uri? {
        try {
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val uniqueFileName = "gallery_${System.currentTimeMillis()}.jpg"
            val galleryFile = File(imagesDir, uniqueFileName)
            
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(galleryFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.japanesegrammarapp.fileprovider",
                galleryFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to internal cache", e)
            return null
        }
    }
}
