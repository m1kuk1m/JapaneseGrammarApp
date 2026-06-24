package com.example.japanesegrammarapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme

object ZenColors {
    val SumiInk = Color(0xFF2B2A28)          // 墨色 (Deep charcoal black)
    val WashiBg = Color(0xFFFCF8F2)          // 和纸色 (Soft warm white cream)
    val SakuraPink = Color(0xFFFEDFE1)       // 樱花色 (Soft warm blush pink)
    val MatchaGreen = Color(0xFFC5E2C6)      // 抹茶色 (Soft sage green)
    val AizomeIndigo = Color(0xFFBCCCD4)     // 蓝染色 (Soft slate blue)
    val KuriAmber = Color(0xFFECCEB1)        // 栗色 (Soft amber/chestnut)
    val HaiMist = Color(0xFFE5E4E2)          // 雾灰色 (Soft grey)
}

object ZenThemeColors {
    @Composable
    fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val PosColorsLight = mapOf(
        "NOUN" to Color(0xFFD3E0EA),
        "VERB" to Color(0xFFD4ECD5),
        "ADJECTIVE" to Color(0xFFF6E2CD),
        "AUXILIARY" to Color(0xFFE8D3EA),
        "PARTICLE" to Color(0xFFFDD4D8),
        "ADVERB" to Color(0xFFFAF0D2),
        "CONJUNCTION" to Color(0xFFD2F5F0),
        "PRONOUN" to Color(0xFFE1E0F5),
        "PRE_NOUN_ADJECTIVAL" to Color(0xFFECEFC9),
        "INTERJECTION" to Color(0xFFFCE1D4),
        "SYMBOL" to Color(0xFFEAEAEA),
        "AFFIX" to Color(0xFFE5E4E2),
        "PHRASE" to Color(0xFFECCEB1)
    )

    val PosColorsDark = mapOf(
        "NOUN" to Color(0xFF1E2D3D),
        "VERB" to Color(0xFF1E3D24),
        "ADJECTIVE" to Color(0xFF3D2A1E),
        "AUXILIARY" to Color(0xFF2D1E3D),
        "PARTICLE" to Color(0xFF3D1E25),
        "ADVERB" to Color(0xFF3D351E),
        "CONJUNCTION" to Color(0xFF1E3D3A),
        "PRONOUN" to Color(0xFF24223D),
        "PRE_NOUN_ADJECTIVAL" to Color(0xFF323D1E),
        "INTERJECTION" to Color(0xFF3D2A22),
        "SYMBOL" to Color(0xFF232323),
        "AFFIX" to Color(0xFF2F3131),
        "PHRASE" to Color(0xFF3D2A1E)
    )

    @Composable
    fun getChipColor(category: String): Color {
        return getChipColor(category, isDark())
    }

    fun getChipColor(category: String, isDark: Boolean): Color {
        val colorMap = if (isDark) PosColorsDark else PosColorsLight
        return colorMap[category] ?: (if (isDark) Color(0xFF2D2D2D) else Color(0xFFEFEFEF))
    }

    @Composable
    fun sumiInk(): Color = if (isDark()) Color(0xFFE0E0E0) else ZenColors.SumiInk

    @Composable
    fun washiBg(): Color = if (isDark()) Color(0xFF1E1E1E) else ZenColors.WashiBg

    @Composable
    fun cardBg(): Color = if (isDark()) Color(0xFF2C2C2C) else Color.White

    @Composable
    fun pillBg(): Color = if (isDark()) Color(0xFF2B2C2C) else Color(0xFFF4EFE6)

    @Composable
    fun buttonBg(): Color = if (isDark()) Color(0xFF2B2C2C) else Color(0xFFFDFBF7)

    @Composable
    fun divider(): Color = if (isDark()) Color(0xFF3C3C3C) else Color(0xFFE5E4E2)

    @Composable
    fun selectedHistoryBg(): Color = if (isDark()) Color(0xFF4A4A4A) else Color(0xFFD6D6D6)
}
