package com.example.japanesegrammarapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarked_grammar_points",
    foreignKeys = [
        ForeignKey(
            entity = AnalysisRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordId", "pattern"], unique = true)]
)
data class BookmarkedGrammarPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val recordId: Int,
    val pattern: String,
    val explanation: String? = null,
    val bookmarkedAt: Long = System.currentTimeMillis(),
    val sourceText: String = "",
    val isArchived: Boolean = false
)
