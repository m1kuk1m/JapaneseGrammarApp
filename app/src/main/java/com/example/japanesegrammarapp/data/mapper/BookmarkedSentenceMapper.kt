package com.example.japanesegrammarapp.data.mapper

import com.example.japanesegrammarapp.data.BookmarkedSentence
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain

fun BookmarkedSentence.toDomain(): BookmarkedSentenceDomain {
    return BookmarkedSentenceDomain(
        id = id,
        recordId = recordId,
        originalText = originalText,
        translation = translation,
        analysisResult = analysisResult,
        modelUsed = modelUsed,
        bookmarkedAt = bookmarkedAt
    )
}

fun BookmarkedSentenceDomain.toEntity(): BookmarkedSentence {
    return BookmarkedSentence(
        id = id,
        recordId = recordId,
        originalText = originalText,
        translation = translation,
        analysisResult = analysisResult,
        modelUsed = modelUsed,
        bookmarkedAt = bookmarkedAt
    )
}
