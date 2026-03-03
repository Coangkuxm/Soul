package com.example.soul.data.model

data class ExploreUser(
    val id: Int,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    var isFollowing: Boolean = false
)
