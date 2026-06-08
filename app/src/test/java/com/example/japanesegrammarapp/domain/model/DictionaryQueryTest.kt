package com.example.japanesegrammarapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryQueryTest {
    @Test
    fun dictionaryQueryWord_prefersReadingThenDictionaryFormThenText() {
        assertEquals(
            "よみ",
            WordSegment(
                text = "表層",
                dictionaryForm = "辞書形",
                dictionaryFormReading = "よみ"
            ).dictionaryQueryWord()
        )

        assertEquals(
            "辞書形",
            WordSegment(
                text = "表層",
                dictionaryForm = "辞書形"
            ).dictionaryQueryWord()
        )

        assertEquals("表層", WordSegment(text = "表層").dictionaryQueryWord())
    }

    @Test
    fun dictionaryQueryWord_trimsAdjectivalNounEndings() {
        assertEquals(
            "静か",
            WordSegment(dictionaryForm = "静かだ", partOfSpeech = "形容動詞").dictionaryQueryWord()
        )
        assertEquals(
            "静か",
            WordSegment(dictionaryForm = "静かな", partOfSpeech = "形状詞").dictionaryQueryWord()
        )
        assertEquals(
            "静か",
            WordSegment(dictionaryForm = "静かに", partOfSpeech = "形容動詞").dictionaryQueryWord()
        )
        assertEquals(
            "静か",
            WordSegment(dictionaryForm = "静かで", partOfSpeech = "形容動詞").dictionaryQueryWord()
        )
    }

    @Test
    fun dictionaryQueryWord_trimsGeneralDaAndSuruSuffixes() {
        assertEquals(
            "学生",
            WordSegment(dictionaryForm = "学生だ", partOfSpeech = "名詞").dictionaryQueryWord()
        )
        assertEquals(
            "勉強",
            WordSegment(dictionaryForm = "勉強する", partOfSpeech = "動詞").dictionaryQueryWord()
        )
        assertEquals(
            "する",
            WordSegment(dictionaryForm = "する", partOfSpeech = "動詞").dictionaryQueryWord()
        )
    }
}
