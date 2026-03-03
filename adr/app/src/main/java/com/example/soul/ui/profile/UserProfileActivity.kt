package com.example.soul.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityUserProfileBinding
import com.example.soul.ui.collection.CollectionItemsActivity
import com.example.soul.ui.messenger.ChatActivity
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
    }

    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: UserCollectionAdapter
    private var userId: Int = -1
    private var targetName: String? = null
    private var targetAvatarUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        userId = intent.getIntExtra(EXTRA_USER_ID, -1)
        if (userId <= 0) {
            finish()
            return
        }

        setupUi()
        loadData()
    }

    private fun setupUi() {
        adapter = UserCollectionAdapter { collection ->
            startActivity(android.content.Intent(this, CollectionItemsActivity::class.java).apply {
                putExtra(CollectionItemsActivity.EXTRA_COLLECTION_ID, collection.id)
                putExtra(CollectionItemsActivity.EXTRA_COLLECTION_NAME, collection.name)
            })
        }
        binding.rvCollections.layoutManager = GridLayoutManager(this, 2)
        binding.rvCollections.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnMessage.setOnClickListener { startDirectChat() }
        binding.swipeRefresh.setOnRefreshListener { loadData() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"

                val profileRs = RetrofitClient.apiService.getUserProfile(token, userId)
                if (profileRs.isSuccessful) {
                    val user = profileRs.body()?.user
                    if (user != null) {
                        targetName = user.displayName?.takeIf { it.isNotBlank() } ?: user.username
                        targetAvatarUrl = user.avatarUrl
                        binding.tvUsername.text = targetName
                        binding.tvLink.text = "shelf.im/${user.username}"
                        Glide.with(this@UserProfileActivity)
                            .load(user.avatarUrl)
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(binding.ivAvatar)
                    }
                }

                val collectionsRs = RetrofitClient.apiService.getCollections(
                    token = token,
                    userId = userId,
                    page = 1,
                    limit = 50
                )
                if (collectionsRs.isSuccessful) {
                    val data = collectionsRs.body()?.data.orEmpty()
                    adapter.submitList(data)
                    binding.tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    adapter.submitList(emptyList())
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                binding.tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@UserProfileActivity, e.message ?: "Network error", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun startDirectChat() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"
                val response = RetrofitClient.apiService.createOrGetDirectConversation(
                    token = token,
                    body = mapOf("targetUserId" to userId)
                )
                if (!response.isSuccessful || response.body() == null) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "Unable to open chat",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val conversationId = response.body()!!.data.id
                startActivity(android.content.Intent(this@UserProfileActivity, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId)
                    putExtra(ChatActivity.EXTRA_TARGET_USER_ID, userId)
                    putExtra(ChatActivity.EXTRA_TITLE, targetName ?: "Messages")
                    putExtra(ChatActivity.EXTRA_AVATAR_URL, targetAvatarUrl)
                })
            } catch (e: Exception) {
                Toast.makeText(
                    this@UserProfileActivity,
                    e.message ?: "Unable to open chat",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
