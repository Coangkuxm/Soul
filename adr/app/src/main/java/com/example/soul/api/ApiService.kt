package com.example.soul.api

import com.example.soul.data.HealthResponse
import retrofit2.http.GET

interface ApiService {
    @GET("/health")
    suspend fun testConnection(): HealthResponse
}
