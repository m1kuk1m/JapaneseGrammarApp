package com.example.japanesegrammarapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarked_sentences",
    indices = [Index(value = ["recordId"])]
)
data class BookmarkedSentence(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val recordId: Int, // The corresponding analysis record ID, or -1 if the record is deleted
    val originalText: String,
    val translation: String?,
    val analysisResult: String?, // The full raw analysis result (JSON) so it can be parsed when viewed
    val modelUsed: String?,
    val bookmarkedAt: Long = System.currentTimeMillis()
)
