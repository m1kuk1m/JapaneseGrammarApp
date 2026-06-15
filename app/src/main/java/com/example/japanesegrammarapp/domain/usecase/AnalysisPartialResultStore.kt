package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.repository.AppLogWriter
import com.example.japanesegrammarapp.domain.repository.DetailedResultSerializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AnalysisPartialResultStore(
    private val recordId: Int,
    private val saveAnalysisRecordUseCase: SaveAnalysisRecordUseCase,
    private val detailedResultSerializer: DetailedResultSerializer,
    private val appLogWriter: AppLogWriter
) {
    private val mutex = Mutex()
    private var result = DetailedAnalysisResult()

    suspend fun update(updateBlock: (DetailedAnalysisResult) -> DetailedAnalysisResult) {
        mutex.withLock {
            try {
                result = updateBlock(result)
                val currentRecord = saveAnalysisRecordUseCase.getById(recordId)
                if (currentRecord != null) {
                    val mergedResult = detailedResultSerializer.toJson(result)
                    saveAnalysisRecordUseCase.update(
                        currentRecord.copy(
                            analysisResult = mergedResult,
                            consumedTokens = result.consumedTokens,
                            inputTokens = result.inputTokens,
                            outputTokens = result.outputTokens
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appLogWriter.error("LLM_PARTIAL", "Failed to persist partial analysis result for recordId: $recordId", e)
            }
        }
    }

    suspend fun snapshot(): DetailedAnalysisResult = mutex.withLock { result }

    suspend fun toJson(): String = mutex.withLock {
        detailedResultSerializer.toJson(result)
    }

    suspend fun combinedSegmentText(): String = mutex.withLock {
        result.segments?.joinToString("") { it.text ?: "" } ?: ""
    }
}
