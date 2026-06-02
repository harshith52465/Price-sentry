package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    // Tracked Items
    @Query("SELECT * FROM tracked_items ORDER BY addedAt DESC")
    fun getAllTrackedItems(): Flow<List<TrackedItem>>

    @Query("SELECT * FROM tracked_items WHERE isWatched = 1 ORDER BY addedAt DESC")
    fun getWatchlistItems(): Flow<List<TrackedItem>>

    @Query("SELECT * FROM tracked_items WHERE id = :id")
    suspend fun getTrackedItemById(id: Int): TrackedItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedItem(item: TrackedItem): Long

    @Update
    suspend fun updateTrackedItem(item: TrackedItem)

    @Delete
    suspend fun deleteTrackedItem(item: TrackedItem)

    @Query("DELETE FROM tracked_items WHERE id = :id")
    suspend fun deleteTrackedItemById(id: Int)

    // Price History
    @Query("SELECT * FROM price_history WHERE itemId = :itemId ORDER BY timestamp ASC")
    fun getPriceHistoryForItem(itemId: Int): Flow<List<PriceHistoryPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistoryPoint(point: PriceHistoryPoint): Long

    @Query("DELETE FROM price_history WHERE itemId = :itemId")
    suspend fun deletePriceHistoryForItem(itemId: Int)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem): Long

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Int)
}
