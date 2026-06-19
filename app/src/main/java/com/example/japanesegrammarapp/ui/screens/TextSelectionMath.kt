package com.example.japanesegrammarapp.ui.screens

import android.graphics.Rect

data class TextHandleState(val index: Int, val offsetRatio: Float)

fun getOrderedHandles(h1: TextHandleState, h2: TextHandleState): Pair<TextHandleState, TextHandleState> {
    return if (h1.index != h2.index) {
        if (h1.index < h2.index) Pair(h1, h2) else Pair(h2, h1)
    } else {
        if (h1.offsetRatio <= h2.offsetRatio) Pair(h1, h2) else Pair(h2, h1)
    }
}

fun calculateSubLineRects(
    boxes: List<Rect>,
    start: TextHandleState,
    end: TextHandleState
): List<Rect> {
    val result = mutableListOf<Rect>()
    
    for (i in start.index..end.index) {
        if (i < 0 || i >= boxes.size) continue
        
        val box = boxes[i]
        val isVertical = box.height() > box.width()
        
        var newLeft = box.left
        var newTop = box.top
        var newRight = box.right
        var newBottom = box.bottom
        
        val width = box.width()
        val height = box.height()
        
        if (i == start.index) {
            if (isVertical) {
                newTop = (box.top + height * start.offsetRatio).toInt()
            } else {
                newLeft = (box.left + width * start.offsetRatio).toInt()
            }
        }
        
        if (i == end.index) {
            if (isVertical) {
                newBottom = (box.top + height * end.offsetRatio).toInt()
            } else {
                newRight = (box.left + width * end.offsetRatio).toInt()
            }
        }
        
        if (newLeft < newRight && newTop < newBottom) {
            result.add(Rect(newLeft, newTop, newRight, newBottom))
        }
    }
    
    return result
}
