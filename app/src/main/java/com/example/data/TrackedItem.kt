package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_items")
data class TrackedItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val platform: String, // e.g., "Amazon", "eBay", "Walmart", "Best Buy"
    val currentPrice: Double,
    val originalPrice: Double,
    val historicalLow: Double,
    val historicalHigh: Double,
    val imageUrl: String?,
    val productUrl: String,
    val targetPrice: Double, // Price limit set by user to trigger notification
    val isWatched: Boolean = true,
    val category: String, // e.g., "Electronics", "Home & Kitchen", "Books", "Fashion"
    val rating: Double = 4.0,
    val addedAt: Long = System.currentTimeMillis()
) {
    // Helper to calculate discount percentage
    val discountPercentage: Int
        get() = if (originalPrice > 0) {
            (((originalPrice - currentPrice) / originalPrice) * 100).toInt()
        } else {
            0
        }

    // Is it an exceptional value-for-money deal?
    // (e.g., current price is discounted >= 30% OR is at historical low)
    val isExceptionalDeal: Boolean
        get() = currentPrice <= historicalLow || discountPercentage >= 30
}

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = TrackedItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class PriceHistoryPoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val price: Double,
    val timestamp: Long
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val itemName: String,
    val platform: String,
    val oldPrice: Double,
    val newPrice: Double,
    val alertType: String, // "HISTORICAL_LOW", "EXCEPTIONAL_DEAL", "TARGET_MET"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
