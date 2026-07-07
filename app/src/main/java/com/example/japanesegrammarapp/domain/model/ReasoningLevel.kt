package com.example.japanesegrammarapp.domain.model

enum class ReasoningLevel {
    OFF, AUTO, LOW, MEDIUM, HIGH;

    companion object {
        fun fromString(value: String?): ReasoningLevel {
            return entries.find { it.name == value } ?: AUTO
        }
    }
}
