package com.example.soul.ui.explore

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Context
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.ExploreUser
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.FragmentExploreBinding
import com.example.soul.ui.profile.UserProfileActivity
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: ExploreUserAdapter
    private var currentUserId: Int = -1
    private var searchJob: Job? = null
    private var lastKeyword: String = ""
    private var usersCache: MutableList<ExploreUser> = mutableListOf()
    private val followedIds: MutableSet<Int> = mutableSetOf()
    private val followCache by lazy {
        requireContext().getSharedPreferences("soul_follow_cache", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authPreferences = AuthPreferences(requireContext())
        loadFollowCache()
        setupRecycler()
        setupSearch()
        setupActions()
        loadCurrentUserThenSearch()
    }

    private fun setupRecycler() {
        adapter = ExploreUserAdapter(
            onFollowClick = { user -> toggleFollow(user) },
            onUserClick = { user -> openUserProfile(user.id) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { searchUsers(lastKeyword) }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            val keyword = text?.toString()?.trim().orEmpty()
            binding.btnClear.visibility = if (keyword.isNotEmpty()) View.VISIBLE else View.GONE
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(350)
                searchUsers(keyword)
            }
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            val handled = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (handled) {
                searchUsers(binding.etSearch.text?.toString()?.trim().orEmpty())
            }
            handled
        }
    }

    private fun setupActions() {
        binding.btnClear.setOnClickListener {
            binding.etSearch.setText("")
            searchUsers("")
        }
    }

    private fun loadCurrentUserThenSearch() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = authPreferences.getToken().orEmpty()
                val me = RetrofitClient.apiService.getCurrentUser("Bearer $token")
                currentUserId = me.body()?.user?.id ?: -1
            } catch (_: Exception) {
                currentUserId = -1
            }
            searchUsers("")
        }
    }

    private fun searchUsers(keyword: String) {
        lastKeyword = keyword
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            try {
                val token = authPreferences.getToken().orEmpty()
                val response = RetrofitClient.apiService.searchUsers(
                    token = "Bearer $token",
                    keyword = keyword.ifBlank { null },
                    page = 1,
                    limit = 30
                )

                if (!response.isSuccessful || response.body() == null) {
                    showError("Không thể tải danh sách người dùng")
                    return@launch
                }

                val parsed = parseUsers(response.body()!!.get("data"))
                    .filter { it.id != currentUserId }
                    .toMutableList()

                usersCache = parsed
                renderRows()
                binding.tvEmpty.text = if (keyword.isBlank()) {
                    "Chua co nguoi dung nao de goi y"
                } else {
                    "Khong tim thay nguoi dung"
                }
                binding.tvEmpty.visibility = if (usersCache.isEmpty()) View.VISIBLE else View.GONE
                saveFollowCache() // keep cache updated with latest follow states
            } catch (e: Exception) {
                showError(e.message ?: "Lỗi mạng")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
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
            val apiFollow = when {
                obj.has("isFollowing") && !obj.get("isFollowing").isJsonNull -> obj.get("isFollowing").asBoolean
                obj.has("is_following") && !obj.get("is_following").isJsonNull -> obj.get("is_following").asBoolean
                else -> null
            }
            val isFollowing = apiFollow ?: followedIds.contains(id)
            ExploreUser(id = id, username = username, displayName = displayName, avatarUrl = avatarUrl, isFollowing = isFollowing)
        }
    }

    /** Chia danh sách thành 2 nhóm: "Đang theo dõi" lên trước, rồi "Những người bạn có thể biết". */
    private fun renderRows() {
        val following = usersCache.filter { it.isFollowing }
        val others = usersCache.filter { !it.isFollowing }
        val rows = mutableListOf<ExploreRow>()
        if (following.isNotEmpty()) {
            rows += ExploreRow.Header("Đang theo dõi")
            rows += following.map { ExploreRow.UserItem(it) }
        }
        if (others.isNotEmpty()) {
            rows += ExploreRow.Header("Những người bạn có thể biết")
            rows += others.map { ExploreRow.UserItem(it) }
        }
        adapter.submitList(rows)
    }

    private fun toggleFollow(user: ExploreUser) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = authPreferences.getToken().orEmpty()
            val before = user.isFollowing
            user.isFollowing = !before
            renderRows()
            try {
                if (before) {
                    RetrofitClient.apiService.unfollowUser("Bearer $token", user.id)
                    followedIds.remove(user.id)
                } else {
                    RetrofitClient.apiService.followUser("Bearer $token", user.id)
                    followedIds.add(user.id)
                }
                saveFollowCache()
            } catch (e: Exception) {
                user.isFollowing = before
                renderRows()
                showError(e.message ?: "Thao tác theo dõi thất bại")
            }
        }
    }

    private fun openUserProfile(userId: Int) {
        startActivity(Intent(requireContext(), UserProfileActivity::class.java).apply {
            putExtra(UserProfileActivity.EXTRA_USER_ID, userId)
        })
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        _binding = null
        super.onDestroyView()
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

