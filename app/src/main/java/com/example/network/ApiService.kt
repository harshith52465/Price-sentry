package com.example.network

import com.example.data.NotificationItem
import com.example.data.TrackedItem
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // 1. Fetch all items (optional filters)
    @GET("api/v1/deals")
    suspend fun getAllDeals(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): Response<List<TrackedItem>>

    // 2. Track new product
    @POST("api/v1/deals")
    suspend fun createDeal(
        @Body payload: TrackedItem
    ): Response<TrackedItem>

    // 3. Get single item with history or stats
    @GET("api/v1/deals/{id}")
    suspend fun getDealById(
        @Path("id") id: Int
    ): Response<TrackedItem>

    // 4. Update threshold
    @PUT("api/v1/deals/{id}/target-price")
    suspend fun updateTargetPrice(
        @Path("id") id: Int,
        @Body payload: Map<String, Double>
    ): Response<TrackedItem>

    // 5. Untrack and remove item
    @DELETE("api/v1/deals/{id}")
    suspend fun deleteTrackedItem(
        @Path("id") id: Int
    ): Response<Unit>

    // 6. Get push notification log
    @GET("api/v1/notifications")
    suspend fun getNotifications(): Response<List<NotificationItem>>

    // 7. Clear unread payloads
    @POST("api/v1/notifications/clear-unread")
    suspend fun clearAllUnread(): Response<Map<String, String>>

    // 8. Trigger backend simulation drop injection
    @POST("api/v1/deals/{id}/simulate-drop")
    suspend fun simulatePriceDrop(
        @Path("id") id: Int,
        @Query("price") price: Double
    ): Response<Map<String, String>>
}
