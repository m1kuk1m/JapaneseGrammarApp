package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import javax.inject.Inject

class SaveAnalysisRecordUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend fun insert(record: AnalysisDomainRecord): Long {
        return historyRepository.insertRecord(record)
    }

    suspend fun update(record: AnalysisDomainRecord) {
        historyRepository.updateRecord(record)
    }

    suspend fun getById(id: Int): AnalysisDomainRecord? {
        return historyRepository.getRecordById(id)
    }

    suspend fun delete(record: AnalysisDomainRecord) {
        historyRepository.deleteRecord(record)
    }
}
