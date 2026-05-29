package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.data.AnalysisEvent
import com.example.japanesegrammarapp.data.AnalysisRecord
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    val analysisEvents: Flow<AnalysisEvent>
    suspend fun emitEvent(event: AnalysisEvent)
    
    val history: Flow<List<AnalysisRecord>>
    suspend fun getRecordById(id: Int): AnalysisRecord?
    suspend fun insertRecord(record: AnalysisRecord): Long
    suspend fun updateRecord(record: AnalysisRecord)
    suspend fun deleteRecord(record: AnalysisRecord)
}
