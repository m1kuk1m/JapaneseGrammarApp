package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.data.BookmarkDao
import com.example.japanesegrammarapp.data.BookmarkedSegment
import com.example.japanesegrammarapp.data.BookmarkedSentence
import com.example.japanesegrammarapp.data.BookmarkedSentenceDao
import com.example.japanesegrammarapp.data.BookmarkedGrammarPoint
import com.example.japanesegrammarapp.data.BookmarkedGrammarPointDao
import com.example.japanesegrammarapp.data.mapper.toDomain
import com.example.japanesegrammarapp.data.mapper.toEntity
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.WordSegment
import com.example.japanesegrammarapp.domain.model.ConflictStrategy
import com.example.japanesegrammarapp.domain.model.ImportResult
import com.example.japanesegrammarapp.domain.model.ExportFormat
import com.example.japanesegrammarapp.domain.repository.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val dao: BookmarkDao,
    private val sentenceDao: BookmarkedSentenceDao,
    private val grammarPointDao: BookmarkedGrammarPointDao
) : BookmarkRepository {

    override fun getAllGrammarPoints(): Flow<List<BookmarkedGrammarPointDomain>> =
        grammarPointDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getGrammarPointsForRecord(recordId: Int): Flow<List<BookmarkedGrammarPointDomain>> =
        grammarPointDao.getGrammarPointsForRecord(recordId).map { list -> list.map { it.toDomain() } }

    override suspend fun toggleGrammarPointBookmark(
        recordId: Int,
        pattern: String,
        explanation: String?,
        sourceText: String
    ): Boolean = withContext(Dispatchers.IO) {
        grammarPointDao.toggleGrammarPointBookmark(recordId, pattern, explanation, sourceText)
    }

    override suspend fun deleteGrammarPointById(id: Int) = withContext(Dispatchers.IO) {
        grammarPointDao.deleteById(id)
    }

    override suspend fun setGrammarPointArchivedStatus(id: Int, isArchived: Boolean) = withContext(Dispatchers.IO) {
        grammarPointDao.updateArchivedStatus(id, isArchived)
    }

    override suspend fun archiveMultipleGrammarPoints(ids: List<Int>) = withContext(Dispatchers.IO) {
        grammarPointDao.archiveMultiple(ids)
    }

    override val allBookmarks: Flow<List<BookmarkedSegmentDomain>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun bookmarkedTextsForRecord(recordId: Int): Flow<Set<String>> =
        dao.getSegmentTextsForRecord(recordId).map { it.toSet() }

    override suspend fun toggleBookmark(
        segment: WordSegment,
        recordId: Int,
        sourceText: String
    ): Boolean? = withContext(Dispatchers.IO) {
        val surfaceForm = segment.text ?: return@withContext null
        val dictForm = segment.dictionaryForm?.takeIf { it.isNotBlank() } ?: surfaceForm
        val dictReading = segment.dictionaryFormReading?.takeIf { it.isNotBlank() } ?: segment.reading

        dao.toggleBookmark(
            recordId = recordId,
            surfaceForm = surfaceForm,
            dictForm = dictForm,
            dictReading = dictReading ?: "",
            partOfSpeech = segment.partOfSpeech,
            posCategory = segment.posCategory,
            meaning = segment.meaning,
            inflection = segment.inflection,
            role = segment.role,
            sourceText = sourceText
        )
    }

    override suspend fun removeBookmarkById(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    override suspend fun updateWordBookmark(domain: BookmarkedSegmentDomain) = withContext(Dispatchers.IO) {
        val entity = BookmarkedSegment(
            id = domain.id,
            recordId = domain.recordId,
            segmentText = domain.segmentText,
            surfaceForm = domain.surfaceForm,
            reading = domain.reading,
            partOfSpeech = domain.partOfSpeech,
            posCategory = domain.posCategory,
            dictionaryForm = domain.dictionaryForm,
            dictionaryFormReading = domain.dictionaryFormReading,
            meaning = domain.meaning,
            inflection = domain.inflection,
            role = domain.role,
            bookmarkedAt = domain.bookmarkedAt,
            sourceText = domain.sourceText,
            isArchived = domain.isArchived
        )
        dao.update(entity)
    }

    override suspend fun exportData(format: ExportFormat, includeWords: Boolean, includeSentences: Boolean, includeGrammarPoints: Boolean): String = withContext(Dispatchers.IO) {
        val handler = com.example.japanesegrammarapp.data.format.BookmarkFormatHandlerImpl()
        
        val words = if (includeWords) dao.getAll().first().map { it.toDomain() } else emptyList()
        val sentences = if (includeSentences) sentenceDao.getAll().first().map { it.toDomain() } else emptyList()
        val grammarPoints = if (includeGrammarPoints) grammarPointDao.getAll().first().map { it.toDomain() } else emptyList()
        
        handler.exportData(format, words, sentences, grammarPoints)
    }

    override suspend fun checkConflicts(
        data: String,
        format: ExportFormat,
        includeWords: Boolean,
        includeSentences: Boolean,
        includeGrammarPoints: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val handler = com.example.japanesegrammarapp.data.format.BookmarkFormatHandlerImpl()
        val parsed = try {
            handler.importData(data, format)
        } catch (e: Exception) {
            return@withContext false
        }
        
        if (includeWords) {
            for (w in parsed.words) {
                if (dao.existsForImport(w.recordId, w.surfaceForm ?: w.segmentText, w.dictionaryForm ?: w.segmentText)) {
                    return@withContext true
                }
            }
        }
        if (includeSentences) {
            for (s in parsed.sentences) {
                if (sentenceDao.existsByOriginalTextDirect(s.originalText)) {
                    return@withContext true
                }
            }
        }
        if (includeGrammarPoints) {
            for (gp in parsed.grammarPoints) {
                if (grammarPointDao.existsByPatternDirect(gp.recordId, gp.pattern)) {
                    return@withContext true
                }
            }
        }
        return@withContext false
    }

    override suspend fun importData(
        data: String,
        format: ExportFormat,
        includeWords: Boolean,
        includeSentences: Boolean,
        includeGrammarPoints: Boolean,
        conflictStrategy: com.example.japanesegrammarapp.domain.model.ConflictStrategy
    ): ImportResult = withContext(Dispatchers.IO) {
        val handler = com.example.japanesegrammarapp.data.format.BookmarkFormatHandlerImpl()
        val parsed = handler.importData(data, format)
        
        var successCount = 0
        var skippedCount = 0
        var failedCount = 0
        val failureReasons = parsed.failureReasons.toMutableList()
        
        if (includeWords) {
            for (domain in parsed.words) {
                try {
                    val entity = BookmarkedSegment(
                        recordId = domain.recordId,
                        segmentText = domain.segmentText,
                        surfaceForm = domain.surfaceForm ?: domain.segmentText,
                        reading = domain.reading,
                        partOfSpeech = domain.partOfSpeech,
                        posCategory = domain.posCategory,
                        dictionaryForm = domain.dictionaryForm ?: domain.segmentText,
                        dictionaryFormReading = domain.dictionaryFormReading,
                        meaning = domain.meaning,
                        inflection = domain.inflection,
                        role = domain.role,
                        bookmarkedAt = domain.bookmarkedAt,
                        sourceText = domain.sourceText,
                        isArchived = domain.isArchived
                    )
                    if (entity.segmentText.isNotBlank()) {
                        if (conflictStrategy == com.example.japanesegrammarapp.domain.model.ConflictStrategy.OVERWRITE) {
                            dao.deleteForImport(
                                entity.recordId,
                                entity.surfaceForm ?: entity.segmentText,
                                entity.dictionaryForm ?: entity.segmentText
                            )
                        }
                        val result = dao.insert(entity)
                        if (result != -1L) successCount++ else skippedCount++
                    } else {
                        skippedCount++
                    }
                } catch (e: Exception) {
                    failedCount++
                    failureReasons.add("Word import failed: ${e.message}")
                }
            }
        }
        
        if (includeSentences) {
            for (domain in parsed.sentences) {
                try {
                    val entity = BookmarkedSentence(
                        recordId = domain.recordId,
                        originalText = domain.originalText,
                        translation = domain.translation,
                        analysisResult = domain.analysisResult,
                        modelUsed = domain.modelUsed,
                        bookmarkedAt = domain.bookmarkedAt,
                        isArchived = domain.isArchived
                    )
                    if (entity.originalText.isNotBlank()) {
                        if (conflictStrategy == com.example.japanesegrammarapp.domain.model.ConflictStrategy.SKIP) {
                            val exists = sentenceDao.existsByOriginalTextDirect(entity.originalText)
                            if (exists) {
                                skippedCount++
                                continue
                            }
                        } else {
                            sentenceDao.deleteByOriginalText(entity.originalText)
                        }
                        val result = sentenceDao.insert(entity)
                        if (result != -1L) successCount++ else skippedCount++
                    } else {
                        skippedCount++
                    }
                } catch (e: Exception) {
                    failedCount++
                    failureReasons.add("Sentence import failed: ${e.message}")
                }
            }
        }
        
        if (includeGrammarPoints) {
            for (domain in parsed.grammarPoints) {
                try {
                    val entity = BookmarkedGrammarPoint(
                        recordId = domain.recordId,
                        pattern = domain.pattern,
                        explanation = domain.explanation,
                        bookmarkedAt = domain.bookmarkedAt,
                        sourceText = domain.sourceText,
                        isArchived = domain.isArchived
                    )
                    if (entity.pattern.isNotBlank()) {
                        if (conflictStrategy == com.example.japanesegrammarapp.domain.model.ConflictStrategy.OVERWRITE) {
                            grammarPointDao.deleteForImport(entity.recordId, entity.pattern)
                        }
                        val result = grammarPointDao.insert(entity)
                        if (result != -1L) successCount++ else skippedCount++
                    } else {
                        skippedCount++
                    }
                } catch (e: Exception) {
                    failedCount++
                    failureReasons.add("Grammar point import failed: ${e.message}")
                }
            }
        }
        
        ImportResult(successCount, skippedCount, failedCount, failureReasons)
    }

    override suspend fun updateArchivedStatus(id: Int, isArchived: Boolean) = withContext(Dispatchers.IO) {
        dao.updateArchivedStatus(id, isArchived)
    }

    override suspend fun archiveMultiple(ids: List<Int>) = withContext(Dispatchers.IO) {
        dao.archiveMultiple(ids)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun BookmarkedSegment.toDomain() = BookmarkedSegmentDomain(
        id = id,
        recordId = recordId,
        segmentText = segmentText,
        surfaceForm = surfaceForm,
        reading = reading,
        partOfSpeech = partOfSpeech,
        posCategory = posCategory,
        dictionaryForm = dictionaryForm,
        dictionaryFormReading = dictionaryFormReading,
        meaning = meaning,
        inflection = inflection,
        role = role,
        bookmarkedAt = bookmarkedAt,
        sourceText = sourceText,
        isArchived = isArchived
    )

    override val allBookmarkedSentences: Flow<List<BookmarkedSentenceDomain>> =
        sentenceDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun isSentenceBookmarked(recordId: Int): Flow<Boolean> =
        sentenceDao.existsByRecordId(recordId)

    override suspend fun toggleSentenceBookmark(record: AnalysisDomainRecord): Boolean = withContext(Dispatchers.IO) {
        var translation: String? = null
        try {
            if (!record.analysisResult.isNullOrBlank()) {
                val obj = JSONObject(record.analysisResult)
                translation = obj.optString("translation").takeIf { it.isNotEmpty() } ?: obj.optString("meaning").takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        sentenceDao.toggleSentenceBookmark(
            recordId = record.id,
            originalText = record.originalText,
            translation = translation,
            analysisResult = record.analysisResult,
            modelUsed = record.modelUsed
        )
    }

    override suspend fun deleteSentenceBookmark(id: Int) = withContext(Dispatchers.IO) {
        sentenceDao.deleteById(id)
    }

    override suspend fun deleteSentenceBookmarkByRecordId(recordId: Int) = withContext(Dispatchers.IO) {
        sentenceDao.deleteByRecordId(recordId)
    }

    override suspend fun setSentenceArchivedStatus(id: Int, isArchived: Boolean) = withContext(Dispatchers.IO) {
        sentenceDao.updateArchivedStatus(id, isArchived)
    }

    override suspend fun detachSentenceBookmarkFromRecord(recordId: Int) = withContext(Dispatchers.IO) {
        sentenceDao.detachFromRecord(recordId)
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
    }
}
