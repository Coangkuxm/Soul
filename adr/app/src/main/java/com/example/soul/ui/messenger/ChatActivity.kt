package com.example.soul.ui.messenger

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.ChatMessage
import com.example.soul.data.remote.ChatSocketManager
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: ChatMessageAdapter

    private var token: String = ""
    private var currentUserId: Int = -1
    private var conversationId: Int = -1
    private val messages = mutableListOf<ChatMessage>()

    private val messageListener: (ChatMessage) -> Unit = { incoming ->
        if (incoming.conversationId == conversationId) {
            runOnUiThread {
                appendMessageIfNeeded(incoming)
                markConversationRead()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        token = authPreferences.getToken().orEmpty()
        currentUserId = authPreferences.getUser()?.id ?: -1
        conversationId = intent.getIntExtra(EXTRA_CONVERSATION_ID, -1)

        if (conversationId <= 0) {
            showError("Không tìm thấy cuộc trò chuyện")
            finish()
            return
        }

        setupUi()
        setupActions()

        if (token.isNotBlank()) {
            ChatSocketManager.connect(token)
            ChatSocketManager.addMessageListener(messageListener)
            ChatSocketManager.joinConversation(conversationId)
        }

        loadMessages()
        markConversationRead()
    }

    override fun onDestroy() {
        ChatSocketManager.removeMessageListener(messageListener)
        ChatSocketManager.leaveConversation(conversationId)
        super.onDestroy()
    }

    private fun setupUi() {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Tin nhắn" }
        val avatarUrl = intent.getStringExtra(EXTRA_AVATAR_URL)
        binding.tvTitle.text = title

        Glide.with(this)
            .load(avatarUrl)
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .circleCrop()
            .into(binding.ivAvatar)

        adapter = ChatMessageAdapter(currentUserId)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSend.setOnClickListener { onSendClicked() }
        binding.etMessage.setOnEditorActionListener { _, actionId, event ->
            val handled = actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (handled) onSendClicked()
            handled
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val response = RetrofitClient.apiService.getChatMessages(
                    token = "Bearer $token",
                    conversationId = conversationId,
                    page = 1,
                    limit = 100
                )
                if (!response.isSuccessful || response.body() == null) {
                    showError("Không thể tải tin nhắn")
                    return@launch
                }

                messages.clear()
                val ordered = response.body()!!.data.reversed()
                messages.addAll(ordered)
                adapter.submitList(messages.toList())
                scrollToBottom()
            } catch (e: Exception) {
                showError(e.message ?: "Lỗi mạng")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun onSendClicked() {
        val content = binding.etMessage.text?.toString()?.trim().orEmpty()
        if (content.isBlank()) return

        binding.btnSend.isEnabled = false
        hideKeyboard()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.sendChatMessage(
                    token = "Bearer $token",
                    conversationId = conversationId,
                    body = mapOf("content" to content)
                )
                if (response.isSuccessful) {
                    binding.etMessage.setText("")
                    // Refresh from server to avoid waiting for socket ack/buffer.
                    loadMessages()
                    markConversationRead()
                } else {
                    showError("Không thể gửi tin nhắn")
                }
            } catch (e: Exception) {
                showError(e.message ?: "Không thể gửi tin nhắn")
            } finally {
                binding.btnSend.isEnabled = true
            }
        }
    }

    private fun appendMessageIfNeeded(message: ChatMessage) {
        if (messages.any { it.id == message.id }) return
        messages.add(message)
        adapter.submitList(messages.toList())
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (messages.isEmpty()) return
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun markConversationRead() {
        if (token.isBlank()) return
        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.markConversationRead(
                    token = "Bearer $token",
                    conversationId = conversationId
                )
            } catch (_: Exception) {
            }
        }
        ChatSocketManager.markConversationRead(conversationId)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etMessage.windowToken, 0)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_TARGET_USER_ID = "extra_target_user_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AVATAR_URL = "extra_avatar_url"
    }
}
