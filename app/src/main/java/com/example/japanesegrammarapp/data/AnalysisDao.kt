package com.example.japanesegrammarapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.example.japanesegrammarapp.domain.model.ModelTokenUsage
import com.example.japanesegrammarapp.domain.model.DailyTokenUsage

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

    @Query("SELECT * FROM analysis_records WHERE originalText = :originalText ORDER BY timestamp DESC LIMIT 1")
    suspend fun getRecordByOriginalText(originalText: String): AnalysisRecord?

    @Query("SELECT * FROM analysis_records ORDER BY timestamp DESC")
    fun getAllRecords(): androidx.paging.PagingSource<Int, AnalysisRecord>

    @Query("SELECT * FROM analysis_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsList(): List<AnalysisRecord>

    @Query("SELECT SUM(consumedTokens) FROM analysis_records")
    fun getTotalTokensConsumed(): Flow<Int?>

    @Query("SELECT modelUsed, SUM(consumedTokens) as totalTokens FROM analysis_records WHERE consumedTokens > 0 GROUP BY modelUsed ORDER BY totalTokens DESC")
    fun getTokenUsageByModel(): Flow<List<ModelTokenUsage>>

    @Query("SELECT date(timestamp / 1000, 'unixepoch', 'localtime') as date, modelUsed, SUM(inputTokens) as inputTokens, SUM(outputTokens) as outputTokens, SUM(consumedTokens) as totalTokens FROM analysis_records WHERE consumedTokens > 0 GROUP BY date, modelUsed ORDER BY date DESC, totalTokens DESC")
    fun getDailyTokenUsage(): Flow<List<DailyTokenUsage>>
}

