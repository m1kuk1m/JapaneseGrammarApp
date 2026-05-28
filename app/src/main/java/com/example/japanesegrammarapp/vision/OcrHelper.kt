package com.example.japanesegrammarapp.vision

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.Closeable

class OcrHelper : Closeable {
    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    suspend fun extractTextFromUri(context: Context, uri: Uri): String {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            
            val blocks = result.textBlocks
            if (blocks.isEmpty()) return ""

            // Determine if the layout is primarily vertical (tategaki)
            var verticalCount = 0
            var horizontalCount = 0
            for (block in blocks) {
                for (line in block.lines) {
                    val box = line.boundingBox
                    if (box != null) {
                        if (box.height() > box.width()) {
                            verticalCount++
                        } else {
                            horizontalCount++
                        }
                    }
                }
            }
            val isVertical = verticalCount > horizontalCount

            // Sort TextBlocks
            val sortedBlocks = blocks.sortedWith(Comparator { b1, b2 ->
                val box1 = b1.boundingBox ?: return@Comparator 0
                val box2 = b2.boundingBox ?: return@Comparator 0

                if (isVertical) {
                    // Vertical Japanese: columns read right-to-left
                    val overlapStart = maxOf(box1.left, box2.left)
                    val overlapEnd = minOf(box1.right, box2.right)
                    val overlapWidth = overlapEnd - overlapStart
                    val minWidth = minOf(box1.width(), box2.width())

                    if (overlapWidth > minWidth * 0.5) {
                        // Same column: top-to-bottom
                        box1.top.compareTo(box2.top)
                    } else {
                        // Different columns: right-to-left
                        box2.centerX().compareTo(box1.centerX())
                    }
                } else {
                    // Horizontal text: rows read top-to-bottom, left-to-right
                    val overlapStart = maxOf(box1.top, box2.top)
                    val overlapEnd = minOf(box1.bottom, box2.bottom)
                    val overlapHeight = overlapEnd - overlapStart
                    val minHeight = minOf(box1.height(), box2.height())

                    if (overlapHeight > minHeight * 0.5) {
                        // Same row: left-to-right
                        box1.left.compareTo(box2.left)
                    } else {
                        // Different rows: top-to-bottom
                        box1.top.compareTo(box2.top)
                    }
                }
            })

            // Reconstruct text block by block, sorting lines within each block
            val reconstructedText = StringBuilder()
            for (block in sortedBlocks) {
                val sortedLines = block.lines.sortedWith(Comparator { l1, l2 ->
                    val box1 = l1.boundingBox ?: return@Comparator 0
                    val box2 = l2.boundingBox ?: return@Comparator 0

                    if (isVertical) {
                        val overlapStart = maxOf(box1.left, box2.left)
                        val overlapEnd = minOf(box1.right, box2.right)
                        val overlapWidth = overlapEnd - overlapStart
                        val minWidth = minOf(box1.width(), box2.width())

                        if (overlapWidth > minWidth * 0.5) {
                            box1.top.compareTo(box2.top)
                        } else {
                            box2.centerX().compareTo(box1.centerX())
                        }
                    } else {
                        val overlapStart = maxOf(box1.top, box2.top)
                        val overlapEnd = minOf(box1.bottom, box2.bottom)
                        val overlapHeight = overlapEnd - overlapStart
                        val minHeight = minOf(box1.height(), box2.height())

                        if (overlapHeight > minHeight * 0.5) {
                            box1.left.compareTo(box2.left)
                        } else {
                            box1.top.compareTo(box2.top)
                        }
                    }
                })

                for (line in sortedLines) {
                    reconstructedText.append(line.text).append("\n")
                }
                reconstructedText.append("\n")
            }

            reconstructedText.toString().trim()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error extracting text: ${e.localizedMessage}"
        }
    }

    override fun close() {
        recognizer.close()
    }
}
