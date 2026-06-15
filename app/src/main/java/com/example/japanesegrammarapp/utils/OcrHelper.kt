package com.example.japanesegrammarapp.utils

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.tasks.await

object OcrHelper {
    suspend fun processBitmap(bitmap: Bitmap): List<Rect> {
        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = recognizer.process(image).await()
            val boxes = mutableListOf<Rect>()
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    line.boundingBox?.let { boxes.add(it) }
                }
            }
            boxes
        } catch (e: Exception) {
            emptyList()
        } finally {
            recognizer.close()
        }
    }
}