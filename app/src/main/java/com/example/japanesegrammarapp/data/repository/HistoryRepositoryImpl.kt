package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.data.AnalysisDao
import com.example.japanesegrammarapp.data.AnalysisEvent
import com.example.japanesegrammarapp.data.AnalysisRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val analysisDao: AnalysisDao
) : HistoryRepository {

    private val _analysisEvents = MutableSharedFlow<AnalysisEvent>(extraBufferCapacity = 10)
    override val analysisEvents = _analysisEvents.asSharedFlow()

    override suspend fun emitEvent(event: AnalysisEvent) {
        _analysisEvents.emit(event)
    }

    override val history: Flow<List<AnalysisRecord>> = analysisDao.getAllRecords()

    override suspend fun getRecordById(id: Int): AnalysisRecord? {
        return analysisDao.getRecordById(id)
    }

    override suspend fun insertRecord(record: AnalysisRecord): Long {
        return analysisDao.insert(record)
    }

    override suspend fun updateRecord(record: AnalysisRecord) {
        analysisDao.update(record)
    }

    override suspend fun deleteRecord(record: AnalysisRecord) {
        analysisDao.delete(record)
    }
}
