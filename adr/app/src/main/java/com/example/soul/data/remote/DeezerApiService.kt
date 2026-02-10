package com.example.soul.data.remote

import com.example.soul.data.model.DeezerSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DeezerApiService {
    @GET("search")
    suspend fun search(@Query("q") query: String): DeezerSearchResponse
}
