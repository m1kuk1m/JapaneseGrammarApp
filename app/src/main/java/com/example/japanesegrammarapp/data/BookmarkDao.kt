package com.example.japanesegrammarapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: BookmarkedSegment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(bookmark: BookmarkedSegment): Long

    @androidx.room.Update
    suspend fun update(bookmark: BookmarkedSegment)

    @Query("SELECT * FROM bookmarked_segments ORDER BY bookmarkedAt DESC")
    fun getAll(): Flow<List<BookmarkedSegment>>

    @Query("SELECT segmentText FROM bookmarked_segments WHERE recordId = :recordId UNION SELECT surfaceForm FROM bookmarked_segments WHERE recordId = :recordId AND surfaceForm IS NOT NULL")
    fun getSegmentTextsForRecord(recordId: Int): Flow<List<String>>

    @Query("SELECT * FROM bookmarked_segments WHERE recordId = :recordId AND dictionaryForm = :dictForm")
    fun getByRecordAndDictForm(recordId: Int, dictForm: String): Flow<List<BookmarkedSegment>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_segments WHERE recordId = :recordId AND (surfaceForm = :surfaceForm OR (surfaceForm IS NULL AND segmentText = :surfaceForm)))")
    fun existsBySurfaceForm(recordId: Int, surfaceForm: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_segments WHERE recordId = :recordId AND segmentText = :dictForm)")
    fun existsByDictForm(recordId: Int, dictForm: String): Flow<Boolean>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM bookmarked_segments
            WHERE recordId = :recordId
              AND segmentText = :dictForm
              AND (
                  surfaceForm = :surfaceForm
                  OR (surfaceForm IS NULL AND segmentText = :surfaceForm)
              )
        )
    """)
    suspend fun existsByKeyDirect(recordId: Int, surfaceForm: String, dictForm: String): Boolean

    @Query("DELETE FROM bookmarked_segments WHERE recordId = :recordId AND (surfaceForm = :surfaceForm OR (surfaceForm IS NULL AND segmentText = :surfaceForm)) AND segmentText = :dictForm")
    suspend fun deleteByKey(recordId: Int, surfaceForm: String, dictForm: String)

    @Query("DELETE FROM bookmarked_segments WHERE recordId = :recordId AND segmentText = :dictForm")
    suspend fun deleteByDictForm(recordId: Int, dictForm: String)

    @Query("DELETE FROM bookmarked_segments WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM bookmarked_segments")
    fun getCount(): Flow<Int>

    @Query("UPDATE bookmarked_segments SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchivedStatus(id: Int, isArchived: Boolean)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_segments WHERE recordId = :recordId AND (surfaceForm = :surfaceForm OR (surfaceForm IS NULL AND segmentText = :surfaceForm)) AND (dictionaryForm = :dictionaryForm OR (dictionaryForm IS NULL AND segmentText = :dictionaryForm)))")
    suspend fun existsForImport(recordId: Int, surfaceForm: String, dictionaryForm: String): Boolean

    @Query("UPDATE bookmarked_segments SET isArchived = 1 WHERE id IN (:ids)")
    suspend fun archiveMultiple(ids: List<Int>)

    @Transaction
    suspend fun toggleBookmark(
        recordId: Int,
        surfaceForm: String,
        dictForm: String,
        dictReading: String,
        partOfSpeech: String?,
        posCategory: String?,
        meaning: String?,
        inflection: String?,
        role: String?,
        sourceText: String
    ): Boolean {
        val exists = existsByKeyDirect(recordId, surfaceForm, dictForm)
        if (exists) {
            deleteByKey(recordId, surfaceForm, dictForm)
            return false
        } else {
            insert(
                BookmarkedSegment(
                    recordId = recordId,
                    segmentText = dictForm,
                    surfaceForm = surfaceForm,
                    reading = dictReading,
                    partOfSpeech = partOfSpeech,
                    posCategory = posCategory,
                    dictionaryForm = dictForm,
                    dictionaryFormReading = dictReading,
                    meaning = meaning,
                    inflection = inflection,
                    role = role,
                    bookmarkedAt = System.currentTimeMillis(),
                    sourceText = sourceText
                )
            )
            return true
        }
    }
}
