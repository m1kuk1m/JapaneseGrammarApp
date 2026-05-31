package com.example.japanesegrammarapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.japanesegrammarapp.domain.repository.ImageAttachmentLoader
import com.example.japanesegrammarapp.domain.repository.ImagePayload
import com.example.japanesegrammarapp.utils.BitmapHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageAttachmentLoaderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageAttachmentLoader {

    private companion object {
        const val MAX_UPLOAD_DIMENSION = 1024
        const val JPEG_QUALITY = 75
    }
    
    override suspend fun loadAsBase64(uriString: String): ImagePayload? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            // Use BitmapHelper to safely downsample and rotate the image according to EXIF
            val bitmap = BitmapHelper.loadRotatedBitmapFromUri(context, uri) ?: return@withContext null
            val uploadBitmap = resizeForUpload(bitmap)
            
            // Compress to a bounded JPEG payload so mobile networks and Gemini gateways are less likely to close the stream.
            val outputStream = ByteArrayOutputStream()
            uploadBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val bytes = outputStream.toByteArray()
            if (uploadBitmap != bitmap) {
                uploadBitmap.recycle()
            }
            bitmap.recycle()
            
            val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            // Since we compress the bitmap to JPEG, the MIME type will always be image/jpeg
            ImagePayload(base64Data, "image/jpeg")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resizeForUpload(bitmap: Bitmap): Bitmap {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        if (longestSide <= MAX_UPLOAD_DIMENSION) {
            return bitmap
        }

        val scale = MAX_UPLOAD_DIMENSION.toFloat() / longestSide
        val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
