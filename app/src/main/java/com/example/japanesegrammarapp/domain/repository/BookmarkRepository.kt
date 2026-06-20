package com.example.japanesegrammarapp.domain.repository

import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.WordSegment
import com.example.japanesegrammarapp.domain.model.ConflictStrategy
import com.example.japanesegrammarapp.domain.model.ExportFormat
import com.example.japanesegrammarapp.domain.model.ImportResult
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

    /** Serialize bookmarks to a string for export */
    suspend fun exportData(format: ExportFormat, includeWords: Boolean, includeSentences: Boolean, includeGrammarPoints: Boolean): String

    /**
     * Pre-flight check before importing.
     * @return true if there are duplicate bookmarks in the database, false otherwise.
     */
    suspend fun checkConflicts(data: String, format: ExportFormat, includeWords: Boolean, includeSentences: Boolean, includeGrammarPoints: Boolean): Boolean

    /**
     * Import bookmarks from a string.
     * @return ImportResult containing counts and failure reasons
     */
    suspend fun importData(
        data: String,
        format: ExportFormat,
        includeWords: Boolean,
        includeSentences: Boolean,
        includeGrammarPoints: Boolean,
        conflictStrategy: ConflictStrategy = ConflictStrategy.SKIP
    ): ImportResult

    suspend fun updateArchivedStatus(id: Int, isArchived: Boolean)

    suspend fun archiveMultiple(ids: List<Int>)

    // ── Sentence Bookmarks ──────────────────────────────────────────────────
    val allBookmarkedSentences: Flow<List<BookmarkedSentenceDomain>>
    fun isSentenceBookmarked(recordId: Int): Flow<Boolean>
    suspend fun toggleSentenceBookmark(record: AnalysisDomainRecord): Boolean
    suspend fun deleteSentenceBookmark(id: Int)
    suspend fun deleteSentenceBookmarkByRecordId(recordId: Int)
    suspend fun detachSentenceBookmarkFromRecord(recordId: Int)

    // ── Grammar Point Bookmarks ─────────────────────────────────────────────
    fun getAllGrammarPoints(): Flow<List<BookmarkedGrammarPointDomain>>
    fun getGrammarPointsForRecord(recordId: Int): Flow<List<BookmarkedGrammarPointDomain>>
    suspend fun toggleGrammarPointBookmark(recordId: Int, pattern: String, explanation: String?, sourceText: String): Boolean
    suspend fun deleteGrammarPointById(id: Int)
    suspend fun setGrammarPointArchivedStatus(id: Int, isArchived: Boolean)
    suspend fun archiveMultipleGrammarPoints(ids: List<Int>)
}
