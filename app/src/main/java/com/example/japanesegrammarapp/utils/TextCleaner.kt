package com.example.japanesegrammarapp.utils

object TextCleaner {
    /**
     * Removes spaces that are adjacent to a CJK character (Japanese Kanji/Hiragana/Katakana/Punctuation),
     * while preserving spaces between English words/numbers.
     */
    fun removeAccidentalSpaces(text: String): String {
        if (text.isBlank()) return text

        // Trim leading and trailing whitespaces (both half-width and full-width spaces)
        var cleaned = text.replace(Regex("^[\\s\\u3000]+|[\\s\\u3000]+$"), "")

        // CJK character ranges (excluding full-width space \u3000)
        // Han: \u4E00-\u9FFF, \u3400-\u4DBF
        // Hiragana: \u3040-\u309F
        // Katakana: \u30A0-\u30FF
        // CJK Symbols/Punctuation (excluding \u3000): \u3001-\u303F
        // Full-width ASCII/Punctuation: \uFF01-\uFFEE
        val cjkRegex = """[\u4E00-\u9FFF\u3400-\u4DBF\u3040-\u309F\u30A0-\u30FF\u3001-\u303F\uFF01-\uFFEE]"""

        // Replace any sequence of half-width spaces, tabs, or full-width spaces
        val spaceRegex = "[ \\t\\u3000]+"

        // 1. Remove spaces preceded by a CJK character
        cleaned = cleaned.replace(Regex("($cjkRegex)$spaceRegex"), "$1")

        // 2. Remove spaces followed by a CJK character
        cleaned = cleaned.replace(Regex("$spaceRegex($cjkRegex)"), "$1")

        return cleaned
    }
}
