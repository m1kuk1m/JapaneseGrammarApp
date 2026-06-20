package com.example.japanesegrammarapp.domain.model

data class BookmarkedGrammarPointDomain(
    val id: Int = 0,
    val recordId: Int,
    val pattern: String,
    val explanation: String? = null,
    val bookmarkedAt: Long,
    val sourceText: String = "",
    val isArchived: Boolean = false
)
