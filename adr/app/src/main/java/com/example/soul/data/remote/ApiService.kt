package com.example.soul.data.remote

import com.example.soul.data.model.CollectionsResponse
import com.example.soul.data.model.FeedResponse
import com.example.soul.data.model.HealthResponse
import com.example.soul.data.model.ProfileResponse
import com.example.soul.data.model.SpotifySearchResponse
import com.example.soul.data.model.TMDBSearchResponse
import com.example.soul.data.model.auth.LoginRequest
import com.example.soul.data.model.auth.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
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
     * Get user profile by ID
     */
    @GET("/api/users/{id}")
    suspend fun getUserProfile(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<ProfileResponse>
    
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
}
