package com.example.japanesegrammarapp.network

import org.junit.Assert.assertTrue
import org.junit.Test

class PromptManagerTest {
    @Test
    fun imageTokenizerPromptsMentionVerticalReadingOrder() {
        assertTrue(PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE.contains("縦書き（文字が縦に積まれている）の場合"))
        assertTrue(PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE.contains("横書き（文字が横に並んでいる）の場合"))
        assertTrue(PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE_REPAIR.contains("縦書き（文字が縦に積まれている）の場合"))
        assertTrue(PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE_REPAIR.contains("横書き（文字が横に並んでいる）の場合"))
    }
}
