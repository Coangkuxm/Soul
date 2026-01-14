package com.example.soul.data.model.auth

import com.example.soul.data.model.User
import com.google.gson.annotations.SerializedName

/**
 * Login response data model
 */
data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("token")
    val token: String?,
    
    @SerializedName("user")
    val user: User?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("error")
    val error: String?
)
