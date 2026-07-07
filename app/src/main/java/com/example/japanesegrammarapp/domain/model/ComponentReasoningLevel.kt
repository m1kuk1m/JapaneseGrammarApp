package com.example.japanesegrammarapp.domain.model

enum class ComponentReasoningLevel {
    GLOBAL, OFF, AUTO, LOW, MEDIUM, HIGH;

    fun toReasoningLevel(globalLevel: ReasoningLevel): ReasoningLevel {
        return when (this) {
            GLOBAL -> globalLevel
            OFF -> ReasoningLevel.OFF
            AUTO -> ReasoningLevel.AUTO
            LOW -> ReasoningLevel.LOW
            MEDIUM -> ReasoningLevel.MEDIUM
            HIGH -> ReasoningLevel.HIGH
        }
    }

    companion object {
        fun fromString(value: String?): ComponentReasoningLevel {
            return entries.find { it.name == value } ?: GLOBAL
        }
    }
}
