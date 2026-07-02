package com.example.japanesegrammarapp.domain.model

data class TokenizationResult(
    val tokens: List<String>? = null,
    val correctedText: String? = null,
    val recognizedText: String? = null
)

data class WordSegment(
    val text: String? = null,
    val reading: String? = null,
    val partOfSpeech: String? = null,
    val posCategory: String? = null,
    val dictionaryForm: String? = null,
    val dictionaryFormReading: String? = null,
    val meaning: String? = null,
    val inflection: String? = null,
    val role: String? = null
)

/**
 * Single source of truth for "is this segment a punctuation / symbol card".
 *
 * Primary signal is the analyzer's POS tag: the analysis layer tags punctuation
 * with partOfSpeech = "補助記号" (covers both 記号 and 補助記号) and posCategory = "OTHER".
 * We also accept posCategory == "symbol" (any case) as a fallback for other producers.
 *
 * If the POS is missing/ambiguous, fall back to inspecting the surface text: if every
 * character is a known punctuation/symbol char, treat it as punctuation. This char set
 * is the union of what the UI and the two TTS paths used to check separately, so all
 * three now agree.
 */
private val PUNCTUATION_CHARS = setOf(
    '。', '、', '・', '？', '！', '「', '」', '『', '』', '（', '）',
    '〔', '〕', '［', '］', '｛', '｝', '〜', '～', '…', '：', '；', '―', '—',
    '【', '】', '《', '》', '〈', '〉', '〝', '〟', '，', '．',
    '?', '!', '(', ')', '[', ']', '{', '}', ':', ';', ',', '.', '~', '-', '_',
    '/', '\\', '|', '<', '>', '"', '\''
)

fun WordSegment.isPunctuation(): Boolean {
    if (partOfSpeech?.contains("記号") == true) return true
    if (posCategory?.equals("symbol", ignoreCase = true) == true) return true
    val text = text?.trim() ?: return false
    if (text.isEmpty()) return false
    return text.all { it in PUNCTUATION_CHARS }
}

/**
 * Maps a partOfSpeech string (e.g. "動詞-一般") to the coarse category used for
 * chip colors and grouping (NOUN, VERB, PARTICLE, ...). Single source of truth —
 * both the chip renderer and the edit-save paths call this so that editing a
 * segment's partOfSpeech always yields a consistent posCategory.
 */
fun derivePosCategory(partOfSpeech: String?): String {
    val pos = partOfSpeech ?: ""
    val primaryPos = pos.split("-").firstOrNull() ?: ""
    return when {
        primaryPos.contains("代名詞") -> "PRONOUN"
        primaryPos.contains("感動詞") -> "INTERJECTION"
        primaryPos.contains("助動詞") -> "AUXILIARY"
        primaryPos.contains("形容") || primaryPos.contains("形状") -> "ADJECTIVE"
        primaryPos.contains("名詞") || primaryPos.contains("数詞") -> "NOUN"
        primaryPos.contains("動詞") -> "VERB"
        primaryPos.contains("助詞") -> "PARTICLE"
        primaryPos.contains("副詞") || primaryPos.contains("擬態語") -> "ADVERB"
        primaryPos.contains("接続詞") -> "CONJUNCTION"
        primaryPos.contains("連体詞") -> "PRE_NOUN_ADJECTIVAL"
        primaryPos.contains("記号") -> "SYMBOL"
        primaryPos.contains("接尾辞") || primaryPos.contains("接頭辞") -> "AFFIX"
        primaryPos.contains("連語") || primaryPos.contains("慣用句") -> "PHRASE"
        else -> "OTHER"
    }
}

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
    val grammarPoints: List<DetailedGrammarPoint>? = null,
    var consumedTokens: Int = 0,
    var inputTokens: Int = 0,
    var outputTokens: Int = 0
)
