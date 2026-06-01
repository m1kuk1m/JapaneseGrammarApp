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
