package com.example.japanesegrammarapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkedSentenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkedSentence): Long

    @Query("SELECT * FROM bookmarked_sentences ORDER BY bookmarkedAt DESC")
    fun getAll(): Flow<List<BookmarkedSentence>>

    @Query("SELECT * FROM bookmarked_sentences WHERE recordId = :recordId LIMIT 1")
    fun getByRecordId(recordId: Int): Flow<BookmarkedSentence?>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_sentences WHERE recordId = :recordId)")
    fun existsByRecordId(recordId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_sentences WHERE originalText = :originalText)")
    fun existsByOriginalText(originalText: String): Flow<Boolean>

    @Query("DELETE FROM bookmarked_sentences WHERE recordId = :recordId")
    suspend fun deleteByRecordId(recordId: Int)

    @Query("DELETE FROM bookmarked_sentences WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE bookmarked_sentences SET recordId = -1 WHERE recordId = :recordId")
    suspend fun detachFromRecord(recordId: Int)
}
