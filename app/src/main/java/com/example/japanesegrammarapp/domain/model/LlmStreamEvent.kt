package com.example.japanesegrammarapp.domain.model

import com.example.japanesegrammarapp.domain.repository.LlmResultMetadata

sealed class LlmStreamEvent {
    data class Chunk(val text: String) : LlmStreamEvent()
    data class Metadata(
        val provider: String,
        val modelName: String,
        val usage: LlmResultMetadata
    ) : LlmStreamEvent()
}
