package com.example.soul.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * Login request data model
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)
