package com.example.soul.data.repository

import com.example.soul.data.model.HealthResponse
import com.example.soul.data.remote.ApiService
import com.example.soul.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * Repository for main/home operations
 */
class MainRepository(
    private val apiService: ApiService
) {
    
    /**
     * Test server connection
     */
    suspend fun testConnection(): Resource<HealthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.testConnection()
                Resource.Success(response)
            } catch (e: SocketTimeoutException) {
                Resource.Error("⏳ Server đang khởi động (cold start).\nRender free tier cần 30-60 giây để thức dậy.\nVui lòng thử lại!")
            } catch (e: ConnectException) {
                Resource.Error("❌ Không thể kết nối đến server.\nKiểm tra kết nối mạng của bạn.")
            } catch (e: UnknownHostException) {
                Resource.Error("❌ Không tìm thấy server.\nKiểm tra kết nối mạng của bạn.")
            } catch (e: Exception) {
                Resource.Error("❌ Lỗi: ${e.message}")
            }
        }
    }
}
