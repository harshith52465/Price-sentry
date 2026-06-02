package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ProductRepository(database.productDao)

    // UI filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Core repository flows
    val allTrackedItems: StateFlow<List<TrackedItem>> = repository.allTrackedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlistItems: StateFlow<List<TrackedItem>> = repository.watchlistItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationItem>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filtered lists combining allTrackedItems + search + category
    val filteredTrackedItems: StateFlow<List<TrackedItem>> = combine(
        allTrackedItems,
        _searchQuery,
        _selectedCategory
    ) { items, query, category ->
        items.filter { item ->
            val matchesQuery = item.name.contains(query, ignoreCase = true) ||
                    item.platform.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || item.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected item for details/charts view
    private val _selectedItemId = MutableStateFlow<Int?>(null)
    val selectedItemId: StateFlow<Int?> = _selectedItemId.asStateFlow()

    val selectedItem: StateFlow<TrackedItem?> = _selectedItemId
        .flatMapLatest { id ->
            if (id != null) {
                repository.allTrackedItems.map { list -> list.find { it.id == id } }
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedItemPriceHistory: StateFlow<List<PriceHistoryPoint>> = _selectedItemId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getPriceHistoryForItem(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize with default demo dataset
        viewModelScope.launch {
            repository.checkAndSeedData()
        }
    }

    // Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectItem(itemId: Int?) {
        _selectedItemId.value = itemId
    }

    fun toggleWatchlist(itemId: Int) {
        viewModelScope.launch {
            repository.toggleWatchlist(itemId)
        }
    }

    fun updateTargetPrice(itemId: Int, targetPrice: Double) {
        viewModelScope.launch {
            repository.updateTargetPrice(itemId, targetPrice)
        }
    }

    fun deleteTrackedItem(itemId: Int) {
        viewModelScope.launch {
            repository.deleteTrackedItemById(itemId)
            if (_selectedItemId.value == itemId) {
                _selectedItemId.value = null
            }
        }
    }

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun deleteNotification(id: Int) {
        viewModelScope.launch {
            repository.deleteNotificationById(id)
        }
    }

    // Simulate Python scraping backend updates
    // This allows testing alerts and historical low updates instantly in-app
    fun simulatePriceUpdate(itemId: Int, newPrice: Double) {
        viewModelScope.launch {
            repository.simulateBackendPriceDrop(itemId, newPrice)
        }
    }

    // Add manual product
    fun addNewTrackedItem(
        name: String,
        platform: String,
        currentPrice: Double,
        originalPrice: Double,
        category: String,
        targetPrice: Double,
        productUrl: String,
        imageUrl: String? = null
    ) {
        viewModelScope.launch {
            val finalImage = if (!imageUrl.isNullOrBlank()) {
                imageUrl
            } else {
                // Return fallback image based on category
                when (category) {
                    "Electronics" -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500&q=80"
                    "Home & Kitchen" -> "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=500&q=80"
                    "Books" -> "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500&q=80"
                    else -> "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&q=80"
                }
            }

            val newItem = TrackedItem(
                name = name,
                platform = platform,
                currentPrice = currentPrice,
                originalPrice = originalPrice,
                historicalLow = currentPrice,
                historicalHigh = originalPrice.coerceAtLeast(currentPrice),
                imageUrl = finalImage,
                productUrl = productUrl.ifEmpty { "https://www.${platform.lowercase().replace(" ", "")}.com" },
                targetPrice = targetPrice,
                category = category,
                isWatched = true,
                rating = 4.0
            )

            val newId = repository.insertTrackedItem(newItem).toInt()
            // Add initial history point
            repository.addPriceHistoryPoint(itemId = newId, price = currentPrice)
        }
    }
}
