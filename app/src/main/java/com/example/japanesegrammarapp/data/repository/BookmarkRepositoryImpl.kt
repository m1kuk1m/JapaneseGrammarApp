package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.data.BookmarkDao
import com.example.japanesegrammarapp.data.BookmarkedSegment
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.WordSegment
import com.example.japanesegrammarapp.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val dao: BookmarkDao
) : BookmarkRepository {

    override val allBookmarks: Flow<List<BookmarkedSegmentDomain>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun bookmarkedTextsForRecord(recordId: Int): Flow<Set<String>> =
        dao.getSegmentTextsForRecord(recordId).map { it.toSet() }

    override suspend fun toggleBookmark(
        segment: WordSegment,
        recordId: Int,
        sourceText: String
    ): Boolean {
        val text = segment.text ?: return false
        val isCurrentlyBookmarked = dao.exists(recordId, text).first()
        return if (isCurrentlyBookmarked) {
            dao.deleteByKey(recordId, text)
            false
        } else {
            dao.insert(
                BookmarkedSegment(
                    recordId = recordId,
                    segmentText = text,
                    reading = segment.reading,
                    partOfSpeech = segment.partOfSpeech,
                    posCategory = segment.posCategory,
                    dictionaryForm = segment.dictionaryForm,
                    dictionaryFormReading = segment.dictionaryFormReading,
                    meaning = segment.meaning,
                    inflection = segment.inflection,
                    role = segment.role,
                    bookmarkedAt = System.currentTimeMillis(),
                    sourceText = sourceText
                )
            )
            true
        }
    }

    override suspend fun removeBookmarkById(id: Int) {
        dao.deleteById(id)
    }

    override suspend fun exportToJson(): String {
        val bookmarks = dao.getAll().first()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val root = JSONObject().apply {
            put("version", 1)
            put("exported_at", sdf.format(Date()))
            put("bookmarks", JSONArray().also { arr ->
                bookmarks.forEach { b ->
                    arr.put(JSONObject().apply {
                        put("id", b.id)
                        put("recordId", b.recordId)
                        put("text", b.segmentText)
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
                    })
                }
            })
        }
        return root.toString(2)
    }

    override suspend fun importFromJson(json: String): Int {
        val root = JSONObject(json)
        val arr = root.getJSONArray("bookmarks")
        var inserted = 0
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val entity = BookmarkedSegment(
                recordId = obj.optInt("recordId", -1),
                segmentText = obj.optString("text", ""),
                reading = obj.optStringOrNull("reading"),
                partOfSpeech = obj.optStringOrNull("partOfSpeech"),
                posCategory = obj.optStringOrNull("posCategory"),
                dictionaryForm = obj.optStringOrNull("dictionaryForm"),
                dictionaryFormReading = obj.optStringOrNull("dictionaryFormReading"),
                meaning = obj.optStringOrNull("meaning"),
                inflection = obj.optStringOrNull("inflection"),
                role = obj.optStringOrNull("role"),
                bookmarkedAt = obj.optLong("bookmarkedAt", System.currentTimeMillis()),
                sourceText = obj.optString("sourceText", "")
            )
            if (entity.recordId >= 0 && entity.segmentText.isNotBlank()) {
                val result = dao.insert(entity)
                if (result != -1L) inserted++
            }
        }
        return inserted
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun BookmarkedSegment.toDomain() = BookmarkedSegmentDomain(
        id = id,
        recordId = recordId,
        segmentText = segmentText,
        reading = reading,
        partOfSpeech = partOfSpeech,
        posCategory = posCategory,
        dictionaryForm = dictionaryForm,
        dictionaryFormReading = dictionaryFormReading,
        meaning = meaning,
        inflection = inflection,
        role = role,
        bookmarkedAt = bookmarkedAt,
        sourceText = sourceText
    )

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
    }
}
