package com.example.data.repository

import com.example.data.local.BookmarkEntity
import com.example.data.local.CastHistoryEntity
import com.example.data.local.DzCastDao
import com.example.data.local.SavedStreamEntity
import kotlinx.coroutines.flow.Flow

class CastLibraryRepository(private val dao: DzCastDao) {
    val bookmarks: Flow<List<BookmarkEntity>> = dao.observeBookmarks()
    val history: Flow<List<CastHistoryEntity>> = dao.observeHistory()
    val savedStreams: Flow<List<SavedStreamEntity>> = dao.observeSavedStreams()

    suspend fun saveBookmark(title: String, url: String) {
        dao.insertBookmark(BookmarkEntity(title = title.ifBlank { url }, url = url))
    }

    suspend fun removeBookmark(id: Long) = dao.deleteBookmark(id)

    suspend fun removeBookmarkByUrl(url: String) = dao.deleteBookmarkByUrl(url)

    suspend fun recordCastHistory(
        title: String,
        mediaUrl: String,
        formatTag: String,
        targetDeviceName: String
    ) {
        dao.insertHistory(
            CastHistoryEntity(
                title = title.ifBlank { "بث فيديو" },
                mediaUrl = mediaUrl,
                formatTag = formatTag,
                targetDeviceName = targetDeviceName
            )
        )
    }

    suspend fun deleteHistory(id: Long) = dao.deleteHistory(id)

    suspend fun clearHistory() = dao.clearHistory()

    suspend fun saveStream(title: String, url: String, category: String, formatTag: String) {
        dao.insertSavedStream(
            SavedStreamEntity(
                title = title.ifBlank { "رابط بث مباشر" },
                streamUrl = url,
                category = category,
                formatTag = formatTag
            )
        )
    }

    suspend fun saveStreams(streams: List<SavedStreamEntity>) {
        if (streams.isNotEmpty()) {
            dao.insertSavedStreams(streams)
        }
    }

    suspend fun deleteSavedStream(id: Long) = dao.deleteSavedStream(id)
}
