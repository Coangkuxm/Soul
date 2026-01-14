package com.example.soul.data.remote

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client singleton
 * Configured for Render.com free tier (cold start ~30-60s)
 */
object RetrofitClient {
    
    private const val TAG = "RetrofitClient"
    const val BASE_URL = "https://soul-rqnu.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // Tăng timeout lên 120s vì Render free tier có cold start rất chậm
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true) // Tự động retry khi connection fail
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "Request: ${request.method} ${request.url}")
            
            // Retry logic for cold start
            var response = chain.proceed(request)
            var tryCount = 0
            val maxRetries = 2
            
            while (!response.isSuccessful && tryCount < maxRetries) {
                tryCount++
                Log.d(TAG, "Retry attempt $tryCount for ${request.url}")
                response.close()
                Thread.sleep(2000) // Wait 2s before retry
                response = chain.proceed(request)
            }
            
            response
        }
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
