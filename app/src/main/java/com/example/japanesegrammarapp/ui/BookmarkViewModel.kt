package com.example.japanesegrammarapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    val allBookmarks: StateFlow<List<BookmarkedSegmentDomain>> =
        bookmarkRepository.allBookmarks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmarkById(id)
        }
    }

    /**
     * Export all bookmarks as a JSON file and share it via system share sheet.
     */
    fun exportAndShare(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = bookmarkRepository.exportToJson()
                val fileName = "bookmarks_${System.currentTimeMillis()}.json"
                val file = File(context.cacheDir, fileName)
                file.writeText(json, Charsets.UTF_8)
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "导出收藏")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Import bookmarks from a JSON string (e.g. picked from file picker).
     * @return number of newly imported bookmarks, or -1 on error
     */
    suspend fun importFromJson(json: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkRepository.importFromJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
                -1
            }
        }
    }
}
