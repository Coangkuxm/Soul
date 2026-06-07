package com.example.soul.data.remote

import com.example.soul.data.model.CollectionsResponse
import com.example.soul.data.model.CollectionItemsResponse
import com.example.soul.data.model.CommentsResponse
import com.example.soul.data.model.CreateCommentResponse
import com.example.soul.data.model.ChatConversationsResponse
import com.example.soul.data.model.ChatCreateConversationResponse
import com.example.soul.data.model.ChatMessagesResponse
import com.example.soul.data.model.ChatReadConversationResponse
import com.example.soul.data.model.AdminReportedPostsResponse
import com.example.soul.data.model.AdminReportedUsersResponse
import com.example.soul.data.model.FeedResponse
import com.example.soul.data.model.HealthResponse
import com.example.soul.data.model.NotificationsResponse
import com.example.soul.data.model.ProfileResponse
import com.example.soul.data.model.SpotifySearchResponse
import com.example.soul.data.model.TMDBSearchResponse
import com.example.soul.data.model.auth.LoginRequest
import com.example.soul.data.model.auth.LoginResponse
import com.example.soul.data.model.auth.RegisterRequest
import com.example.soul.data.model.auth.ChangePasswordRequest
import com.example.soul.data.model.auth.SimpleResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * API Service interface for Retrofit
 */
interface ApiService {
    
    /**
     * Health check endpoint
     */
    @GET("/health")
    suspend fun testConnection(): HealthResponse
    
    /**
     * Login user with email and password
     */
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("/api/account/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<SimpleResponse>
    
    /**
     * Get current user profile
     */
    @GET("/api/users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<ProfileResponse>
    
    /**
     * Get user's collections
     */
    @GET("/api/collections")
    suspend fun getCollections(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<CollectionsResponse>

    /**
     * Get items in a collection
     */
    @GET("/api/collection-items/{collectionId}/items")
    suspend fun getCollectionItems(
        @Path("collectionId") collectionId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<CollectionItemsResponse>
    
    /**
     * Get user profile by ID
     */
    @GET("/api/users/{id}")
    suspend fun getUserProfile(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<ProfileResponse>

    @GET("/api/users")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("search") keyword: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<JsonObject>

    @POST("/api/users/{id}/follow")
    suspend fun followUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<JsonObject>

    @HTTP(method = "DELETE", path = "/api/users/{id}/unfollow", hasBody = false)
    suspend fun unfollowUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<JsonObject>

    @GET("/api/users/{id}/is-following")
    suspend fun checkIsFollowing(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<JsonObject>
    
    /**
     * Get news feed - random items from friends and others
     */
    @GET("/api/social/feed")
    suspend fun getFeed(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<FeedResponse>

    /**
     * Get comments for a target (collection/item)
     */
    @GET("/api/social/comments/{targetType}/{targetId}")
    suspend fun getComments(
        @Path("targetType") targetType: String,
        @Path("targetId") targetId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<CommentsResponse>

    /**
     * Create a comment
     */
    @POST("/api/social/comments")
    suspend fun createComment(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<CreateCommentResponse>

    @GET("/api/social/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("unreadOnly") unreadOnly: Boolean = false
    ): Response<NotificationsResponse>

    @PUT("/api/social/notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Header("Authorization") token: String,
        @Path("id") notificationId: Int
    ): Response<Map<String, Any?>>

    @POST("/api/chat/conversations")
    suspend fun createOrGetDirectConversation(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ChatCreateConversationResponse>

    @GET("/api/chat/conversations")
    suspend fun getChatConversations(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ChatConversationsResponse>

    @GET("/api/chat/conversations/{id}/messages")
    suspend fun getChatMessages(
        @Header("Authorization") token: String,
        @Path("id") conversationId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): Response<ChatMessagesResponse>

    @POST("/api/chat/conversations/{id}/messages")
    suspend fun sendChatMessage(
        @Header("Authorization") token: String,
        @Path("id") conversationId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @PUT("/api/chat/conversations/{id}/read")
    suspend fun markConversationRead(
        @Header("Authorization") token: String,
        @Path("id") conversationId: Int
    ): Response<ChatReadConversationResponse>

    @POST("/api/reports")
    suspend fun createReport(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @GET("/api/reports/posts")
    suspend fun getReportedPosts(
        @Header("Authorization") token: String,
        @Query("status") status: String = "pending"
    ): Response<AdminReportedPostsResponse>

    @GET("/api/reports/users")
    suspend fun getReportedUsers(
        @Header("Authorization") token: String,
        @Query("status") status: String = "pending"
    ): Response<AdminReportedUsersResponse>

    @retrofit2.http.PATCH("/api/reports/posts/{id}/lock")
    suspend fun lockReportedPost(
        @Header("Authorization") token: String,
        @Path("id") collectionItemId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @retrofit2.http.PATCH("/api/reports/posts/{id}/unlock")
    suspend fun unlockReportedPost(
        @Header("Authorization") token: String,
        @Path("id") collectionItemId: Int
    ): Response<Map<String, Any?>>

    @retrofit2.http.PATCH("/api/reports/users/{id}/lock")
    suspend fun lockReportedUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @retrofit2.http.PATCH("/api/reports/users/{id}/unlock")
    suspend fun unlockReportedUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<Map<String, Any?>>

    /**
     * Like an item
     */
    @POST("/api/social/likes")
    suspend fun likeItem(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Map<String, Any?>>

    /**
     * Unlike an item
     */
    @HTTP(method = "DELETE", path = "/api/social/likes", hasBody = true)
    suspend fun unlikeItem(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Map<String, Any?>>

    /**
     * Create a new collection
     */
    @POST("/api/collections")
    suspend fun createCollection(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    /**
     * Create a new item
     */
    @POST("/api/items")
    suspend fun createItem(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    /**
     * Add item to collection
     */
    @POST("/api/collection-items/{collection_id}/items")
    suspend fun addItemToCollection(
        @Header("Authorization") token: String,
        @Path("collection_id") collectionId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    // ==================== SPOTIFY API ====================
    
    /**
     * Search for tracks on Spotify
     */
    @GET("/api/spotify/search")
    suspend fun searchSpotify(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): Response<SpotifySearchResponse>

    // ==================== TMDB API ====================
    
    /**
     * Search for movies/TV shows on TMDB
     */
    @GET("/api/tmdb/search")
    suspend fun searchTMDB(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): Response<TMDBSearchResponse>
    
    // ==================== UPLOAD API ====================
    
    /**
     * Upload image to Cloudinary
     */
    @Multipart
    @POST("/api/upload/image")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("folder") folder: RequestBody
    ): Response<Map<String, Any?>>

    @GET("/api/items/external/{externalId}")
    suspend fun getItemByExternalId(
        @Header("Authorization") token: String,
        @Path("externalId") externalId: String
    ): Response<Map<String, Any?>>
}
