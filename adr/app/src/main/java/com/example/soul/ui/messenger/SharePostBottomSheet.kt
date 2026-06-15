package com.example.soul.ui.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.ExploreUser
import com.example.soul.data.model.ShareRecipient
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.DialogSharePostBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SharePostBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SharePostBottomSheet"

        private const val ARG_COLLECTION_ITEM_ID = "arg_collection_item_id"
        private const val ARG_ITEM_ID = "arg_item_id"
        private const val ARG_ITEM_TITLE = "arg_item_title"
        private const val ARG_ITEM_SUBTITLE = "arg_item_subtitle"
        private const val ARG_POST_NOTE = "arg_post_note"
        private const val ARG_COVER_URL = "arg_cover_url"
        private const val ARG_POST_USER_NAME = "arg_post_user_name"
        private const val ARG_POST_USER_AVATAR = "arg_post_user_avatar"
        private const val ARG_POST_ADDED_AT = "arg_post_added_at"

        fun newInstance(
            collectionItemId: Int,
            itemId: Int,
            itemTitle: String?,
            itemSubtitle: String?,
            postNote: String?,
            coverUrl: String?,
            postUserName: String?,
            postUserAvatar: String?,
            postAddedAt: String?
        ): SharePostBottomSheet {
            return SharePostBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLLECTION_ITEM_ID, collectionItemId)
                    putInt(ARG_ITEM_ID, itemId)
                    putString(ARG_ITEM_TITLE, itemTitle)
                    putString(ARG_ITEM_SUBTITLE, itemSubtitle)
                    putString(ARG_POST_NOTE, postNote)
                    putString(ARG_COVER_URL, coverUrl)
                    putString(ARG_POST_USER_NAME, postUserName)
                    putString(ARG_POST_USER_AVATAR, postUserAvatar)
                    putString(ARG_POST_ADDED_AT, postAddedAt)
                }
            }
        }
    }

    private var _binding: DialogSharePostBinding? = null
    private val binding get() = _binding!!

    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: ShareRecipientAdapter

    private var token: String = ""
    private var currentUserId: Int = -1
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSharePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authPreferences = AuthPreferences(requireContext())
        token = authPreferences.getToken().orEmpty()
        currentUserId = authPreferences.getUser()?.id ?: -1

        setupUi()
        loadRecentConversations()
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = (dialog as? BottomSheetDialog)
            ?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return
        bottomSheet.layoutParams.height = (resources.displayMetrics.heightPixels * 0.48f).toInt()
        BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupUi() {
        adapter = ShareRecipientAdapter { recipient -> sendSharedPost(recipient) }
        binding.rvRecipients.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvRecipients.adapter = adapter

        binding.tvPostTitle.text = arguments?.getString(ARG_ITEM_TITLE).orEmpty()
        binding.tvPostSubtitle.text = arguments?.getString(ARG_ITEM_SUBTITLE).orEmpty()

        val postNote = arguments?.getString(ARG_POST_NOTE).orEmpty()
        if (postNote.isBlank()) {
            binding.tvPostNote.visibility = View.GONE
        } else {
            binding.tvPostNote.visibility = View.VISIBLE
            binding.tvPostNote.text = postNote
        }

        Glide.with(this)
            .load(arguments?.getString(ARG_COVER_URL))
            .placeholder(R.drawable.ic_default_cover)
            .error(R.drawable.ic_default_cover)
            .centerCrop()
            .into(binding.ivPostCover)

        binding.btnClose.setOnClickListener { dismiss() }
        binding.etSearch.doAfterTextChanged { text ->
            val keyword = text?.toString()?.trim().orEmpty()
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
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
        viewLifecycleOwner.lifecycleScope.launch {
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
                bindRecipients(recipients, "Chưa có cuộc trò chuyện nào")
            } catch (e: Exception) {
                showError(e.message ?: "Lỗi mạng")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun searchUsers(keyword: String) {
        viewLifecycleOwner.lifecycleScope.launch {
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
                bindRecipients(recipients, "Không tìm thấy người dùng")
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
            val id = obj.get("id")?.takeIf { !it.isJsonNull }?.asInt ?: return@mapNotNull null
            val username = obj.get("username")?.takeIf { !it.isJsonNull }?.asString ?: return@mapNotNull null
            val displayName = obj.get("displayName")?.takeIf { !it.isJsonNull }?.asString
            val avatarUrl = obj.get("avatarUrl")?.takeIf { !it.isJsonNull }?.asString
            ExploreUser(id = id, username = username, displayName = displayName, avatarUrl = avatarUrl)
        }
    }

    private fun bindRecipients(recipients: List<ShareRecipient>, emptyMessage: String) {
        adapter.submitList(recipients)
        binding.tvEmpty.text = emptyMessage
        binding.tvEmpty.visibility = if (recipients.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun sendSharedPost(recipient: ShareRecipient) {
        viewLifecycleOwner.lifecycleScope.launch {
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

                val response = RetrofitClient.apiService.sendChatMessage(
                    token = "Bearer $token",
                    conversationId = conversationId,
                    body = mapOf(
                        "content" to "Đã chia sẻ một bài viết",
                        "message_type" to "shared_post",
                        "metadata" to buildSharedPostMetadata()
                    )
                )
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Đã chia sẻ qua tin nhắn", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    showError(response.errorBody()?.string() ?: "Không thể gửi bài viết")
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
            "targetId" to arguments?.getInt(ARG_COLLECTION_ITEM_ID, -1),
            "itemId" to arguments?.getInt(ARG_ITEM_ID, -1),
            "itemTitle" to arguments?.getString(ARG_ITEM_TITLE),
            "itemSubtitle" to arguments?.getString(ARG_ITEM_SUBTITLE),
            "postNote" to arguments?.getString(ARG_POST_NOTE),
            "coverUrl" to arguments?.getString(ARG_COVER_URL),
            "postUserName" to arguments?.getString(ARG_POST_USER_NAME),
            "postUserAvatar" to arguments?.getString(ARG_POST_USER_AVATAR),
            "addedAt" to arguments?.getString(ARG_POST_ADDED_AT)
        )
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        _binding = null
        super.onDestroyView()
    }
}


