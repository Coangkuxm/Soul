package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

/**
 * User data model
 */
data class User(
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

    @SerializedName("role")
    val role: String? = "user",

    @SerializedName("accountStatus")
    val accountStatus: String? = "active",
    
    @SerializedName("followerCount")
    val followerCount: Int = 0,
    
    @SerializedName("followingCount")
    val followingCount: Int = 0,
    
    @SerializedName("collectionCount")
    val collectionCount: Int = 0
)
