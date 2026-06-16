package com.example.soul.ui.profile

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityUserProfileBinding
import com.example.soul.ui.collection.CollectionItemsActivity
import com.example.soul.ui.messenger.ChatActivity
import com.example.soul.utils.AvatarLoader
import com.google.android.material.tabs.TabLayout
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
    private val currentUserId: Int by lazy { authPreferences.getUser()?.id ?: -1 }
    private var isFollowing: Boolean = false
    private var followerCount: Int = 0

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
        binding.btnReport.setOnClickListener { showReportAccountDialog() }
        binding.swipeRefresh.setOnRefreshListener { loadData() }
        binding.layoutFollowers.setOnClickListener { openFollowList(FollowListActivity.MODE_FOLLOWERS) }
        binding.layoutFollowing.setOnClickListener { openFollowList(FollowListActivity.MODE_FOLLOWING) }
        binding.btnFollow.setOnClickListener { toggleFollow() }
        setupTabs()
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Bài đăng"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Bộ sưu tập"))

        if (supportFragmentManager.findFragmentById(R.id.postsContainer) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.postsContainer, UserPostsFragment.newInstance(userId))
                .commit()
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = showTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        showTab(0) // mặc định: tab Bài đăng
    }

    private fun showTab(position: Int) {
        val showPosts = position == 0
        binding.postsContainer.visibility = if (showPosts) View.VISIBLE else View.GONE
        binding.swipeRefresh.visibility = if (showPosts) View.GONE else View.VISIBLE
        if (showPosts) {
            binding.progressBar.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    private fun isCollectionsTab(): Boolean = binding.tabLayout.selectedTabPosition == 1

    private fun openFollowList(mode: String) {
        startActivity(android.content.Intent(this, FollowListActivity::class.java).apply {
            putExtra(FollowListActivity.EXTRA_USER_ID, userId)
            putExtra(FollowListActivity.EXTRA_MODE, mode)
        })
    }

    private fun toggleFollow() {
        val token = "Bearer ${authPreferences.getToken().orEmpty()}"
        val target = !isFollowing
        // Cập nhật giao diện ngay (optimistic) trong lúc gọi API.
        setFollowingState(target)
        lifecycleScope.launch {
            try {
                if (target) {
                    RetrofitClient.apiService.followUser(token, userId)
                } else {
                    RetrofitClient.apiService.unfollowUser(token, userId)
                }
                updateFollowCache(userId, target)
            } catch (e: Exception) {
                setFollowingState(!target) // hoàn lại khi lỗi
                Toast.makeText(
                    this@UserProfileActivity,
                    e.message ?: "Thao tác theo dõi thất bại",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /** Đổi trạng thái follow + cập nhật số người theo dõi và giao diện nút. */
    private fun setFollowingState(following: Boolean) {
        if (isFollowing != following) {
            isFollowing = following
            followerCount = (followerCount + if (following) 1 else -1).coerceAtLeast(0)
            binding.tvFollowerCount.text = followerCount.toString()
        }
        applyFollowVisual(following)
    }

    private fun applyFollowVisual(following: Boolean) {
        binding.btnFollow.text = if (following) "Đang theo dõi" else "+ Theo dõi"
        val bgColor = if (following) R.color.primary_light else R.color.primary
        val textColor = if (following) R.color.primary_dark else R.color.white
        binding.btnFollow.backgroundTintList = ColorStateList.valueOf(getColor(bgColor))
        binding.btnFollow.setTextColor(getColor(textColor))
    }

    /** Đồng bộ trạng thái follow với tab Khám phá / danh sách follow qua cache dùng chung. */
    private fun updateFollowCache(id: Int, following: Boolean) {
        val prefs = getSharedPreferences("soul_follow_cache", MODE_PRIVATE)
        val ids = (prefs.getStringSet("ids", emptySet()) ?: emptySet()).toMutableSet()
        if (following) ids.add(id.toString()) else ids.remove(id.toString())
        prefs.edit().putStringSet("ids", ids).apply()
    }

    private fun loadData() {
        lifecycleScope.launch {
            binding.progressBar.visibility = if (isCollectionsTab()) View.VISIBLE else View.GONE
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
                        followerCount = user.followerCount
                        binding.tvFollowerCount.text = followerCount.toString()
                        binding.tvFollowingCount.text = user.followingCount.toString()
                        AvatarLoader.load(binding.ivAvatar, user.avatarUrl)
                    }
                }

                // Nút theo dõi: chỉ hiện khi xem hồ sơ người khác (không tự follow mình).
                if (currentUserId > 0 && currentUserId != userId) {
                    val followRs = RetrofitClient.apiService.checkIsFollowing(token, userId)
                    if (followRs.isSuccessful) {
                        isFollowing = followRs.body()
                            ?.get("isFollowing")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
                    }
                    binding.btnFollow.visibility = View.VISIBLE
                    applyFollowVisual(isFollowing)
                } else {
                    binding.btnFollow.visibility = View.GONE
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
                    binding.tvEmpty.visibility = if (data.isEmpty() && isCollectionsTab()) View.VISIBLE else View.GONE
                } else {
                    adapter.submitList(emptyList())
                    binding.tvEmpty.visibility = if (isCollectionsTab()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                binding.tvEmpty.visibility = if (isCollectionsTab()) View.VISIBLE else View.GONE
                Toast.makeText(this@UserProfileActivity, e.message ?: "Lỗi mạng", Toast.LENGTH_SHORT).show()
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
                        "Không thể mở đoạn chat",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val conversationId = response.body()!!.data.id
                startActivity(android.content.Intent(this@UserProfileActivity, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId)
                    putExtra(ChatActivity.EXTRA_TARGET_USER_ID, userId)
                    putExtra(ChatActivity.EXTRA_TITLE, targetName ?: "Tin nhắn")
                    putExtra(ChatActivity.EXTRA_AVATAR_URL, targetAvatarUrl)
                })
            } catch (e: Exception) {
                Toast.makeText(
                    this@UserProfileActivity,
                    e.message ?: "Không thể mở đoạn chat",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showReportAccountDialog() {
        val reasonCodes = listOf(
            "spam" to "Spam",
            "impersonation" to "Mạo danh",
            "harassment" to "Quấy rối",
            "hate_speech" to "Ngôn từ thù ghét",
            "other" to "Lý do khác"
        )
        val labels = reasonCodes.map { it.second }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Báo cáo tài khoản")
            .setItems(labels) { _, which ->
                submitUserReport(reasonCodes[which].first, reasonCodes[which].second)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun submitUserReport(reasonCode: String, reasonLabel: String) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"
                val response = RetrofitClient.apiService.createReport(
                    token = token,
                    body = mapOf(
                        "targetType" to "user",
                        "targetId" to userId,
                        "reasonCode" to reasonCode,
                        "reasonDetail" to reasonLabel
                    )
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@UserProfileActivity, "Đã gửi báo cáo", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@UserProfileActivity,
                        response.errorBody()?.string() ?: "Không thể gửi báo cáo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@UserProfileActivity,
                    e.message ?: "Không thể gửi báo cáo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
