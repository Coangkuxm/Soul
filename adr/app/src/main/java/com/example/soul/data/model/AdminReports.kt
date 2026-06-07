package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

data class AdminReportedPostsResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: List<AdminReportedPost>
)

data class AdminReportedUsersResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: List<AdminReportedUser>
)

data class AdminReportedPost(
    @SerializedName("collection_item_id")
    val collectionItemId: Int,
    @SerializedName("report_count")
    val reportCount: Int,
    @SerializedName("latest_report_at")
    val latestReportAt: String?,
    @SerializedName("reason_codes")
    val reasonCodes: List<String>?,
    @SerializedName("moderation_status")
    val moderationStatus: String?,
    @SerializedName("locked_reason")
    val lockedReason: String?,
    @SerializedName("collection_id")
    val collectionId: Int?,
    @SerializedName("item_id")
    val itemId: Int?,
    @SerializedName("item_title")
    val itemTitle: String?,
    @SerializedName("collection_name")
    val collectionName: String?,
    @SerializedName("owner_id")
    val ownerId: Int?,
    @SerializedName("owner_username")
    val ownerUsername: String?
)

data class AdminReportedUser(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("report_count")
    val reportCount: Int,
    @SerializedName("latest_report_at")
    val latestReportAt: String?,
    @SerializedName("reason_codes")
    val reasonCodes: List<String>?,
    @SerializedName("username")
    val username: String,
    @SerializedName("display_name")
    val displayName: String?,
    @SerializedName("role")
    val role: String?,
    @SerializedName("account_status")
    val accountStatus: String?,
    @SerializedName("locked_reason")
    val lockedReason: String?
)

sealed class AdminReportRow {
    data class Post(val value: AdminReportedPost) : AdminReportRow()
    data class User(val value: AdminReportedUser) : AdminReportRow()
}
