package com.example.japanesegrammarapp.domain.model

data class BookmarkedSegmentDomain(
    val id: Int = 0,
    val recordId: Int,
    /** The dictionary/citation form used as the bookmark key */
    val segmentText: String,
    /** The original surface form as it appeared in the sentence */
    val surfaceForm: String? = null,
    val reading: String? = null,
    val partOfSpeech: String? = null,
    val posCategory: String? = null,
    val dictionaryForm: String? = null,
    val dictionaryFormReading: String? = null,
    val meaning: String? = null,
    val inflection: String? = null,
    val role: String? = null,
    val bookmarkedAt: Long = System.currentTimeMillis(),
    val sourceText: String = "",
    val isArchived: Boolean = false
)

val BookmarkedSegmentDomain.effectivePosCategory: String
    get() {
        val cat = posCategory ?: ""
        if (cat.isNotBlank() && cat != "OTHER") return cat
        val pos = partOfSpeech ?: ""
        val primaryPos = pos.split("-").firstOrNull() ?: ""
        return when {
            primaryPos.contains("助動詞") -> "AUXILIARY"
            primaryPos.contains("形容") || primaryPos.contains("形状") -> "ADJECTIVE"
            primaryPos.contains("名詞") -> "NOUN"
            primaryPos.contains("動詞") -> "VERB"
            primaryPos.contains("助詞") -> "PARTICLE"
            primaryPos.contains("副詞") -> "ADVERB"
            primaryPos.contains("接続詞") -> "CONJUNCTION"
            primaryPos.contains("代名詞") -> "PRONOUN"
            primaryPos.contains("感動詞") -> "INTERJECTION"
            primaryPos.contains("連体詞") -> "PRE_NOUN_ADJECTIVAL"
            primaryPos.contains("記号") || primaryPos.contains("補助記号") -> "SYMBOL"
            else -> "OTHER"
        }
    }
