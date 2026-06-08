package com.example.japanesegrammarapp.domain.model

data class BookmarkedSentenceDomain(
    val id: Int = 0,
    val recordId: Int,
    val originalText: String,
    val translation: String?,
    val analysisResult: String?,
    val modelUsed: String?,
    val bookmarkedAt: Long
)
