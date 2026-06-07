package com.example.soul.ui.messenger

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.ChatConversation
import com.example.soul.data.model.ChatConversationUpdatedEvent
import com.example.soul.data.remote.ChatSocketManager
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.FragmentMessengerBinding
import kotlinx.coroutines.launch

class MessengerFragment : Fragment() {

    private var _binding: FragmentMessengerBinding? = null
    private val binding get() = _binding!!

    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: ChatConversationAdapter
    private var token: String = ""
    private var currentUserId: Int = -1
    private val conversations = mutableListOf<ChatConversation>()

    private val conversationUpdatedListener: (ChatConversationUpdatedEvent) -> Unit = { event ->
        activity?.runOnUiThread {
            handleConversationUpdated(event)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessengerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authPreferences = AuthPreferences(requireContext())
        token = authPreferences.getToken().orEmpty()
        currentUserId = authPreferences.getUser()?.id ?: -1

        setupRecycler()
        binding.swipeRefresh.setOnRefreshListener { loadConversations() }

        if (token.isNotBlank()) {
            ChatSocketManager.connect(token)
            ChatSocketManager.addConversationUpdatedListener(conversationUpdatedListener)
        }

        loadConversations()
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    override fun onDestroyView() {
        ChatSocketManager.removeConversationUpdatedListener(conversationUpdatedListener)
        _binding = null
        super.onDestroyView()
    }

    private fun setupRecycler() {
        adapter = ChatConversationAdapter { conversation ->
            openConversation(conversation)
        }
        binding.rvConversations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConversations.adapter = adapter
    }

    private fun loadConversations() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            try {
                val response = RetrofitClient.apiService.getChatConversations(
                    token = "Bearer $token",
                    page = 1,
                    limit = 50
                )
                if (!response.isSuccessful || response.body() == null) {
                    showError("Không thể tải danh sách cuộc trò chuyện")
                    return@launch
                }
                conversations.clear()
                conversations.addAll(response.body()!!.data)
                adapter.submitList(conversations.toList())
                binding.tvEmpty.visibility = if (conversations.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                showError(e.message ?: "Lỗi mạng")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun handleConversationUpdated(event: ChatConversationUpdatedEvent) {
        val idx = conversations.indexOfFirst { it.id == event.conversationId }
        if (idx < 0) {
            loadConversations()
            return
        }

        val current = conversations[idx]
        val message = event.lastMessage
        val unreadCount = if (message != null && message.senderId != currentUserId) {
            current.unreadCount + 1
        } else {
            current.unreadCount
        }

        val updated = current.copy(
            lastMessageId = message?.id ?: current.lastMessageId,
            lastMessageContent = message?.content ?: current.lastMessageContent,
            lastMessageSenderId = message?.senderId ?: current.lastMessageSenderId,
            lastMessageCreatedAt = message?.createdAt ?: current.lastMessageCreatedAt,
            lastMessageAt = message?.createdAt ?: current.lastMessageAt,
            unreadCount = unreadCount
        )

        conversations.removeAt(idx)
        conversations.add(0, updated)
        adapter.submitList(conversations.toList())
    }

    private fun openConversation(conversation: ChatConversation) {
        startActivity(Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.id)
            putExtra(
                ChatActivity.EXTRA_TITLE,
                conversation.otherDisplayName?.takeIf { it.isNotBlank() }
                    ?: conversation.otherUsername
                    ?: "Tin nhắn"
            )
            putExtra(ChatActivity.EXTRA_AVATAR_URL, conversation.otherAvatarUrl)
            putExtra(ChatActivity.EXTRA_TARGET_USER_ID, conversation.otherUserId ?: -1)
        })
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
