package com.example.japanesegrammarapp.data.mapper

import com.example.japanesegrammarapp.data.BookmarkedGrammarPoint
import com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain

fun BookmarkedGrammarPoint.toDomain(): BookmarkedGrammarPointDomain {
    return BookmarkedGrammarPointDomain(
        id = id,
        recordId = recordId,
        pattern = pattern,
        explanation = explanation,
        bookmarkedAt = bookmarkedAt,
        sourceText = sourceText,
        isArchived = isArchived
    )
}

fun BookmarkedGrammarPointDomain.toEntity(): BookmarkedGrammarPoint {
    return BookmarkedGrammarPoint(
        id = id,
        recordId = recordId,
        pattern = pattern,
        explanation = explanation,
        bookmarkedAt = bookmarkedAt,
        sourceText = sourceText,
        isArchived = isArchived
    )
}
