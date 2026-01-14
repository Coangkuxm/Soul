package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

/**
 * Profile/User detail response
 */
data class ProfileResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: UserProfile?
)

data class UserProfile(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("displayName")
    val displayName: String?,
    
    @SerializedName("avatarUrl")
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
