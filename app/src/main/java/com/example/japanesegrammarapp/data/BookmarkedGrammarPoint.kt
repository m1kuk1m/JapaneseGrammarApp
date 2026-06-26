package com.example.japanesegrammarapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarked_grammar_points",
    indices = [Index(value = ["recordId", "pattern"], unique = true)]
)
data class BookmarkedGrammarPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // The corresponding analysis record ID, or -1 for imported/orphaned favorites.
    val recordId: Int,
    val pattern: String,
    val explanation: String? = null,
    val bookmarkedAt: Long = System.currentTimeMillis(),
    val sourceText: String = "",
    val isArchived: Boolean = false
)
