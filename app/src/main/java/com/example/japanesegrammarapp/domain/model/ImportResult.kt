package com.example.japanesegrammarapp.domain.model

data class ImportResult(
    val successCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val failureReasons: List<String> = emptyList()
)
