package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

data class NotificationsResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: List<NotificationItem> = emptyList(),
    @SerializedName("pagination")
    val pagination: NotificationsPagination? = null
)

data class NotificationsPagination(
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("page")
    val page: Int = 1,
    @SerializedName("limit")
    val limit: Int = 20,
    @SerializedName("totalPages")
    val totalPages: Int = 1
)

data class NotificationItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("notification_type")
    val notificationType: String,
    @SerializedName("sender_id")
    val senderId: Int,
    @SerializedName("target_id")
    val targetId: Int,
    @SerializedName("target_type")
    val targetType: String,
    @SerializedName("is_read")
    var isRead: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("sender_username")
    val senderUsername: String?,
    @SerializedName("sender_avatar")
    val senderAvatar: String?
)

