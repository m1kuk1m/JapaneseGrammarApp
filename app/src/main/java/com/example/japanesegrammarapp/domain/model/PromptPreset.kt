package com.example.japanesegrammarapp.domain.model

data class PromptPreset(
    val id: String,
    val name: String,
    val prompts: Map<String, String> = emptyMap()
) {
    companion object {
        const val DEFAULT_PRESET_ID = "default"
    }
}
