package com.example.japanesegrammarapp.domain.repository

data class ImagePayload(
    val base64Data: String,
    val mimeType: String
)

interface ImageAttachmentLoader {
    suspend fun loadAsBase64(uriString: String): ImagePayload?
}
