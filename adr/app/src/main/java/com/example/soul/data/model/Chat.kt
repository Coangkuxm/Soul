package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

data class ChatConversationsResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: List<ChatConversation> = emptyList(),
    @SerializedName("pagination")
    val pagination: ChatPagination? = null
)

data class ChatMessagesResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: List<ChatMessage> = emptyList(),
    @SerializedName("pagination")
    val pagination: ChatPagination? = null
)

data class ChatCreateConversationResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: ChatConversationInfo
)

data class ChatReadConversationResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: ChatReadConversationData
)

data class ChatReadConversationData(
    @SerializedName("conversation_id")
    val conversationId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("last_read_at")
    val lastReadAt: String?
)

data class ChatConversationInfo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("conversation_type")
    val conversationType: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("last_message_at")
    val lastMessageAt: String? = null
)

data class ChatConversation(
    @SerializedName("id")
    val id: Int,
    @SerializedName("conversation_type")
    val conversationType: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("last_message_at")
    val lastMessageAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("last_message_id")
    val lastMessageId: Int? = null,
    @SerializedName("last_message_content")
    val lastMessageContent: String? = null,
    @SerializedName("last_message_sender_id")
    val lastMessageSenderId: Int? = null,
    @SerializedName("last_message_created_at")
    val lastMessageCreatedAt: String? = null,
    @SerializedName("other_user_id")
    val otherUserId: Int? = null,
    @SerializedName("other_username")
    val otherUsername: String? = null,
    @SerializedName("other_display_name")
    val otherDisplayName: String? = null,
    @SerializedName("other_avatar_url")
    val otherAvatarUrl: String? = null,
    @SerializedName("unread_count")
    val unreadCount: Int = 0
)

data class ChatMessage(
    @SerializedName("id")
    val id: Int,
    @SerializedName("conversation_id")
    val conversationId: Int,
    @SerializedName("sender_id")
    val senderId: Int,
    @SerializedName("content")
    val content: String,
    @SerializedName("message_type")
    val messageType: String? = null,
    @SerializedName("metadata")
    val metadata: Map<String, @JvmSuppressWildcards Any?>? = null,
    @SerializedName("reply_to_message_id")
    val replyToMessageId: Int? = null,
    @SerializedName("is_deleted")
    val isDeleted: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("sender_username")
    val senderUsername: String? = null,
    @SerializedName("sender_display_name")
    val senderDisplayName: String? = null,
    @SerializedName("sender_avatar_url")
    val senderAvatarUrl: String? = null
)

data class ChatPagination(
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("page")
    val page: Int = 1,
    @SerializedName("limit")
    val limit: Int = 20,
    @SerializedName("totalPages")
    val totalPages: Int = 1
)

data class ChatConversationUpdatedEvent(
    @SerializedName("conversationId")
    val conversationId: Int,
    @SerializedName("lastMessage")
    val lastMessage: ChatMessage? = null
)

data class ChatConversationReadEvent(
    @SerializedName("conversationId")
    val conversationId: Int,
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("readAt")
    val readAt: String? = null
)
