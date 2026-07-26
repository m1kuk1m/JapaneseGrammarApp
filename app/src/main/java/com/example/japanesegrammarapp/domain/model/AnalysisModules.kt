package com.example.japanesegrammarapp.domain.model

/**
 * 分析パイプラインのモジュール定義(全コードベース唯一の情報源)。
 * 編集可能なプロンプト1件につきモジュール1件。id は prefs キー・APIログの
 * apiTypeLabel として保存される安定した ASCII 識別子で、UI 表示名とは分離されている。
 */
enum class AnalysisModule(
    val id: String,
    val promptKey: String,
    /** 旧バージョンの prefs キー接尾辞(移行時の読み取りフォールバック用) */
    val legacySlug: String
) {
    TOKENIZER_TEXT("tokenizer_text", "prompt_tokenizer", "word_segmentation"),
    TOKENIZER_OCR("tokenizer_ocr", "prompt_tokenizer_ocr", "word_segmentation"),
    TOKENIZER_IMAGE("tokenizer_image", "prompt_tokenizer_image", "word_segmentation"),
    TOKENIZER_IMAGE_REPAIR("tokenizer_image_repair", "prompt_tokenizer_image_repair", "word_segmentation"),
    SEGMENTS("segments", "prompt_segments", "detailed_analysis"),
    TRANSLATION("translation", "prompt_translation", "translation"),
    CLAUSES("clauses", "prompt_clauses", "clause_analysis"),
    GRAMMAR("grammar", "prompt_grammar", "grammar_explanation");

    val isTokenizer: Boolean
        get() = this == TOKENIZER_TEXT || this == TOKENIZER_OCR ||
            this == TOKENIZER_IMAGE || this == TOKENIZER_IMAGE_REPAIR

    companion object {
        fun fromId(id: String): AnalysisModule? = entries.find { it.id == id }

        /** 旧 apiTypeLabel(日本語)からの解決。旧ログ表示・互換用。 */
        fun fromLegacyLabel(label: String): AnalysisModule? = when (label) {
            "単語分割" -> TOKENIZER_TEXT
            "翻訳" -> TRANSLATION
            "文節解析" -> CLAUSES
            "文法解説" -> GRAMMAR
            "詳細文法解析" -> SEGMENTS
            else -> null
        }

        /**
         * 入力モードから tokenizer 変種を解決する。
         * LlmAnalysisServiceImpl.executeTokenizer のプロンプト選択と同一条件を共有する。
         */
        fun tokenizerVariant(
            imageBase64: String?,
            isOcrMode: Boolean,
            imageTokenizerMode: String
        ): AnalysisModule = when {
            isOcrMode -> TOKENIZER_OCR
            imageBase64 != null && imageTokenizerMode == "repair" -> TOKENIZER_IMAGE_REPAIR
            imageBase64 != null -> TOKENIZER_IMAGE
            else -> TOKENIZER_TEXT
        }
    }
}
