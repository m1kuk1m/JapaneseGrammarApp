package com.example.japanesegrammarapp.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisModule

/**
 * モジュール ID → UI 表示名のマッピング。
 * 表示名の基準はメイン画面(分析結果)のセクション名。
 */
@StringRes
fun AnalysisModule.displayNameRes(): Int = when (this) {
    AnalysisModule.TOKENIZER_TEXT -> R.string.module_tokenizer_text
    AnalysisModule.TOKENIZER_OCR -> R.string.module_tokenizer_ocr
    AnalysisModule.TOKENIZER_IMAGE -> R.string.module_tokenizer_image
    AnalysisModule.TOKENIZER_IMAGE_REPAIR -> R.string.module_tokenizer_image_repair
    AnalysisModule.SEGMENTS -> R.string.module_segments
    AnalysisModule.TRANSLATION -> R.string.overall_translation
    AnalysisModule.CLAUSES -> R.string.sentence_clauses
    AnalysisModule.GRAMMAR -> R.string.grammar_points
}

/**
 * apiTypeLabel(新形式の ID または旧形式の日本語ラベル)をローカライズ済み表示名へ解決する。
 * 未知の値はそのまま返す(旧ログ互換)。
 */
@Composable
fun moduleDisplayName(apiTypeLabel: String): String {
    val module = AnalysisModule.fromId(apiTypeLabel)
        ?: AnalysisModule.fromLegacyLabel(apiTypeLabel)
        ?: return apiTypeLabel
    return stringResource(module.displayNameRes())
}

/** 非 Compose 文脈(エクスポート等)向けの Context ベース解決。 */
fun moduleDisplayName(context: android.content.Context, apiTypeLabel: String): String {
    val module = AnalysisModule.fromId(apiTypeLabel)
        ?: AnalysisModule.fromLegacyLabel(apiTypeLabel)
        ?: return apiTypeLabel
    return context.getString(module.displayNameRes())
}
