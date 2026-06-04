package com.example.japanesegrammarapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarked_segments",
    indices = [Index(value = ["recordId", "segmentText"], unique = true)]
)
data class BookmarkedSegment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val recordId: Int,
    val segmentText: String,
    val reading: String? = null,
    val partOfSpeech: String? = null,
    val posCategory: String? = null,
    val dictionaryForm: String? = null,
    val dictionaryFormReading: String? = null,
    val meaning: String? = null,
    val inflection: String? = null,
    val role: String? = null,
    val bookmarkedAt: Long = System.currentTimeMillis(),
    // Snapshot of the source sentence for display even if the record is deleted
    val sourceText: String = ""
)
