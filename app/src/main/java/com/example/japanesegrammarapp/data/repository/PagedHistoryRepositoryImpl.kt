package com.example.japanesegrammarapp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.japanesegrammarapp.data.AnalysisDao
import com.example.japanesegrammarapp.data.mapper.toDomain
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PagedHistoryRepositoryImpl @Inject constructor(
    private val analysisDao: AnalysisDao
) : PagedHistoryRepository {

    override fun getHistory(query: String): Flow<PagingData<AnalysisDomainRecord>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isEmpty()) {
                analysisDao.getAllRecords()
            } else {
                analysisDao.searchRecords("%${trimmedQuery.escapeLikePattern()}%")
            }
        }
    ).flow.map { pagingData ->
        pagingData.map { it.toDomain() }
    }

    private fun String.escapeLikePattern(): String {
        return replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}
