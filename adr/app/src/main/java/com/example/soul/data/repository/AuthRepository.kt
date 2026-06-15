package com.example.soul.data.repository

import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.User
import com.example.soul.data.model.auth.ChangePasswordRequest
import com.example.soul.data.model.auth.ForgotPasswordRequest
import com.example.soul.data.model.auth.ForgotPasswordResponse
import com.example.soul.data.model.auth.LoginRequest
import com.example.soul.data.model.auth.LoginResponse
import com.example.soul.data.model.auth.RegisterRequest
import com.example.soul.data.model.auth.ResetPasswordRequest
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
    
    /** Quên mật khẩu: yêu cầu gửi mã OTP tới email */
    suspend fun forgotPassword(email: String): Resource<ForgotPasswordResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful && response.body()?.success == true) {
                    Resource.Success(response.body()!!)
                } else {
                    val body = response.body()
                    Resource.Error(body?.error ?: body?.message ?: "Không thể gửi mã. Vui lòng thử lại.")
                }
            } catch (e: SocketTimeoutException) {
                Resource.Error("⏳ Server đang khởi động. Vui lòng đợi rồi thử lại.")
            } catch (e: ConnectException) {
                Resource.Error("❌ Không thể kết nối đến server.")
            } catch (e: UnknownHostException) {
                Resource.Error("❌ Không tìm thấy server. Kiểm tra kết nối mạng.")
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    /** Đặt lại mật khẩu bằng mã OTP */
    suspend fun resetPassword(email: String, code: String, newPassword: String): Resource<SimpleResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.resetPassword(ResetPasswordRequest(email, code, newPassword))
                if (response.isSuccessful && response.body()?.success == true) {
                    Resource.Success(response.body()!!)
                } else {
                    val message = when (response.code()) {
                        400 -> "Dữ liệu không hợp lệ"
                        401 -> "Mã đặt lại không đúng hoặc đã hết hạn"
                        else -> response.body()?.error ?: "Đặt lại mật khẩu thất bại: ${response.code()}"
                    }
                    Resource.Error(message)
                }
            } catch (e: SocketTimeoutException) {
                Resource.Error("⏳ Server đang khởi động. Vui lòng đợi rồi thử lại.")
            } catch (e: ConnectException) {
                Resource.Error("❌ Không thể kết nối đến server.")
            } catch (e: UnknownHostException) {
                Resource.Error("❌ Không tìm thấy server. Kiểm tra kết nối mạng.")
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Đã xảy ra lỗi")
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
