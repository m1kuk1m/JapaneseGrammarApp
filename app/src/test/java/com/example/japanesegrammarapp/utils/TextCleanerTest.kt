package com.example.japanesegrammarapp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCleanerTest {
    @Test
    fun removeAccidentalSpaces_basicJapanese() {
        val input = "こ れ は 日本 語 です"
        val expected = "これは日本語です"
        assertEquals(expected, TextCleaner.removeAccidentalSpaces(input))
    }

    @Test
    fun removeAccidentalSpaces_cjkAndEnglish() {
        val input = "これは Apple Watch です"
        val expected = "これはApple Watchです"
        assertEquals(expected, TextCleaner.removeAccidentalSpaces(input))
    }

    @Test
    fun removeAccidentalSpaces_punctuationAndSymbols() {
        val input = "こんにちは 、 元気 です か ？"
        val expected = "こんにちは、元気ですか？"
        assertEquals(expected, TextCleaner.removeAccidentalSpaces(input))
    }

    @Test
    fun removeAccidentalSpaces_fullWidthSpaces() {
        val input = "今日　は　いい　天気　です"
        val expected = "今日はいい天気です"
        assertEquals("自然な日本語", expected, TextCleaner.removeAccidentalSpaces(input))
    }


    @Test
    fun removeAccidentalSpaces_cjkAndNumbers() {
        val input = "10 個 の りんご"
        val expected = "10個のりんご"
        assertEquals(expected, TextCleaner.removeAccidentalSpaces(input))
    }

    @Test
    fun removeAccidentalSpaces_onlyEnglish() {
        val input = "This is a pen."
        val expected = "This is a pen."
        assertEquals(expected, TextCleaner.removeAccidentalSpaces(input))
    }

    @Test
    fun removeAccidentalSpaces_trimSpaces() {
        val input = "　 こ れ は 　"
        val expected = "これは"
        assertEquals(expected, TextCleaner.removeAccidentalSpaces(input))
    }
}
