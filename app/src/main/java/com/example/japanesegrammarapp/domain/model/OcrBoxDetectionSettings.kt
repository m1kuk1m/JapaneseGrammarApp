package com.example.japanesegrammarapp.domain.model

enum class OcrBoxDetectorEngine {
    ML_KIT,
    RAPID_OCR,
    HYBRID,
    AUTO
}

enum class OcrBoxPreviewMode {
    FINAL,
    RAW
}

data class OcrBoxDetectionSettings(
    val detectorEngine: OcrBoxDetectorEngine = OcrBoxDetectorEngine.HYBRID,
    val previewMode: OcrBoxPreviewMode = OcrBoxPreviewMode.FINAL,
    val horizontalRowGapMultiplier: Float = DEFAULT_HORIZONTAL_ROW_GAP_MULTIPLIER,
    val horizontalXOverlapThreshold: Float = DEFAULT_HORIZONTAL_X_OVERLAP_THRESHOLD,
    val horizontalFillRatioMin: Float = DEFAULT_HORIZONTAL_FILL_RATIO_MIN,
    val verticalColumnGapMultiplier: Float = DEFAULT_VERTICAL_COLUMN_GAP_MULTIPLIER,
    val verticalXOverlapThreshold: Float = DEFAULT_VERTICAL_X_OVERLAP_THRESHOLD,
    val verticalFillRatioMin: Float = DEFAULT_VERTICAL_FILL_RATIO_MIN,
    val horizontalPaddingXRatio: Float = DEFAULT_HORIZONTAL_PADDING_X_RATIO,
    val horizontalPaddingYRatio: Float = DEFAULT_HORIZONTAL_PADDING_Y_RATIO,
    val verticalPaddingXRatio: Float = DEFAULT_VERTICAL_PADDING_X_RATIO,
    val verticalPaddingYRatio: Float = DEFAULT_VERTICAL_PADDING_Y_RATIO,
    val rapidOcrInputLongSide: Int = DEFAULT_RAPID_OCR_INPUT_LONG_SIDE,
    val rapidOcrDetThreshold: Float = DEFAULT_RAPID_OCR_DET_THRESHOLD,
    val rapidOcrBoxThreshold: Float = DEFAULT_RAPID_OCR_BOX_THRESHOLD,
    val rapidOcrUnclipRatio: Float = DEFAULT_RAPID_OCR_UNCLIP_RATIO
) {
    fun normalized(): OcrBoxDetectionSettings {
        return copy(
            horizontalRowGapMultiplier = horizontalRowGapMultiplier.coerceIn(MERGE_GAP_MIN, MERGE_GAP_MAX),
            horizontalXOverlapThreshold = horizontalXOverlapThreshold.coerceIn(THRESHOLD_MIN, THRESHOLD_MAX),
            horizontalFillRatioMin = horizontalFillRatioMin.coerceIn(THRESHOLD_MIN, FILL_RATIO_MAX),
            verticalColumnGapMultiplier = verticalColumnGapMultiplier.coerceIn(MERGE_GAP_MIN, MERGE_GAP_MAX),
            verticalXOverlapThreshold = verticalXOverlapThreshold.coerceIn(THRESHOLD_MIN, THRESHOLD_MAX),
            verticalFillRatioMin = verticalFillRatioMin.coerceIn(THRESHOLD_MIN, FILL_RATIO_MAX),
            horizontalPaddingXRatio = horizontalPaddingXRatio.coerceIn(PADDING_MIN, PADDING_MAX),
            horizontalPaddingYRatio = horizontalPaddingYRatio.coerceIn(PADDING_MIN, PADDING_MAX),
            verticalPaddingXRatio = verticalPaddingXRatio.coerceIn(PADDING_MIN, PADDING_MAX),
            verticalPaddingYRatio = verticalPaddingYRatio.coerceIn(PADDING_MIN, PADDING_MAX),
            rapidOcrInputLongSide = rapidOcrInputLongSide.coerceIn(RAPID_OCR_INPUT_LONG_SIDE_MIN, RAPID_OCR_INPUT_LONG_SIDE_MAX),
            rapidOcrDetThreshold = rapidOcrDetThreshold.coerceIn(THRESHOLD_MIN, THRESHOLD_MAX),
            rapidOcrBoxThreshold = rapidOcrBoxThreshold.coerceIn(THRESHOLD_MIN, THRESHOLD_MAX),
            rapidOcrUnclipRatio = rapidOcrUnclipRatio.coerceIn(RAPID_OCR_UNCLIP_MIN, RAPID_OCR_UNCLIP_MAX)
        )
    }

    companion object {
        const val MERGE_GAP_MIN = 0.1f
        const val MERGE_GAP_MAX = 5.0f
        const val THRESHOLD_MIN = 0.01f
        const val THRESHOLD_MAX = 0.98f
        const val FILL_RATIO_MAX = 0.95f
        const val PADDING_MIN = 0f
        const val PADDING_MAX = 0.6f
        const val RAPID_OCR_INPUT_LONG_SIDE_MIN = 480
        const val RAPID_OCR_INPUT_LONG_SIDE_MAX = 2240
        const val RAPID_OCR_UNCLIP_MIN = 0.8f
        const val RAPID_OCR_UNCLIP_MAX = 5.0f

        const val DEFAULT_HORIZONTAL_ROW_GAP_MULTIPLIER = 1.25f
        const val DEFAULT_HORIZONTAL_X_OVERLAP_THRESHOLD = 0.52f
        const val DEFAULT_HORIZONTAL_FILL_RATIO_MIN = 0.34f
        const val DEFAULT_VERTICAL_COLUMN_GAP_MULTIPLIER = 1.8f
        const val DEFAULT_VERTICAL_X_OVERLAP_THRESHOLD = 0.40f
        const val DEFAULT_VERTICAL_FILL_RATIO_MIN = 0.30f
        const val DEFAULT_HORIZONTAL_PADDING_X_RATIO = 0.12f
        const val DEFAULT_HORIZONTAL_PADDING_Y_RATIO = 0.10f
        const val DEFAULT_VERTICAL_PADDING_X_RATIO = 0.16f
        const val DEFAULT_VERTICAL_PADDING_Y_RATIO = 0.10f
        const val DEFAULT_RAPID_OCR_INPUT_LONG_SIDE = 960
        const val DEFAULT_RAPID_OCR_DET_THRESHOLD = 0.30f
        const val DEFAULT_RAPID_OCR_BOX_THRESHOLD = 0.50f
        const val DEFAULT_RAPID_OCR_UNCLIP_RATIO = 1.60f

        val DEFAULT = OcrBoxDetectionSettings()
    }
}
