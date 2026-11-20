package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cast_history")
data class CastHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val mediaUrl: String,
    val formatTag: String,
    val targetDeviceName: String,
    val castedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_streams")
data class SavedStreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val streamUrl: String,
    val category: String = "IPTV",
    val formatTag: String = "HLS",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DzCastDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT * FROM cast_history ORDER BY castedAt DESC LIMIT 50")
    fun observeHistory(): Flow<List<CastHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: CastHistoryEntity)

    @Query("DELETE FROM cast_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM cast_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM saved_streams ORDER BY createdAt DESC")
    fun observeSavedStreams(): Flow<List<SavedStreamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedStream(stream: SavedStreamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedStreams(streams: List<SavedStreamEntity>)

    @Query("DELETE FROM saved_streams WHERE id = :id")
    suspend fun deleteSavedStream(id: Long)
}

@Database(
    entities = [BookmarkEntity::class, CastHistoryEntity::class, SavedStreamEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DzCastDatabase : RoomDatabase() {
    abstract fun dao(): DzCastDao

    companion object {
        @Volatile
        private var INSTANCE: DzCastDatabase? = null

        fun getInstance(context: Context): DzCastDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DzCastDatabase::class.java,
                    "dz_cast_local.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
