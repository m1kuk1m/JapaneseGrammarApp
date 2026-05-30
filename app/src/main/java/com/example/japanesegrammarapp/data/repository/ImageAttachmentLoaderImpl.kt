package com.example.japanesegrammarapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.japanesegrammarapp.domain.repository.ImageAttachmentLoader
import com.example.japanesegrammarapp.domain.repository.ImagePayload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageAttachmentLoaderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageAttachmentLoader {
    
    override suspend fun loadAsBase64(uriString: String): ImagePayload? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            ImagePayload(base64Data, mimeType)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
