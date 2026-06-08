package com.example.japanesegrammarapp.domain.model

data class OcrBoxDetectionSettings(
    val horizontalRowGapMultiplier: Float = DEFAULT_HORIZONTAL_ROW_GAP_MULTIPLIER,
    val horizontalXOverlapThreshold: Float = DEFAULT_HORIZONTAL_X_OVERLAP_THRESHOLD,
    val horizontalFillRatioMin: Float = DEFAULT_HORIZONTAL_FILL_RATIO_MIN,
    val verticalColumnGapMultiplier: Float = DEFAULT_VERTICAL_COLUMN_GAP_MULTIPLIER,
    val verticalXOverlapThreshold: Float = DEFAULT_VERTICAL_X_OVERLAP_THRESHOLD,
    val verticalFillRatioMin: Float = DEFAULT_VERTICAL_FILL_RATIO_MIN,
    val horizontalPaddingXRatio: Float = DEFAULT_HORIZONTAL_PADDING_X_RATIO,
    val horizontalPaddingYRatio: Float = DEFAULT_HORIZONTAL_PADDING_Y_RATIO,
    val verticalPaddingXRatio: Float = DEFAULT_VERTICAL_PADDING_X_RATIO,
    val verticalPaddingYRatio: Float = DEFAULT_VERTICAL_PADDING_Y_RATIO
) {
    fun normalized(): OcrBoxDetectionSettings {
        return copy(
            horizontalRowGapMultiplier = horizontalRowGapMultiplier.coerceIn(0.4f, 3.0f),
            horizontalXOverlapThreshold = horizontalXOverlapThreshold.coerceIn(0.05f, 0.9f),
            horizontalFillRatioMin = horizontalFillRatioMin.coerceIn(0.05f, 0.8f),
            verticalColumnGapMultiplier = verticalColumnGapMultiplier.coerceIn(0.4f, 3.0f),
            verticalXOverlapThreshold = verticalXOverlapThreshold.coerceIn(0.05f, 0.9f),
            verticalFillRatioMin = verticalFillRatioMin.coerceIn(0.05f, 0.8f),
            horizontalPaddingXRatio = horizontalPaddingXRatio.coerceIn(0f, 0.3f),
            horizontalPaddingYRatio = horizontalPaddingYRatio.coerceIn(0f, 0.3f),
            verticalPaddingXRatio = verticalPaddingXRatio.coerceIn(0f, 0.3f),
            verticalPaddingYRatio = verticalPaddingYRatio.coerceIn(0f, 0.3f)
        )
    }

    companion object {
        const val DEFAULT_HORIZONTAL_ROW_GAP_MULTIPLIER = 1.25f
        const val DEFAULT_HORIZONTAL_X_OVERLAP_THRESHOLD = 0.52f
        const val DEFAULT_HORIZONTAL_FILL_RATIO_MIN = 0.34f
        const val DEFAULT_VERTICAL_COLUMN_GAP_MULTIPLIER = 1.8f
        const val DEFAULT_VERTICAL_X_OVERLAP_THRESHOLD = 0.40f
        const val DEFAULT_VERTICAL_FILL_RATIO_MIN = 0.30f
        const val DEFAULT_HORIZONTAL_PADDING_X_RATIO = 0.12f
        const val DEFAULT_HORIZONTAL_PADDING_Y_RATIO = 0.10f
        const val DEFAULT_VERTICAL_PADDING_X_RATIO = 0.10f
        const val DEFAULT_VERTICAL_PADDING_Y_RATIO = 0.10f

        val DEFAULT = OcrBoxDetectionSettings()
    }
}
