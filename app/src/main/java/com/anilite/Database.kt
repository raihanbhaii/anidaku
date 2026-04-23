package com.anilite

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: Int,
    val title: String,
    val coverImage: String?,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey val episodeId: String,
    val animeId: Int,
    val animeTitle: String,
    val episodeNumber: Int,
    val position: Long,
    val duration: Long,
    val watchedAt: Long = System.currentTimeMillis()
)

@Dao
interface AnimeDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlist(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistItem)

    @Delete
    suspend fun removeFromWatchlist(item: WatchlistItem)

    @Query("SELECT * FROM history ORDER BY watchedAt DESC")
    fun getHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToHistory(item: HistoryItem)
}

@Database(entities = [WatchlistItem::class, HistoryItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
}
