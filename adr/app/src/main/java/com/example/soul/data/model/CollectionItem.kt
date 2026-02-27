package com.example.soul.data.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class CollectionItemsResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: List<CollectionContentItem>,
    @SerializedName("pagination")
    val pagination: CollectionItemsPagination?
)

data class CollectionItemsPagination(
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("total")
    val total: Int,
    @SerializedName("total_pages")
    val totalPages: Int
)

data class CollectionContentItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("type")
    val type: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("cover_image_url")
    val coverImageUrl: String?,
    @SerializedName("metadata")
    val metadata: JsonObject?,
    @SerializedName("spotify_id")
    val spotifyId: String?,
    @SerializedName("spotify_type")
    val spotifyType: String?,
    @SerializedName("added_at")
    val addedAt: String?
)
