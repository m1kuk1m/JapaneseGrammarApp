package com.example.japanesegrammarapp.domain.model

/**
 * モジュール別モデル設定。provider/model が空の場合はグローバル設定に従う。
 */
data class ComponentModelConfig(
    val provider: String = "",
    val model: String = ""
) {
    val isGlobal: Boolean get() = provider.isBlank() || model.isBlank()
}
