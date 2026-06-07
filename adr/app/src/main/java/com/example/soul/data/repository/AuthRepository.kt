package com.example.soul.data.repository

import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.User
import com.example.soul.data.model.auth.ChangePasswordRequest
import com.example.soul.data.model.auth.LoginRequest
import com.example.soul.data.model.auth.LoginResponse
import com.example.soul.data.model.auth.RegisterRequest
import com.example.soul.data.model.auth.SimpleResponse
import com.example.soul.data.remote.ApiService
import com.example.soul.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * Repository for authentication operations
 * Single source of truth for auth data
 */
class AuthRepository(
    private val apiService: ApiService,
    private val authPreferences: AuthPreferences
) {
    
    /**
     * Login user with email and password
     */
    suspend fun login(email: String, password: String): Resource<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(email, password)
                val response = apiService.login(request)
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    
                    if (loginResponse?.success == true && loginResponse.token != null && loginResponse.user != null) {
                        // Save session locally
                        authPreferences.saveLoginSession(loginResponse.token, loginResponse.user)
                        Resource.Success(loginResponse)
                    } else {
                        val errorMessage = loginResponse?.message ?: loginResponse?.error ?: "Login failed"
                        Resource.Error(errorMessage)
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Email hoặc mật khẩu không đúng"
                        429 -> "Quá nhiều yêu cầu. Vui lòng thử lại sau."
                        500 -> "Lỗi server. Vui lòng thử lại sau."
                        else -> "Lỗi đăng nhập: ${response.code()}"
                    }
                    Resource.Error(errorMessage)
                }
            } catch (e: SocketTimeoutException) {
                Resource.Error("⏳ Server đang khởi động (cold start).\nVui lòng đợi 30-60 giây rồi thử lại.")
            } catch (e: ConnectException) {
                Resource.Error("❌ Không thể kết nối đến server.\nKiểm tra kết nối mạng của bạn.")
            } catch (e: UnknownHostException) {
                Resource.Error("❌ Không tìm thấy server.\nKiểm tra kết nối mạng của bạn.")
            } catch (e: Exception) {
                Resource.Error("❌ Lỗi: ${e.message}")
            }
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String?
    ): Resource<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterRequest(
                    username = username,
                    email = email,
                    password = password,
                    displayName = displayName?.takeIf { it.isNotBlank() }
                )
                val response = apiService.register(request)
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse?.success == true && registerResponse.token != null && registerResponse.user != null) {
                        authPreferences.saveLoginSession(registerResponse.token, registerResponse.user)
                        Resource.Success(registerResponse)
                    } else {
                        val message = registerResponse?.message ?: registerResponse?.error ?: "Register failed"
                        
                        Resource.Error(message)
                    }
                } else {
                    val message = when (response.code()) {
                        400 -> "Dữ liệu đăng ký không hợp lệ"
                        409 -> "Email hoặc tên đăng nhập đã tồn tại"
                        429 -> "Quá nhiều yêu cầu. Vui lòng thử lại sau."
                        else -> "Đăng ký thất bại: ${response.code()}"
                    }
                    Resource.Error(message)
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Đăng ký thất bại")
            }
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Resource<SimpleResponse> {
        return withContext(Dispatchers.IO) {
            val token = authPreferences.getToken()
            if (token.isNullOrEmpty()) {
                return@withContext Resource.Error("Bạn chưa đăng nhập")
            }
            try {
                val response = apiService.changePassword(
                    token = "Bearer $token",
                    request = ChangePasswordRequest(currentPassword, newPassword)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Resource.Success(body)
                    } else {
                        Resource.Error(body?.error ?: body?.message ?: "Change password failed")
                    }
                } else {
                    val message = when (response.code()) {
                        400 -> "Yêu cầu không hợp lệ"
                        401 -> "Mật khẩu hiện tại không đúng"
                        else -> "Đổi mật khẩu thất bại: ${response.code()}"
                    }
                    Resource.Error(message)
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Đổi mật khẩu thất bại")
            }
        }
    }
    
    /**
     * Logout user - clear local session
     */
    fun logout() {
        authPreferences.clearSession()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return authPreferences.isLoggedIn()
    }
    
    /**
     * Get current logged in user
     */
    fun getCurrentUser(): User? {
        return authPreferences.getUser()
    }
    
    /**
     * Get auth token
     */
    fun getToken(): String? {
        return authPreferences.getToken()
    }
}
