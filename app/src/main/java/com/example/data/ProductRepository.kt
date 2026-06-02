package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(private val productDao: ProductDao) {

    val allTrackedItems: Flow<List<TrackedItem>> = productDao.getAllTrackedItems()
    val watchlistItems: Flow<List<TrackedItem>> = productDao.getWatchlistItems()
    val allNotifications: Flow<List<NotificationItem>> = productDao.getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = productDao.getUnreadNotificationsCount()

    fun getPriceHistoryForItem(itemId: Int): Flow<List<PriceHistoryPoint>> {
        return productDao.getPriceHistoryForItem(itemId)
    }

    suspend fun getTrackedItemById(id: Int): TrackedItem? = withContext(Dispatchers.IO) {
        productDao.getTrackedItemById(id)
    }

    suspend fun insertTrackedItem(item: TrackedItem): Long = withContext(Dispatchers.IO) {
        productDao.insertTrackedItem(item)
    }

    suspend fun updateTrackedItem(item: TrackedItem) = withContext(Dispatchers.IO) {
        productDao.updateTrackedItem(item)
    }

    suspend fun updateTargetPrice(id: Int, newTarget: Double) = withContext(Dispatchers.IO) {
        val item = productDao.getTrackedItemById(id)
        if (item != null) {
            productDao.updateTrackedItem(item.copy(targetPrice = newTarget))
        }
    }

    suspend fun toggleWatchlist(id: Int) = withContext(Dispatchers.IO) {
        val item = productDao.getTrackedItemById(id)
        if (item != null) {
            productDao.updateTrackedItem(item.copy(isWatched = !item.isWatched))
        }
    }

    suspend fun deleteTrackedItemById(id: Int) = withContext(Dispatchers.IO) {
        productDao.deletePriceHistoryForItem(id)
        productDao.deleteTrackedItemById(id)
    }

    suspend fun addPriceHistoryPoint(itemId: Int, price: Double, timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = itemId, price = price, timestamp = timestamp))
    }

    suspend fun insertNotification(notification: NotificationItem) = withContext(Dispatchers.IO) {
        productDao.insertNotification(notification)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        productDao.markAllNotificationsAsRead()
    }

    suspend fun markNotificationAsRead(id: Int) = withContext(Dispatchers.IO) {
        productDao.markNotificationAsRead(id)
    }

    suspend fun deleteNotificationById(id: Int) = withContext(Dispatchers.IO) {
        productDao.deleteNotificationById(id)
    }

    // Pushes simulated price update (e.g., simulating what the Python scraping backend sends)
    suspend fun simulateBackendPriceDrop(itemId: Int, newPrice: Double) = withContext(Dispatchers.IO) {
        val item = productDao.getTrackedItemById(itemId) ?: return@withContext
        val oldPrice = item.currentPrice
        val timestamp = System.currentTimeMillis()

        // 1. Calculate new highs/lows
        val updatedLow = if (newPrice < item.historicalLow) newPrice else item.historicalLow
        val updatedHigh = if (newPrice > item.historicalHigh) newPrice else item.historicalHigh

        // 2. Update item
        val updatedItem = item.copy(
            currentPrice = newPrice,
            historicalLow = updatedLow,
            historicalHigh = updatedHigh
        )
        productDao.updateTrackedItem(updatedItem)

        // 3. Log into historical prices
        productDao.insertPriceHistoryPoint(
            PriceHistoryPoint(itemId = itemId, price = newPrice, timestamp = timestamp)
        )

        // 4. Create alerts if applicable
        var notificationCreated = false
        if (newPrice <= updatedLow && !notificationCreated) {
            productDao.insertNotification(
                NotificationItem(
                    itemId = itemId,
                    itemName = item.name,
                    platform = item.platform,
                    oldPrice = oldPrice,
                    newPrice = newPrice,
                    alertType = "HISTORICAL_LOW",
                    timestamp = timestamp
                )
            )
            notificationCreated = true
        }

        if (newPrice <= item.targetPrice && !notificationCreated) {
            productDao.insertNotification(
                NotificationItem(
                    itemId = itemId,
                    itemName = item.name,
                    platform = item.platform,
                    oldPrice = oldPrice,
                    newPrice = newPrice,
                    alertType = "TARGET_MET",
                    timestamp = timestamp
                )
            )
            notificationCreated = true
        }

        val discountPercentage = if (item.originalPrice > 0) {
            (((item.originalPrice - newPrice) / item.originalPrice) * 100).toInt()
        } else {
            0
        }
        if (discountPercentage >= 35 && !notificationCreated) {
            productDao.insertNotification(
                NotificationItem(
                    itemId = itemId,
                    itemName = item.name,
                    platform = item.platform,
                    oldPrice = oldPrice,
                    newPrice = newPrice,
                    alertType = "EXCEPTIONAL_DEAL",
                    timestamp = timestamp
                )
            )
        }
    }

    // Seeding demo data
    suspend fun checkAndSeedData() = withContext(Dispatchers.IO) {
        val existing = productDao.getAllTrackedItems().first()
        if (existing.isNotEmpty()) return@withContext // Database standard check

        // Seed 6 beautiful tracker items with high-res placeholders/images (INR and Indian Retailers)
        val itemsToSeed = listOf(
            TrackedItem(
                id = 1,
                name = "Sony WH-1000XM4 Noise Canceling Headphones",
                platform = "Amazon IN",
                currentPrice = 19990.00,
                originalPrice = 29990.00,
                historicalLow = 19990.00,
                historicalHigh = 29990.00,
                imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&q=80",
                productUrl = "https://www.amazon.in/dp/B086383Y1Y",
                targetPrice = 22000.00,
                isWatched = true,
                category = "Electronics",
                rating = 4.8
            ),
            TrackedItem(
                id = 2,
                name = "Apple iPad Air (Wi-Fi, 64GB) - Space Gray",
                platform = "Flipkart",
                currentPrice = 49900.00,
                originalPrice = 59900.00,
                historicalLow = 49900.00,
                historicalHigh = 59900.00,
                imageUrl = "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&q=80",
                productUrl = "https://www.flipkart.com",
                targetPrice = 52000.00,
                isWatched = true,
                category = "Electronics",
                rating = 4.7
            ),
            TrackedItem(
                id = 3,
                name = "Dell UltraSharp 27\" Hub USB-C Monitor 4K",
                platform = "Amazon IN",
                currentPrice = 34999.00,
                originalPrice = 45999.00,
                historicalLow = 32999.00,
                historicalHigh = 45999.00,
                imageUrl = "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500&q=80",
                productUrl = "https://www.amazon.in",
                targetPrice = 36000.00,
                isWatched = true,
                category = "Electronics",
                rating = 4.5
            ),
            TrackedItem(
                id = 4,
                name = "Kent Instant Multi-Cooker & Egg Boiler (600W)",
                platform = "Reliance Digital",
                currentPrice = 1299.00,
                originalPrice = 2499.00,
                historicalLow = 1299.00,
                historicalHigh = 2499.00,
                imageUrl = "https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?w=500&q=80",
                productUrl = "https://www.reliancedigital.in",
                targetPrice = 1500.00,
                isWatched = true,
                category = "Home & Kitchen",
                rating = 4.6
            ),
            TrackedItem(
                id = 5,
                name = "Adidas UltraBoost Light Running Shoes",
                platform = "Myntra",
                currentPrice = 11899.00,
                originalPrice = 17999.00,
                historicalLow = 10999.00,
                historicalHigh = 17999.00,
                imageUrl = "https://images.unsplash.com/photo-1548883354-7622d03aca27?w=500&q=80",
                productUrl = "https://www.myntra.com",
                targetPrice = 13000.00,
                isWatched = false,
                category = "Fashion",
                rating = 4.4
            ),
            TrackedItem(
                id = 6,
                name = "Atomic Habits (Paperback Book) by James Clear",
                platform = "Amazon IN",
                currentPrice = 399.00,
                originalPrice = 799.00,
                historicalLow = 399.00,
                historicalHigh = 799.00,
                imageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500&q=80",
                productUrl = "https://www.amazon.in/dp/0735211299",
                targetPrice = 450.00,
                isWatched = true,
                category = "Books",
                rating = 4.9
            )
        )

        for (item in itemsToSeed) {
            productDao.insertTrackedItem(item)
        }

        // Fill price history over custom timestamps in the past
        val msInDay = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        // Item 1 History (Sony)
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 1, price = 29990.00, timestamp = now - 30 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 1, price = 27500.00, timestamp = now - 20 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 1, price = 25000.00, timestamp = now - 12 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 1, price = 22000.00, timestamp = now - 5 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 1, price = 19990.00, timestamp = now - 1 * msInDay))

        // Item 2 History (Apple iPad)
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 2, price = 59900.00, timestamp = now - 25 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 2, price = 56900.00, timestamp = now - 18 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 2, price = 54900.00, timestamp = now - 10 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 2, price = 52900.00, timestamp = now - 4 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 2, price = 49900.00, timestamp = now - 1 * msInDay))

        // Item 3 History (Dell)
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 3, price = 45999.00, timestamp = now - 28 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 3, price = 42999.00, timestamp = now - 15 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 3, price = 32999.00, timestamp = now - 8 * msInDay)) 
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 3, price = 34999.00, timestamp = now - 2 * msInDay)) 

        // Item 4 History (Kent)
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 4, price = 2499.00, timestamp = now - 20 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 4, price = 1999.00, timestamp = now - 14 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 4, price = 1599.00, timestamp = now - 8 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 4, price = 1299.00, timestamp = now - 1 * msInDay))

        // Item 5 History (Adidas)
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 5, price = 17999.00, timestamp = now - 15 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 5, price = 15999.00, timestamp = now - 9 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 5, price = 10999.00, timestamp = now - 4 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 5, price = 11899.00, timestamp = now - 1 * msInDay))

        // Item 6 History (Atomic Habits)
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 6, price = 799.00, timestamp = now - 35 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 6, price = 699.00, timestamp = now - 25 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 6, price = 499.00, timestamp = now - 15 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 6, price = 450.00, timestamp = now - 7 * msInDay))
        productDao.insertPriceHistoryPoint(PriceHistoryPoint(itemId = 6, price = 399.00, timestamp = now - 1 * msInDay))

        // Seed some initial notifications
        productDao.insertNotification(
            NotificationItem(
                id = 1,
                itemId = 1,
                itemName = "Sony WH-1000XM4 Noise Canceling Headphones",
                platform = "Amazon IN",
                oldPrice = 25000.00,
                newPrice = 19990.00,
                alertType = "HISTORICAL_LOW",
                timestamp = now - 10 * 60 * 1000, // 10 mins ago
                isRead = false
            )
        )
        productDao.insertNotification(
            NotificationItem(
                id = 2,
                itemId = 6,
                itemName = "Atomic Habits (Paperback Book)",
                platform = "Amazon IN",
                oldPrice = 450.00,
                newPrice = 399.00,
                alertType = "TARGET_MET",
                timestamp = now - 2 * 60 * 60 * 1000, // 2 hours ago
                isRead = false
            )
        )
        productDao.insertNotification(
            NotificationItem(
                id = 3,
                itemId = 4,
                itemName = "Kent Instant Multi-Cooker & Egg Boiler",
                platform = "Reliance Digital",
                oldPrice = 1799.00,
                newPrice = 1299.00,
                alertType = "EXCEPTIONAL_DEAL",
                timestamp = now - 12 * 60 * 60 * 1000, // 12 hours ago
                isRead = true
            )
        )
    }
}
