package com.example.japanesegrammarapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkedGrammarPointDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(point: BookmarkedGrammarPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(point: BookmarkedGrammarPoint): Long

    @Query("SELECT * FROM bookmarked_grammar_points ORDER BY bookmarkedAt DESC")
    fun getAll(): Flow<List<BookmarkedGrammarPoint>>

    @Query("SELECT * FROM bookmarked_grammar_points WHERE recordId = :recordId")
    fun getGrammarPointsForRecord(recordId: Int): Flow<List<BookmarkedGrammarPoint>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_grammar_points WHERE recordId = :recordId AND pattern = :pattern)")
    suspend fun existsByPatternDirect(recordId: Int, pattern: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_grammar_points WHERE recordId = :recordId AND pattern = :pattern)")
    fun existsByPattern(recordId: Int, pattern: String): Flow<Boolean>

    @Query("DELETE FROM bookmarked_grammar_points WHERE recordId = :recordId AND pattern = :pattern")
    suspend fun deleteByPattern(recordId: Int, pattern: String)

    @Query("DELETE FROM bookmarked_grammar_points WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE bookmarked_grammar_points SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchivedStatus(id: Int, isArchived: Boolean)

    @Query("UPDATE bookmarked_grammar_points SET isArchived = 1 WHERE id IN (:ids)")
    suspend fun archiveMultiple(ids: List<Int>)

    @Transaction
    suspend fun toggleGrammarPointBookmark(
        recordId: Int,
        pattern: String,
        explanation: String?,
        sourceText: String
    ): Boolean {
        val exists = existsByPatternDirect(recordId, pattern)
        if (exists) {
            deleteByPattern(recordId, pattern)
            return false
        } else {
            insert(
                BookmarkedGrammarPoint(
                    recordId = recordId,
                    pattern = pattern,
                    explanation = explanation,
                    bookmarkedAt = System.currentTimeMillis(),
                    sourceText = sourceText
                )
            )
            return true
        }
    }
}
