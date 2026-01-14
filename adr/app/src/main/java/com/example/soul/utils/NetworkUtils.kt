package com.example.soul.utils

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Network utility functions
 */
object NetworkUtils {
    
    /**
     * Get user-friendly error message from exception
     */
    fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is ConnectException -> "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng."
            is SocketTimeoutException -> "Hết thời gian chờ kết nối. Vui lòng thử lại."
            is UnknownHostException -> "Không tìm thấy máy chủ. Vui lòng kiểm tra kết nối mạng."
            else -> exception.message ?: "Đã xảy ra lỗi không xác định"
        }
    }
}
