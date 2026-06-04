package com.example.japanesegrammarapp.domain.repository

import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.WordSegment
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    /** Full bookmark list, ordered by bookmark time descending */
    val allBookmarks: Flow<List<BookmarkedSegmentDomain>>

    /** Set of segmentText keys currently bookmarked for the given record */
    fun bookmarkedTextsForRecord(recordId: Int): Flow<Set<String>>

    /**
     * Toggle bookmark:
     *  - If the segment is already bookmarked for this record, removes it.
     *  - Otherwise, inserts a new bookmark.
     * @return true if the segment is now bookmarked, false if removed.
     */
    suspend fun toggleBookmark(
        segment: WordSegment,
        recordId: Int,
        sourceText: String
    ): Boolean

    suspend fun removeBookmarkById(id: Int)

    /** Serialize all bookmarks to a JSON string for export */
    suspend fun exportToJson(): String

    /**
     * Import bookmarks from a JSON string (produced by [exportToJson]).
     * Duplicates (same recordId + segmentText) are silently skipped.
     * @return number of newly inserted bookmarks
     */
    suspend fun importFromJson(json: String): Int
}
