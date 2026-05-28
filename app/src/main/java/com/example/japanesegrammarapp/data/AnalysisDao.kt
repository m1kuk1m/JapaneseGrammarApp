package com.example.japanesegrammarapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    @Insert
    suspend fun insert(record: AnalysisRecord): Long

    @Update
    suspend fun update(record: AnalysisRecord)

    @Delete
    suspend fun delete(record: AnalysisRecord)

    @Query("SELECT * FROM analysis_records WHERE id = :id")
    suspend fun getRecordById(id: Int): AnalysisRecord?

    @Query("SELECT * FROM analysis_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AnalysisRecord>>
}
