package com.example.japanesegrammarapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_records")
data class AnalysisRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val originalText: String,
    val imageUri: String?,
    val analysisResult: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String,
    val status: String = "COMPLETED", // "PENDING", "COMPLETED", "FAILED"
    val errorMessage: String? = null,
    val consumedTokens: Int = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0
)
