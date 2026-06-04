package com.example.japanesegrammarapp.domain.model

data class BookmarkedSegmentDomain(
    val id: Int = 0,
    val recordId: Int,
    val segmentText: String,
    val reading: String? = null,
    val partOfSpeech: String? = null,
    val posCategory: String? = null,
    val dictionaryForm: String? = null,
    val dictionaryFormReading: String? = null,
    val meaning: String? = null,
    val inflection: String? = null,
    val role: String? = null,
    val bookmarkedAt: Long = System.currentTimeMillis(),
    val sourceText: String = ""
)
