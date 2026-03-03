package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

/**
 * Profile/User detail response
 */
data class ProfileResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("user")
    val user: UserProfile?
)

data class UserProfile(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName(value = "displayName", alternate = ["display_name"])
    val displayName: String?,
    
    @SerializedName(value = "avatarUrl", alternate = ["avatar_url"])
    val avatarUrl: String?,
    
    @SerializedName("bio")
    val bio: String?,
    
    @SerializedName("followerCount")
    val followerCount: Int = 0,
    
    @SerializedName("followingCount")
    val followingCount: Int = 0,
    
    @SerializedName("collectionCount")
    val collectionCount: Int = 0,
    
    @SerializedName("createdAt")
    val createdAt: String? = null
)

/**
 * Simple Profile data class for UI
 */
data class Profile(
    val id: Int,
    val username: String,
    val email: String?,
    val avatarUrl: String?,
    val profileUrl: String,
    val bio: String?
)
