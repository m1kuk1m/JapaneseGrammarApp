package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.DailyTokenUsage
import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.model.ModelTokenUsage
import com.example.japanesegrammarapp.domain.model.WordSegment
import com.example.japanesegrammarapp.domain.repository.DetailedResultSerializer
import com.example.japanesegrammarapp.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisStoresTest {
    @Test
    fun progressStoreTracksAndClearsRecordProgress() {
        val store = AnalysisProgressStore()

        store.start(7)
        store.markTokenizerCompleted(7)
        store.markTranslationCompleted(7)
        store.markSegmentsCompleted(7)

        val progress = store.progressFlow.value.getValue(7)
        assertTrue(progress.tokenizerCompleted)
        assertTrue(progress.translationCompleted)
        assertTrue(progress.segmentsCompleted)
        assertFalse(progress.clausesCompleted)
        assertFalse(progress.grammarCompleted)

        store.finish(7)

        assertFalse(store.progressFlow.value.containsKey(7))
    }

    @Test
    fun partialResultStorePersistsJsonAndTokenTotals() = runBlocking {
        val repository = FakeHistoryRepository(
            AnalysisDomainRecord(
                id = 3,
                originalText = "日本語",
                modelUsed = "test"
            )
        )
        val serializer = FakeDetailedResultSerializer()
        val store = AnalysisPartialResultStore(
            recordId = 3,
            saveAnalysisRecordUseCase = SaveAnalysisRecordUseCase(repository),
            detailedResultSerializer = serializer
        )

        store.update { current ->
            current.copy(
                translation = "Japanese",
                segments = listOf(WordSegment(text = "日本語")),
                consumedTokens = current.consumedTokens + 11,
                inputTokens = current.inputTokens + 7,
                outputTokens = current.outputTokens + 4
            )
        }

        val saved = repository.requireRecord(3)
        assertEquals("translation=Japanese;tokens=11/7/4", saved.analysisResult)
        assertEquals(11, saved.consumedTokens)
        assertEquals(7, saved.inputTokens)
        assertEquals(4, saved.outputTokens)
        assertEquals("日本語", store.combinedSegmentText())
    }

    private class FakeDetailedResultSerializer : DetailedResultSerializer {
        override fun toJson(result: DetailedAnalysisResult): String {
            return "translation=${result.translation};tokens=${result.consumedTokens}/${result.inputTokens}/${result.outputTokens}"
        }

        override fun fromJson(json: String?): DetailedAnalysisResult? = null
    }

    private class FakeHistoryRepository(initialRecord: AnalysisDomainRecord) : HistoryRepository {
        private val records = mutableMapOf(initialRecord.id to initialRecord)

        override suspend fun getAllRecordsList(): List<AnalysisDomainRecord> = records.values.toList()

        override suspend fun getRecordById(id: Int): AnalysisDomainRecord? = records[id]

        override fun observeRecordById(id: Int): Flow<AnalysisDomainRecord?> = MutableStateFlow(records[id])

        override suspend fun getRecordByOriginalText(originalText: String): AnalysisDomainRecord? {
            return records.values.firstOrNull { it.originalText == originalText }
        }

        override suspend fun insertRecord(record: AnalysisDomainRecord): Long {
            val nextId = (records.keys.maxOrNull() ?: 0) + 1
            records[nextId] = record.copy(id = nextId)
            return nextId.toLong()
        }

        override suspend fun updateRecord(record: AnalysisDomainRecord) {
            records[record.id] = record
        }

        override suspend fun deleteRecord(record: AnalysisDomainRecord) {
            records.remove(record.id)
        }

        override val totalTokensConsumed: Flow<Int?> = MutableStateFlow(0)
        override val tokenUsageByModel: Flow<List<ModelTokenUsage>> = MutableStateFlow(emptyList())
        override val dailyTokenUsage: Flow<List<DailyTokenUsage>> = MutableStateFlow(emptyList())

        fun requireRecord(id: Int): AnalysisDomainRecord = checkNotNull(records[id])
    }
}
