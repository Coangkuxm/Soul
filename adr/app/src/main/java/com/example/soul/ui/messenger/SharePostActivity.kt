package com.example.soul.ui.messenger

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.ExploreUser
import com.example.soul.data.model.ShareRecipient
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivitySharePostBinding
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SharePostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COLLECTION_ITEM_ID = "extra_collection_item_id"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ITEM_TITLE = "extra_item_title"
        const val EXTRA_ITEM_SUBTITLE = "extra_item_subtitle"
        const val EXTRA_POST_NOTE = "extra_post_note"
        const val EXTRA_COVER_URL = "extra_cover_url"
        const val EXTRA_POST_USER_NAME = "extra_post_user_name"
        const val EXTRA_POST_USER_AVATAR = "extra_post_user_avatar"
        const val EXTRA_POST_ADDED_AT = "extra_post_added_at"
    }

    private lateinit var binding: ActivitySharePostBinding
    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: ShareRecipientAdapter

    private var token: String = ""
    private var currentUserId: Int = -1
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        token = authPreferences.getToken().orEmpty()
        currentUserId = authPreferences.getUser()?.id ?: -1

        setupUi()
        loadRecentConversations()
    }

    private fun setupUi() {
        adapter = ShareRecipientAdapter { recipient ->
            sendSharedPost(recipient)
        }
        binding.rvRecipients.layoutManager = LinearLayoutManager(this)
        binding.rvRecipients.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.etSearch.doAfterTextChanged { text ->
            val keyword = text?.toString()?.trim().orEmpty()
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(250)
                if (keyword.isBlank()) {
                    loadRecentConversations()
                } else {
                    searchUsers(keyword)
                }
            }
        }
    }

    private fun loadRecentConversations() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val response = RetrofitClient.apiService.getChatConversations(
                    token = "Bearer $token",
                    page = 1,
                    limit = 30
                )
                if (!response.isSuccessful || response.body() == null) {
                    showError("Không thể tải cuộc trò chuyện")
                    return@launch
                }

                val recipients = response.body()!!.data.mapNotNull { conversation ->
                    val title = conversation.otherDisplayName?.takeIf { it.isNotBlank() }
                        ?: conversation.otherUsername
                        ?: return@mapNotNull null
                    ShareRecipient(
                        mode = ShareRecipient.Mode.CONVERSATION,
                        conversationId = conversation.id,
                        userId = conversation.otherUserId,
                        title = title,
                        subtitle = conversation.lastMessageContent ?: "Tin nhắn gần đây",
                        avatarUrl = conversation.otherAvatarUrl
                    )
                }
                bindRecipients(recipients, emptyMessage = "Chưa có cuộc trò chuyện nào")
            } catch (e: Exception) {
                showError(e.message ?: "Lỗi mạng")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun searchUsers(keyword: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val response = RetrofitClient.apiService.searchUsers(
                    token = "Bearer $token",
                    keyword = keyword,
                    page = 1,
                    limit = 30
                )
                if (!response.isSuccessful || response.body() == null) {
                    showError("Không thể tìm người dùng")
                    return@launch
                }

                val recipients = parseUsers(response.body()!!.get("data"))
                    .filter { it.id != currentUserId }
                    .map { user ->
                        ShareRecipient(
                            mode = ShareRecipient.Mode.USER,
                            userId = user.id,
                            title = user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                            subtitle = "@${user.username}",
                            avatarUrl = user.avatarUrl
                        )
                    }
                bindRecipients(recipients, emptyMessage = "Không tìm thấy người dùng")
            } catch (e: Exception) {
                showError(e.message ?: "Lỗi mạng")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun parseUsers(dataNode: JsonElement?): List<ExploreUser> {
        if (dataNode == null || dataNode.isJsonNull) return emptyList()
        val usersArray: JsonArray = when {
            dataNode.isJsonArray -> dataNode.asJsonArray
            dataNode.isJsonObject && dataNode.asJsonObject.has("users") ->
                dataNode.asJsonObject.getAsJsonArray("users")
            else -> return emptyList()
        }
        return usersArray.mapNotNull { node ->
            if (!node.isJsonObject) return@mapNotNull null
            val obj = node.asJsonObject
            val id = obj.get("id")?.asInt ?: return@mapNotNull null
            val username = obj.get("username")?.asString ?: return@mapNotNull null
            val displayName = obj.get("displayName")?.asString
            val avatarUrl = obj.get("avatarUrl")?.asString
            ExploreUser(id = id, username = username, displayName = displayName, avatarUrl = avatarUrl)
        }
    }

    private fun bindRecipients(recipients: List<ShareRecipient>, emptyMessage: String) {
        adapter.submitList(recipients)
        binding.tvEmpty.text = emptyMessage
        binding.tvEmpty.visibility = if (recipients.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun sendSharedPost(recipient: ShareRecipient) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val conversationId = when (recipient.mode) {
                    ShareRecipient.Mode.CONVERSATION -> recipient.conversationId
                    ShareRecipient.Mode.USER -> openOrCreateConversation(recipient.userId)
                }

                if (conversationId == null || conversationId <= 0) {
                    showError("Không thể mở cuộc trò chuyện")
                    return@launch
                }

                val body = mapOf(
                    "content" to "Đã chia sẻ một bài viết",
                    "message_type" to "shared_post",
                    "metadata" to buildSharedPostMetadata()
                )

                val response = RetrofitClient.apiService.sendChatMessage(
                    token = "Bearer $token",
                    conversationId = conversationId,
                    body = body
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@SharePostActivity, "Đã chia sẻ qua tin nhắn", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    showError("Không thể gửi bài viết")
                }
            } catch (e: Exception) {
                showError(e.message ?: "Lỗi mạng")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun openOrCreateConversation(targetUserId: Int?): Int? {
        if (targetUserId == null || targetUserId <= 0) return null
        val response = RetrofitClient.apiService.createOrGetDirectConversation(
            token = "Bearer $token",
            body = mapOf("targetUserId" to targetUserId)
        )
        return if (response.isSuccessful && response.body() != null) {
            response.body()!!.data.id
        } else {
            null
        }
    }

    private fun buildSharedPostMetadata(): Map<String, Any?> {
        return mapOf(
            "targetType" to "collection_item",
            "targetId" to intent.getIntExtra(EXTRA_COLLECTION_ITEM_ID, -1),
            "itemId" to intent.getIntExtra(EXTRA_ITEM_ID, -1),
            "itemTitle" to intent.getStringExtra(EXTRA_ITEM_TITLE),
            "itemSubtitle" to intent.getStringExtra(EXTRA_ITEM_SUBTITLE),
            "postNote" to intent.getStringExtra(EXTRA_POST_NOTE),
            "coverUrl" to intent.getStringExtra(EXTRA_COVER_URL),
            "postUserName" to intent.getStringExtra(EXTRA_POST_USER_NAME),
            "postUserAvatar" to intent.getStringExtra(EXTRA_POST_USER_AVATAR),
            "addedAt" to intent.getStringExtra(EXTRA_POST_ADDED_AT)
        )
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
