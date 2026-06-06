package com.example.japanesegrammarapp.domain.repository

import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.ModelTokenUsage
import com.example.japanesegrammarapp.domain.model.DailyTokenUsage
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

interface HistoryRepository {
    val history: Flow<PagingData<AnalysisDomainRecord>>
    fun getHistory(query: String): Flow<PagingData<AnalysisDomainRecord>>
    suspend fun getAllRecordsList(): List<AnalysisDomainRecord>
    suspend fun getRecordById(id: Int): AnalysisDomainRecord?
    fun observeRecordById(id: Int): Flow<AnalysisDomainRecord?>
    suspend fun getRecordByOriginalText(originalText: String): AnalysisDomainRecord?
    suspend fun insertRecord(record: AnalysisDomainRecord): Long
    suspend fun updateRecord(record: AnalysisDomainRecord)
    suspend fun deleteRecord(record: AnalysisDomainRecord)
    val totalTokensConsumed: Flow<Int?>
    val tokenUsageByModel: Flow<List<ModelTokenUsage>>
    val dailyTokenUsage: Flow<List<DailyTokenUsage>>
}
