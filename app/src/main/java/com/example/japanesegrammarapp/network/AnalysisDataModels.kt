package com.example.japanesegrammarapp.network

data class WordSegment(
    val text: String? = null,
    val reading: String? = null,
    val partOfSpeech: String? = null,
    val posCategory: String? = null,
    val dictionaryForm: String? = null,
    val meaning: String? = null,
    val inflection: String? = null,
    val role: String? = null
)

data class SentenceClause(
    val index: Int? = null,
    val role: String? = null,
    val text: String? = null,
    val explanation: String? = null
)

data class DetailedGrammarPoint(
    val pattern: String? = null,
    val explanation: String? = null
)

data class DetailedAnalysisResult(
    val translation: String? = null,
    val segments: List<WordSegment>? = null,
    val clauses: List<SentenceClause>? = null,
    val grammarPoints: List<DetailedGrammarPoint>? = null
)
