package com.example.soul.data.model.auth

import com.google.gson.annotations.SerializedName

data class ForgotPasswordResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null,
    // Chỉ có khi backend chạy OTP_TEST_MODE (chưa cấu hình SMTP)
    @SerializedName("devCode")
    val devCode: String? = null
)
