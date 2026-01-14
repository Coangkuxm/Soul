package com.example.soul.data.remote

import com.example.soul.data.model.CollectionsResponse
import com.example.soul.data.model.HealthResponse
import com.example.soul.data.model.ProfileResponse
import com.example.soul.data.model.auth.LoginRequest
import com.example.soul.data.model.auth.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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
}
