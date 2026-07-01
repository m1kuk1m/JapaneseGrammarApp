package com.example.japanesegrammarapp.network

import org.junit.Assert.assertTrue
import org.junit.Test

class PromptManagerTest {
    @Test
    fun imageTokenizerPromptsMentionVerticalReadingOrder() {
        assertTrue(PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE.contains("縦書きは列を右→左、列内を上→下に読む"))
        assertTrue(PromptManager.SYSTEM_PROMPT_TOKENIZER_IMAGE_REPAIR.contains("縦書きは列を右→左、列内を上→下に読んだうえで"))
    }
}
