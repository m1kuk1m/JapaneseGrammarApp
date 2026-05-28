package com.example.japanesegrammarapp.network

data class WordSegment(
    val text: String,
    val reading: String,
    val partOfSpeech: String,
    val dictionaryForm: String? = null,
    val meaning: String,
    val inflection: String? = null,
    val role: String
)

data class SentenceClause(
    val index: Int,
    val role: String,
    val text: String,
    val explanation: String
)

data class DetailedGrammarPoint(
    val pattern: String,
    val explanation: String
)

data class DetailedAnalysisResult(
    val translation: String,
    val segments: List<WordSegment>,
    val clauses: List<SentenceClause>,
    val grammarPoints: List<DetailedGrammarPoint>
)
