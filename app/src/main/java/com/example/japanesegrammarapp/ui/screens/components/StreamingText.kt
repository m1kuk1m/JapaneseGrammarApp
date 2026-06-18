package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay

@Composable
fun StreamingText(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current
) {
    if (!isStreaming) {
        // Skip animation for historical records
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
            style = style
        )
        return
    }

    var displayedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(text) {
        val targetLength = text.length
        if (targetLength > displayedCount) {
            val diff = targetLength - displayedCount
            // Dynamic delay: faster if there's a big chunk, slower for small updates
            val delayPerChar = when {
                diff > 100 -> 1L
                diff > 50 -> 3L
                diff > 20 -> 8L
                else -> 15L
            }
            
            for (i in displayedCount until targetLength) {
                displayedCount = i + 1
                if (delayPerChar > 0) {
                    delay(delayPerChar)
                }
            }
        } else if (targetLength < displayedCount) {
            // Handle edge case where text might be reset
            displayedCount = targetLength
        }
    }

    Text(
        text = text.take(displayedCount),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        style = style
    )
}
