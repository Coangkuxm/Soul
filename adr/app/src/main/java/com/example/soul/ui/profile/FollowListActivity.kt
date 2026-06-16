package com.example.soul.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.ExploreUser
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityFollowListBinding
import com.example.soul.ui.explore.ExploreRow
import com.example.soul.ui.explore.ExploreUserAdapter
import com.google.gson.JsonElement
import kotlinx.coroutines.launch

/**
 * Hiển thị danh sách "Người theo dõi" (followers) hoặc "Đang theo dõi" (following)
 * của một người dùng. Tái sử dụng [ExploreUserAdapter] để có sẵn nút theo dõi + mở hồ sơ.
 */
class FollowListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_MODE = "extra_mode"
        const val MODE_FOLLOWERS = "followers"
        const val MODE_FOLLOWING = "following"
    }

    private lateinit var binding: ActivityFollowListBinding
    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: ExploreUserAdapter

    private var userId: Int = -1
    private var mode: String = MODE_FOLLOWERS
    private var usersCache: MutableList<ExploreUser> = mutableListOf()

    private val followCache by lazy {
        getSharedPreferences("soul_follow_cache", Context.MODE_PRIVATE)
    }
    private val followedIds: MutableSet<Int> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        userId = intent.getIntExtra(EXTRA_USER_ID, -1)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_FOLLOWERS
        if (userId <= 0) {
            finish()
            return
        }

        binding.tvTitle.text = if (mode == MODE_FOLLOWING) "Đang theo dõi" else "Người theo dõi"
        binding.tvEmpty.text = if (mode == MODE_FOLLOWING) "Chưa theo dõi ai" else "Chưa có người theo dõi"

        loadFollowCache()
        setupRecycler()
        binding.btnBack.setOnClickListener { finish() }
        binding.swipeRefresh.setOnRefreshListener { loadList() }
        loadList()
    }

    private fun setupRecycler() {
        adapter = ExploreUserAdapter(
            onFollowClick = { user -> toggleFollow(user) },
            onUserClick = { user -> openUserProfile(user.id) },
            currentUserId = authPreferences.getUser()?.id ?: -1
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
    }

    private fun loadList() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"
                val response = if (mode == MODE_FOLLOWING) {
                    RetrofitClient.apiService.getFollowing(token, userId)
                } else {
                    RetrofitClient.apiService.getFollowers(token, userId)
                }

                if (!response.isSuccessful || response.body() == null) {
                    showError("Không thể tải danh sách")
                    return@launch
                }

                usersCache = parseUsers(response.body()!!.get("data")).toMutableList()
                adapter.submitList(usersCache.map { ExploreRow.UserItem(it) })
                binding.tvEmpty.visibility = if (usersCache.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                showError(e.message ?: "Lỗi mạng")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun parseUsers(dataNode: JsonElement?): List<ExploreUser> {
        if (dataNode == null || dataNode.isJsonNull || !dataNode.isJsonArray) return emptyList()
        return dataNode.asJsonArray.mapNotNull { node ->
            if (!node.isJsonObject) return@mapNotNull null
            val obj = node.asJsonObject
            val id = obj.get("id")?.takeIf { !it.isJsonNull }?.asInt ?: return@mapNotNull null
            val username = obj.get("username")?.takeIf { !it.isJsonNull }?.asString ?: return@mapNotNull null
            val displayName = obj.get("displayName")?.takeIf { !it.isJsonNull }?.asString
            val avatarUrl = obj.get("avatarUrl")?.takeIf { !it.isJsonNull }?.asString
            ExploreUser(
                id = id,
                username = username,
                displayName = displayName,
                avatarUrl = avatarUrl,
                isFollowing = followedIds.contains(id)
            )
        }
    }

    private fun toggleFollow(user: ExploreUser) {
        lifecycleScope.launch {
            val token = "Bearer ${authPreferences.getToken().orEmpty()}"
            val before = user.isFollowing
            user.isFollowing = !before
            adapter.submitList(usersCache.map { ExploreRow.UserItem(it) })
            try {
                if (before) {
                    RetrofitClient.apiService.unfollowUser(token, user.id)
                    followedIds.remove(user.id)
                } else {
                    RetrofitClient.apiService.followUser(token, user.id)
                    followedIds.add(user.id)
                }
                saveFollowCache()
            } catch (e: Exception) {
                user.isFollowing = before
                adapter.submitList(usersCache.map { ExploreRow.UserItem(it) })
                showError(e.message ?: "Thao tác theo dõi thất bại")
            }
        }
    }

    private fun openUserProfile(targetId: Int) {
        startActivity(Intent(this, UserProfileActivity::class.java).apply {
            putExtra(UserProfileActivity.EXTRA_USER_ID, targetId)
        })
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun loadFollowCache() {
        val stored = followCache.getStringSet("ids", emptySet()) ?: emptySet()
        followedIds.clear()
        stored.forEach { s -> s.toIntOrNull()?.let { followedIds.add(it) } }
    }

    private fun saveFollowCache() {
        followCache.edit().putStringSet("ids", followedIds.map { it.toString() }.toSet()).apply()
    }
}
