package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

/**
 * Collection data model
 */
data class Collection(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("cover_image_url")
    val coverImageUrl: String?,
    
    @SerializedName("is_private")
    val isPrivate: Boolean = false,
    
    @SerializedName("owner_id")
    val ownerId: Int,
    
    @SerializedName("view_count")
    val viewCount: Int = 0,
    
    @SerializedName("like_count")
    val likeCount: Int = 0,
    
    @SerializedName("comment_count")
    val commentCount: Int = 0,
    
    @SerializedName("username")
    val username: String? = null,
    
    @SerializedName("owner_avatar")
    val ownerAvatar: String? = null,
    
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

/**
 * Response wrapper for collections list
 */
data class CollectionsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: List<Collection>,
    
    @SerializedName("pagination")
    val pagination: Pagination?
)

/**
 * Pagination info
 */
data class Pagination(
    @SerializedName("currentPage")
    val currentPage: Int,
    
    @SerializedName("itemsPerPage")
    val itemsPerPage: Int,
    
    @SerializedName("totalItems")
    val totalItems: Int,
    
    @SerializedName("totalPages")
    val totalPages: Int,
    
    @SerializedName("hasNextPage")
    val hasNextPage: Boolean,
    
    @SerializedName("hasPreviousPage")
    val hasPreviousPage: Boolean
)
