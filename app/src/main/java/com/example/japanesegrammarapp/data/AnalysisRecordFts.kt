package com.example.japanesegrammarapp.data

import androidx.room.Entity
import androidx.room.Fts4

import androidx.room.FtsOptions

@Entity(tableName = "analysis_records_fts")
@Fts4(contentEntity = AnalysisRecord::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class AnalysisRecordFts(
    val originalText: String,
    val analysisResult: String?
)
