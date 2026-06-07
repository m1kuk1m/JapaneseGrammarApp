package com.example.japanesegrammarapp.data.repository

import androidx.paging.PagingData
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import kotlinx.coroutines.flow.Flow

interface PagedHistoryRepository {
    fun getHistory(query: String): Flow<PagingData<AnalysisDomainRecord>>
}
