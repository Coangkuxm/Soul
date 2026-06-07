package com.example.soul.data.model.auth

import com.google.gson.annotations.SerializedName

data class SimpleResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null
)
