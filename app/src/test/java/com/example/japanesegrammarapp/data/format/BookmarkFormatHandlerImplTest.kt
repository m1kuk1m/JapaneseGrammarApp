package com.example.japanesegrammarapp.data.format

import com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.ExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkFormatHandlerImplTest {
    private val handler = BookmarkFormatHandlerImpl()

    @Test
    fun jsonRoundTripPreservesArchivedSentenceAndOrphanFavorites() {
        val json = handler.exportData(
            format = ExportFormat.JSON,
            words = listOf(
                BookmarkedSegmentDomain(
                    recordId = -1,
                    segmentText = "食べる",
                    surfaceForm = "食べる",
                    reading = "たべる",
                    meaning = "eat",
                    bookmarkedAt = 1000L,
                    sourceText = "ご飯を食べる"
                )
            ),
            sentences = listOf(
                BookmarkedSentenceDomain(
                    recordId = -1,
                    originalText = "ご飯を食べる",
                    translation = "eat rice",
                    analysisResult = null,
                    modelUsed = null,
                    bookmarkedAt = 1001L,
                    isArchived = true
                )
            ),
            grammarPoints = listOf(
                BookmarkedGrammarPointDomain(
                    recordId = -1,
                    pattern = "〜を",
                    explanation = "object marker",
                    bookmarkedAt = 1002L,
                    sourceText = "ご飯を食べる",
                    isArchived = true
                )
            )
        )

        val parsed = handler.importData(json, ExportFormat.JSON)

        assertEquals(-1, parsed.words.single().recordId)
        assertEquals("食べる", parsed.words.single().segmentText)
        assertTrue(parsed.sentences.single().isArchived)
        assertTrue(parsed.grammarPoints.single().isArchived)
    }

    @Test
    fun csvImportHandlesQuotedCommasAndMultilineFields() {
        val csv = """
            Type,Front,Back,Extra1,Extra2
            Word,"食べる, 飲む","eat
            drink",たべる,"ご飯を食べる"
            Sentence,"これは文です","This, is a sentence",,
            Grammar,"〜ている","ongoing, state","本を読んでいる",
        """.trimIndent()

        val parsed = handler.importData(csv, ExportFormat.CSV)

        assertEquals("食べる, 飲む", parsed.words.single().segmentText)
        assertEquals("eat\ndrink", parsed.words.single().meaning)
        assertEquals("This, is a sentence", parsed.sentences.single().translation)
        assertEquals("ongoing, state", parsed.grammarPoints.single().explanation)
        assertEquals(emptyList<String>(), parsed.failureReasons)
    }
}
