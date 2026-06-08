package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.AnalysisEvent
import com.example.japanesegrammarapp.domain.repository.ImageAttachmentLoader
import com.example.japanesegrammarapp.domain.repository.OcrRepository
import javax.inject.Inject

data class OcrPayload(
    val base64Data: String?,
    val mimeType: String?
)

data class OcrResult(
    val ocrText: String,
    val isOcrMode: Boolean,
    val imagePayload: OcrPayload?
)

class GetOcrTextUseCase @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val imageLoader: ImageAttachmentLoader,
    private val eventBus: AnalysisEventBus
) {
    suspend fun execute(
        text: String,
        imageUri: String?,
        isOcrEnabled: Boolean,
        recordId: Int
    ): OcrResult {
        var isOcrMode = isOcrEnabled && !imageUri.isNullOrBlank()
        var imageBase64: String? = null
        var mimeType: String? = "image/jpeg"

        if (!isOcrMode && !imageUri.isNullOrBlank()) {
            val payload = imageLoader.loadAsBase64(imageUri)
            if (payload != null) {
                imageBase64 = payload.base64Data
                mimeType = payload.mimeType
            }
        }

        var ocrText = text
        if (isOcrMode) {
            if (ocrText.isBlank()) {
                try {
                    ocrText = ocrRepository.extractTextFromImage(imageUri!!)
                } catch (e: Exception) {
                    ocrText = ""
                }
            }

            if (ocrText.isBlank()) {
                isOcrMode = false
                eventBus.post(AnalysisEvent.OcrFallbackTriggered(recordId))

                if (!imageUri.isNullOrBlank()) {
                    val payload = imageLoader.loadAsBase64(imageUri)
                    if (payload != null) {
                        imageBase64 = payload.base64Data
                        mimeType = payload.mimeType
                    }
                }
            }
        }

        return OcrResult(
            ocrText = ocrText,
            isOcrMode = isOcrMode,
            imagePayload = if (imageBase64 != null) OcrPayload(imageBase64, mimeType) else null
        )
    }
}
