package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.data.BookmarkDao
import com.example.japanesegrammarapp.data.BookmarkedSegment
import com.example.japanesegrammarapp.data.BookmarkedSentence
import com.example.japanesegrammarapp.data.BookmarkedSentenceDao
import com.example.japanesegrammarapp.data.BookmarkedGrammarPointDao
import com.example.japanesegrammarapp.data.mapper.toDomain
import com.example.japanesegrammarapp.data.mapper.toEntity
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.WordSegment
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

    override suspend fun exportToJson(includeWords: Boolean, includeSentences: Boolean): String = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val root = JSONObject().apply {
            put("version", 2)
            put("exported_at", sdf.format(Date()))
            
            if (includeWords) {
                val bookmarks = dao.getAll().first()
                put("bookmarks", JSONArray().also { arr ->
                    bookmarks.forEach { b ->
                        arr.put(JSONObject().apply {
                            put("id", b.id)
                            put("recordId", b.recordId)
                            put("text", b.segmentText)
                            putOpt("surfaceForm", b.surfaceForm)
                            putOpt("reading", b.reading)
                            putOpt("partOfSpeech", b.partOfSpeech)
                            putOpt("posCategory", b.posCategory)
                            putOpt("dictionaryForm", b.dictionaryForm)
                            putOpt("dictionaryFormReading", b.dictionaryFormReading)
                            putOpt("meaning", b.meaning)
                            putOpt("inflection", b.inflection)
                            putOpt("role", b.role)
                            put("bookmarkedAt", b.bookmarkedAt)
                            put("sourceText", b.sourceText)
                            put("isArchived", b.isArchived)
                        })
                    }
                })
            }
            
            if (includeSentences) {
                val sentences = sentenceDao.getAll().first()
                put("sentences", JSONArray().also { arr ->
                    sentences.forEach { s ->
                        arr.put(JSONObject().apply {
                            put("id", s.id)
                            put("recordId", s.recordId)
                            put("originalText", s.originalText)
                            putOpt("translation", s.translation)
                            putOpt("analysisResult", s.analysisResult)
                            putOpt("modelUsed", s.modelUsed)
                            put("bookmarkedAt", s.bookmarkedAt)
                        })
                    }
                })
            }
        }
        root.toString(2)
    }

    override suspend fun importFromJson(json: String, includeWords: Boolean, includeSentences: Boolean): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        var inserted = 0
        
        if (includeWords && root.has("bookmarks")) {
            val arr = root.getJSONArray("bookmarks")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val entity = BookmarkedSegment(
                    recordId = obj.optInt("recordId", -1),
                    segmentText = obj.optString("text", ""),
                    surfaceForm = obj.optStringOrNull("surfaceForm"),
                    reading = obj.optStringOrNull("reading"),
                    partOfSpeech = obj.optStringOrNull("partOfSpeech"),
                    posCategory = obj.optStringOrNull("posCategory"),
                    dictionaryForm = obj.optStringOrNull("dictionaryForm"),
                    dictionaryFormReading = obj.optStringOrNull("dictionaryFormReading"),
                    meaning = obj.optStringOrNull("meaning"),
                    inflection = obj.optStringOrNull("inflection"),
                    role = obj.optStringOrNull("role"),
                    bookmarkedAt = obj.optLong("bookmarkedAt", System.currentTimeMillis()),
                    sourceText = obj.optString("sourceText", ""),
                    isArchived = obj.optBoolean("isArchived", false)
                )
                if (entity.recordId >= 0 && entity.segmentText.isNotBlank()) {
                    val result = dao.insert(entity)
                    if (result != -1L) inserted++
                }
            }
        }
        
        if (includeSentences && root.has("sentences")) {
            val arr = root.getJSONArray("sentences")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val entity = BookmarkedSentence(
                    recordId = obj.optInt("recordId", -1),
                    originalText = obj.optString("originalText", ""),
                    translation = obj.optStringOrNull("translation"),
                    analysisResult = obj.optStringOrNull("analysisResult"),
                    modelUsed = obj.optStringOrNull("modelUsed"),
                    bookmarkedAt = obj.optLong("bookmarkedAt", System.currentTimeMillis())
                )
                if (entity.originalText.isNotBlank()) {
                    val result = sentenceDao.insert(entity)
                    if (result != -1L) inserted++
                }
            }
        }
        
        inserted
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
                translation = obj.optString("translation", null) ?: obj.optString("meaning", null)
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

    override suspend fun detachSentenceBookmarkFromRecord(recordId: Int) = withContext(Dispatchers.IO) {
        sentenceDao.detachFromRecord(recordId)
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
    }
}
