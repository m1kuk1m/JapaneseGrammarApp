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
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val analysisDao: AnalysisDao
) : HistoryRepository {

    override val history: Flow<PagingData<AnalysisDomainRecord>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { analysisDao.getAllRecords() }
    ).flow.map { pagingData ->
        pagingData.map { it.toDomain() }
    }

    override suspend fun getAllRecordsList(): List<AnalysisDomainRecord> {
        return analysisDao.getAllRecordsList().map { it.toDomain() }
    }
    
    override val totalTokensConsumed: Flow<Int?> = analysisDao.getTotalTokensConsumed()
    override val tokenUsageByModel: Flow<List<ModelTokenUsage>> = analysisDao.getTokenUsageByModel()
    override val dailyTokenUsage: Flow<List<DailyTokenUsage>> = analysisDao.getDailyTokenUsage()

    override suspend fun getRecordById(id: Int): AnalysisDomainRecord? {
        return analysisDao.getRecordById(id)?.toDomain()
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
