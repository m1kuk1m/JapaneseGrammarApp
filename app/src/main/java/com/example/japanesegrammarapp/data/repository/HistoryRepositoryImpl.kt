package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.data.AnalysisDao
import com.example.japanesegrammarapp.data.mapper.toDomain
import com.example.japanesegrammarapp.data.mapper.toEntity
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.ModelTokenUsage
import com.example.japanesegrammarapp.domain.model.DailyTokenUsage
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val analysisDao: AnalysisDao
) : HistoryRepository {

    override suspend fun getAllRecordsList(): List<AnalysisDomainRecord> {
        return analysisDao.getAllRecordsList().map { it.toDomain() }
    }
    
    override val totalTokensConsumed: Flow<Int?> = analysisDao.getTotalTokensConsumed()
    override val tokenUsageByModel: Flow<List<ModelTokenUsage>> = analysisDao.getTokenUsageByModel()
        .map { list -> list.map { it.toDomain() } }
    override val dailyTokenUsage: Flow<List<DailyTokenUsage>> = analysisDao.getDailyTokenUsage()
        .map { list -> list.map { it.toDomain() } }

    override suspend fun getRecordById(id: Int): AnalysisDomainRecord? {
        val entity = analysisDao.getRecordById(id)
        return entity?.toDomain()
    }

    override fun observeRecordById(id: Int): Flow<AnalysisDomainRecord?> {
        return analysisDao.observeRecordById(id).map { it?.toDomain() }
    }

    override suspend fun getRecordByOriginalText(originalText: String): AnalysisDomainRecord? {
        val entity = analysisDao.getRecordByOriginalText(originalText)
        return entity?.toDomain()
    }

    override suspend fun insertRecord(record: AnalysisDomainRecord): Long {
        return analysisDao.insert(record.toEntity())
    }

    override suspend fun updateRecord(record: AnalysisDomainRecord) {
        analysisDao.update(record.toEntity())
    }

    override suspend fun deleteRecord(record: AnalysisDomainRecord) {
        analysisDao.delete(record.toEntity())
    }
}
