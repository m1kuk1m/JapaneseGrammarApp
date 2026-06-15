package com.example.japanesegrammarapp.domain.model

data class ModelTokenUsage(
    val modelUsed: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)

data class DailyTokenUsage(
    val date: String,
    val modelUsed: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)
