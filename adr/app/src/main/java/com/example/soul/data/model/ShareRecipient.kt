package com.example.soul.data.model

data class ShareRecipient(
    val mode: Mode,
    val conversationId: Int? = null,
    val userId: Int? = null,
    val title: String,
    val subtitle: String?,
    val avatarUrl: String?
) {
    enum class Mode {
        CONVERSATION,
        USER
    }
}
