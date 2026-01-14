package com.example.soul.data.repository

import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.Collection
import com.example.soul.data.model.UserProfile
import com.example.soul.data.remote.ApiService
import com.example.soul.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * Repository for Home screen operations
 */
class HomeRepository(
    private val apiService: ApiService,
    private val authPreferences: AuthPreferences
) {
    
    /**
     * Get current user profile
     */
    suspend fun getCurrentUserProfile(): Resource<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authPreferences.getToken() ?: return@withContext Resource.Error("Not logged in")
                val response = apiService.getCurrentUser("Bearer $token")
                
                if (response.isSuccessful) {
                    val profile = response.body()?.user
                    if (profile != null) {
                        Resource.Success(profile)
                    } else {
                        Resource.Error("Failed to get profile")
                    }
                } else {
                    Resource.Error("Error: ${response.code()}")
                }
            } catch (e: SocketTimeoutException) {
                Resource.Error("⏳ Server đang khởi động. Vui lòng đợi...")
            } catch (e: ConnectException) {
                Resource.Error("❌ Không thể kết nối đến server")
            } catch (e: UnknownHostException) {
                Resource.Error("❌ Kiểm tra kết nối mạng")
            } catch (e: Exception) {
                Resource.Error("❌ ${e.message}")
            }
        }
    }
    
    /**
     * Get user's collections
     */
    suspend fun getUserCollections(userId: Int? = null): Resource<List<Collection>> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authPreferences.getToken() ?: return@withContext Resource.Error("Not logged in")
                val user = authPreferences.getUser()
                val targetUserId = userId ?: user?.id
                
                val response = apiService.getCollections(
                    token = "Bearer $token",
                    userId = targetUserId,
                    limit = 20
                )
                
                if (response.isSuccessful) {
                    val collections = response.body()?.data ?: emptyList()
                    Resource.Success(collections)
                } else {
                    Resource.Error("Error: ${response.code()}")
                }
            } catch (e: SocketTimeoutException) {
                Resource.Error("⏳ Server đang khởi động. Vui lòng đợi...")
            } catch (e: ConnectException) {
                Resource.Error("❌ Không thể kết nối đến server")
            } catch (e: UnknownHostException) {
                Resource.Error("❌ Kiểm tra kết nối mạng")
            } catch (e: Exception) {
                Resource.Error("❌ ${e.message}")
            }
        }
    }
    
    /**
     * Get local user data
     */
    fun getLocalUser() = authPreferences.getUser()
    
    /**
     * Check if logged in
     */
    fun isLoggedIn() = authPreferences.isLoggedIn()
    
    /**
     * Logout
     */
    fun logout() = authPreferences.clearSession()
}
