package com.example.japanesegrammarapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: BookmarkedSegment): Long

    @Query("SELECT * FROM bookmarked_segments ORDER BY bookmarkedAt DESC")
    fun getAll(): Flow<List<BookmarkedSegment>>

    @Query("SELECT segmentText FROM bookmarked_segments WHERE recordId = :recordId")
    fun getSegmentTextsForRecord(recordId: Int): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_segments WHERE recordId = :recordId AND segmentText = :text)")
    fun exists(recordId: Int, text: String): Flow<Boolean>

    @Query("DELETE FROM bookmarked_segments WHERE recordId = :recordId AND segmentText = :text")
    suspend fun deleteByKey(recordId: Int, text: String)

    @Query("DELETE FROM bookmarked_segments WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM bookmarked_segments")
    fun getCount(): Flow<Int>
}
