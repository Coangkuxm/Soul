package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

/**
 * Feed item from news feed
 */
data class FeedItem(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("item")
    val item: FeedItemDetail,
    
    @SerializedName("note")
    val note: String?,
    
    @SerializedName("rating")
    val rating: Int?,
    
    @SerializedName("addedAt")
    val addedAt: String?,
    
    @SerializedName("collection")
    val collection: FeedCollection,
    
    @SerializedName("user")
    val user: FeedUser,
    
    @SerializedName("isFriend")
    val isFriend: Boolean = false,
    
    @SerializedName("isLiked")
    var isLiked: Boolean = false,

    @SerializedName("likeCount")
    var likeCount: Int = 0,

    @SerializedName("commentCount")
    var commentCount: Int = 0,

    @SerializedName("activityText")
    val activityText: String?
)

data class FeedItemDetail(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("coverImageUrl")
    val coverImageUrl: String?,
    
    @SerializedName("metadata")
    val metadata: ItemMetadata?
)

data class FeedCollection(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String
)

data class FeedUser(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("displayName")
    val displayName: String?,
    
    @SerializedName("avatarUrl")
    val avatarUrl: String?
)

data class ItemMetadata(
    @SerializedName("artist")
    val artist: String?,
    
    @SerializedName("album")
    val album: String?,
    
    @SerializedName("duration_ms")
    val durationMs: Int?,
    
    @SerializedName("release_date")
    val releaseDate: String?,
    
    @SerializedName("genres")
    val genres: List<String>?,
    
    @SerializedName("preview_url")
    val previewUrl: String?,
    
    @SerializedName("spotify_url")
    val spotifyUrl: String?
)

/**
 * Response wrapper for feed
 */
data class FeedResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: List<FeedItem>,
    
    @SerializedName("pagination")
    val pagination: FeedPagination?
)

data class FeedPagination(
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("limit")
    val limit: Int,
    
    @SerializedName("hasMore")
    val hasMore: Boolean
)
