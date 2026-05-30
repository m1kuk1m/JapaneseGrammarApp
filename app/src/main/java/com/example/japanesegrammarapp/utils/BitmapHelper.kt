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

    // Loads a photo from file and corrects rotation according to EXIF
    fun loadRotatedBitmap(file: File): Bitmap? {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
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
            
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading rotated bitmap from file", e)
            return null
        }
    }

    // Load rotated bitmap from ContentProvider Uri (Gallery selection)
    fun loadRotatedBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                
                // Check rotation using EXIF
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
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (e: Exception) {
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
