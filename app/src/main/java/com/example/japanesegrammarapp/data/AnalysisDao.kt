package com.example.japanesegrammarapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ModelTokenUsageEntity(
    val modelUsed: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)

data class DailyTokenUsageEntity(
    val date: String,
    val modelUsed: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)

data class HistoryExportPreviewEntity(
    val id: Int,
    val originalText: String,
    val timestamp: Long,
    val modelUsed: String,
    val status: String
)

@Dao
interface AnalysisDao {
    @Insert
    suspend fun insert(record: AnalysisRecord): Long

    @Update
    suspend fun update(record: AnalysisRecord)

    @Delete
    suspend fun delete(record: AnalysisRecord)

    @Query("UPDATE analysis_records SET isRead = 1 WHERE id = :recordId")
    suspend fun markAsRead(recordId: Int)

    @Query("SELECT * FROM analysis_records WHERE id = :id")
    suspend fun getRecordById(id: Int): AnalysisRecord?

    @Query("SELECT * FROM analysis_records WHERE id = :id")
    fun observeRecordById(id: Int): Flow<AnalysisRecord?>

    @Query("SELECT * FROM analysis_records WHERE originalText = :originalText ORDER BY timestamp DESC LIMIT 1")
    suspend fun getRecordByOriginalText(originalText: String): AnalysisRecord?

    @Query("SELECT * FROM analysis_records WHERE timestamp > :currentTimestamp ORDER BY timestamp ASC LIMIT 1")
    suspend fun getNewerRecord(currentTimestamp: Long): AnalysisRecord?

    @Query("SELECT * FROM analysis_records WHERE timestamp < :currentTimestamp ORDER BY timestamp DESC LIMIT 1")
    suspend fun getOlderRecord(currentTimestamp: Long): AnalysisRecord?

    @Query("SELECT * FROM analysis_records ORDER BY timestamp DESC")
    fun getAllRecords(): androidx.paging.PagingSource<Int, AnalysisRecord>

    @Query("SELECT * FROM analysis_records WHERE 0")
    fun getNoRecords(): androidx.paging.PagingSource<Int, AnalysisRecord>

    @Query("""
        SELECT analysis_records.* FROM analysis_records
        JOIN analysis_records_fts ON analysis_records.id = analysis_records_fts.rowid
        WHERE analysis_records_fts MATCH :pattern
        ORDER BY timestamp DESC
    """)
    fun searchRecords(pattern: String): androidx.paging.PagingSource<Int, AnalysisRecord>

    @Query("SELECT * FROM analysis_records ORDER BY timestamp ASC")
    suspend fun getAllRecordsList(): List<AnalysisRecord>

    @Query("SELECT id, originalText, timestamp, modelUsed, status FROM analysis_records ORDER BY timestamp ASC")
    suspend fun getAllExportPreviews(): List<HistoryExportPreviewEntity>

    @Query("SELECT * FROM analysis_records WHERE id IN (:ids) ORDER BY timestamp ASC")
    suspend fun getRecordsByIds(ids: List<Int>): List<AnalysisRecord>

    @Query("SELECT SUM(consumedTokens) FROM analysis_records")
    fun getTotalTokensConsumed(): Flow<Int?>

    @Query("SELECT modelUsed, SUM(inputTokens) as inputTokens, SUM(outputTokens) as outputTokens, SUM(consumedTokens) as totalTokens FROM analysis_records WHERE consumedTokens > 0 GROUP BY modelUsed ORDER BY totalTokens DESC")
    fun getTokenUsageByModel(): Flow<List<ModelTokenUsageEntity>>

    @Query("SELECT date(timestamp / 1000, 'unixepoch', 'localtime') as date, modelUsed, SUM(inputTokens) as inputTokens, SUM(outputTokens) as outputTokens, SUM(consumedTokens) as totalTokens FROM analysis_records WHERE consumedTokens > 0 GROUP BY date, modelUsed ORDER BY date DESC, totalTokens DESC")
    fun getDailyTokenUsage(): Flow<List<DailyTokenUsageEntity>>
}
