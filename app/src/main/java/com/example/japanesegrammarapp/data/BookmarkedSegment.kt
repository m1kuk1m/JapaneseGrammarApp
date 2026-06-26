package com.example.japanesegrammarapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarked_segments",
    indices = [Index(value = ["recordId", "surfaceForm", "dictionaryForm"], unique = true)]
)
data class BookmarkedSegment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // The corresponding analysis record ID, or -1 for imported/orphaned favorites.
    val recordId: Int,
    /** The dictionary/citation form used as the bookmark key (e.g. "食べる") */
    val segmentText: String,
    /** The original surface form as it appeared in the sentence (e.g. "食べた") */
    val surfaceForm: String? = null,
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
    val sourceText: String = "",
    val isArchived: Boolean = false
)
