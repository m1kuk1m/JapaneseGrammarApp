package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.data.AnalysisEvent
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    val analysisEvents: Flow<AnalysisEvent>
    suspend fun emitEvent(event: AnalysisEvent)
    
    val history: Flow<List<AnalysisDomainRecord>>
    suspend fun getRecordById(id: Int): AnalysisDomainRecord?
    suspend fun insertRecord(record: AnalysisDomainRecord): Long
    suspend fun updateRecord(record: AnalysisDomainRecord)
    suspend fun deleteRecord(record: AnalysisDomainRecord)
    val totalTokensConsumed: Flow<Int?>
    val tokenUsageByModel: Flow<List<com.example.japanesegrammarapp.data.ModelTokenUsage>>
    val dailyTokenUsage: Flow<List<com.example.japanesegrammarapp.data.DailyTokenUsage>>
}
