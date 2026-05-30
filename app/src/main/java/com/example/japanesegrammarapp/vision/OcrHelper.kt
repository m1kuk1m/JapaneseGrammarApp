package com.example.japanesegrammarapp.vision

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrHelper @Inject constructor() {
    suspend fun extractTextFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            
            val blocks = result.textBlocks
            if (blocks.isEmpty()) return@withContext ""

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

            val rawExtracted = reconstructedText.toString().trim()
            formatOcrText(rawExtracted)
        } catch (e: Exception) {
            e.printStackTrace()
            "Error extracting text: ${e.localizedMessage}"
        } finally {
            recognizer.close()
        }
    }

    private fun formatOcrText(rawText: String): String {
        if (rawText.isBlank()) return ""
        
        val lines = rawText.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        if (lines.isEmpty()) return ""
        
        val sb = StringBuilder()
        for (i in lines.indices) {
            val currentLine = lines[i]
            if (sb.isEmpty()) {
                sb.append(currentLine)
            } else {
                val prevChar = sb.last()
                val nextChar = currentLine.first()
                
                val isPrevCjk = isCjk(prevChar)
                val isNextCjk = isCjk(nextChar)
                
                if (isPrevCjk || isNextCjk) {
                    sb.append(currentLine)
                } else {
                    sb.append(" ").append(currentLine)
                }
            }
        }
        
        val cleanedText = sb.toString()
        val finalSb = StringBuilder()
        var j = 0
        while (j < cleanedText.length) {
            val c = cleanedText[j]
            if (c == ' ' || c == '　') {
                val prev = if (j > 0) cleanedText[j - 1] else null
                val next = if (j < cleanedText.length - 1) cleanedText[j + 1] else null
                
                if (prev != null && next != null && (isCjk(prev) || isCjk(next))) {
                    // Skip this space (remove it)
                } else {
                    finalSb.append(c)
                }
            } else {
                finalSb.append(c)
            }
            j++
        }
        
        return finalSb.toString().replace("\\s+".toRegex(), " ").trim()
    }

    private fun isCjk(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
               block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
               block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
               block == Character.UnicodeBlock.HIRAGANA ||
               block == Character.UnicodeBlock.KATAKANA ||
               block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||
               block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
               block == Character.UnicodeBlock.HANGUL_JAMO ||
               block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
    }
}
