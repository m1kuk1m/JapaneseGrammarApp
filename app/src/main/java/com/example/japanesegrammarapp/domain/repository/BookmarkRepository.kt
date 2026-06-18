package com.example.japanesegrammarapp.domain.repository

import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.WordSegment
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    /** Full bookmark list, ordered by bookmark time descending */
    val allBookmarks: Flow<List<BookmarkedSegmentDomain>>

    /** Set of segmentText keys currently bookmarked for the given record */
    fun bookmarkedTextsForRecord(recordId: Int): Flow<Set<String>>

    /**
     * Toggle bookmark for a word segment within a sentence.
     * - If the specific surfaceForm is already bookmarked in this sentence → removes it (取消收藏)
     * - Otherwise → inserts a new bookmark, keyed by dictionaryForm (falling back to surface text)
     * @return true if the segment is now bookmarked, false if removed, -1 on failure
     */
    suspend fun toggleBookmark(
        segment: WordSegment,
        recordId: Int,
        sourceText: String
    ): Boolean?

    suspend fun removeBookmarkById(id: Int)

    /** Serialize bookmarks to a JSON string for export */
    suspend fun exportToJson(includeWords: Boolean, includeSentences: Boolean): String

    /**
     * Import bookmarks from a JSON string (produced by [exportToJson]).
     * Duplicates are silently skipped.
     * @return number of newly inserted bookmarks
     */
    suspend fun importFromJson(json: String, includeWords: Boolean, includeSentences: Boolean): Int

    suspend fun updateArchivedStatus(id: Int, isArchived: Boolean)

    suspend fun archiveMultiple(ids: List<Int>)

    // ── Sentence Bookmarks ──────────────────────────────────────────────────
    val allBookmarkedSentences: Flow<List<BookmarkedSentenceDomain>>
    fun isSentenceBookmarked(recordId: Int): Flow<Boolean>
    suspend fun toggleSentenceBookmark(record: AnalysisDomainRecord): Boolean
    suspend fun deleteSentenceBookmark(id: Int)
    suspend fun deleteSentenceBookmarkByRecordId(recordId: Int)
    suspend fun detachSentenceBookmarkFromRecord(recordId: Int)
}
