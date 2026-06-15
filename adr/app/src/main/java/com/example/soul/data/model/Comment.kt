package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

data class Comment(
    @SerializedName("id")
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("content")
    val content: String,
    @SerializedName("target_id")
    val targetId: Int,
    @SerializedName("target_type")
    val targetType: String,
    @SerializedName("parent_id")
    val parentId: Int?,
    @SerializedName("like_count")
    val likeCount: Int,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("username")
    val username: String?,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    @SerializedName("is_liked")
    val isLiked: Boolean = false
)

data class CommentsResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: List<Comment>,
    @SerializedName("pagination")
    val pagination: CommentsPagination? = null
)

data class CommentsPagination(
    @SerializedName("total")
    val total: Int = 0
)

data class CreateCommentResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: Comment
)
