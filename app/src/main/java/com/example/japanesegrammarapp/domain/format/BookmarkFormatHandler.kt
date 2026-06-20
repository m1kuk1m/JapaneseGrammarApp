package com.example.japanesegrammarapp.domain.format

import com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.ExportFormat

data class ParsedBookmarks(
    val words: List<BookmarkedSegmentDomain>,
    val sentences: List<BookmarkedSentenceDomain>,
    val grammarPoints: List<BookmarkedGrammarPointDomain>,
    val failureReasons: List<String>
)

interface BookmarkFormatHandler {
    fun exportData(
        format: ExportFormat,
        words: List<BookmarkedSegmentDomain>,
        sentences: List<BookmarkedSentenceDomain>,
        grammarPoints: List<BookmarkedGrammarPointDomain>
    ): String

    fun importData(
        data: String,
        format: ExportFormat
    ): ParsedBookmarks
}
