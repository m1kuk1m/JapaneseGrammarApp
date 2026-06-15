package com.example.japanesegrammarapp.domain.model

data class LlmEndpoint(
    val id: String,
    val provider: String,
    val name: String,
    val baseUrl: String,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val weight: Int = 1,
    val cooldownUntilMs: Long = 0L,
    val consecutiveFailures: Int = 0,
    val lastUsedAtMs: Long = 0L,
    val lastError: String? = null
)
