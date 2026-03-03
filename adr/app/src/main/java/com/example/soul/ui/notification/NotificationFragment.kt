package com.example.soul.ui.notification

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
import com.example.soul.data.model.NotificationItem
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.FragmentNotificationBinding
import com.example.soul.ui.comments.CommentsActivity
import com.example.soul.ui.profile.UserProfileActivity
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: NotificationAdapter
    private var items: MutableList<NotificationItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authPreferences = AuthPreferences(requireContext())
        setupRecycler()
        binding.swipeRefresh.setOnRefreshListener { loadNotifications() }
        loadNotifications()
    }

    private fun setupRecycler() {
        adapter = NotificationAdapter { item ->
            if (!item.isRead) markAsRead(item)
            openNotificationTarget(item)
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter
    }

    private fun loadNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            try {
                val token = authPreferences.getToken().orEmpty()
                val response = RetrofitClient.apiService.getNotifications(
                    token = "Bearer $token",
                    page = 1,
                    limit = 50
                )
                if (!response.isSuccessful || response.body() == null) {
                    showError("Unable to load notifications")
                    return@launch
                }
                items = response.body()!!.data.toMutableList()
                adapter.submitList(items.toList())
                binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                showError(e.message ?: "Network error")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun markAsRead(item: NotificationItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = authPreferences.getToken().orEmpty()
                val response = RetrofitClient.apiService.markNotificationAsRead(
                    token = "Bearer $token",
                    notificationId = item.id
                )
                if (response.isSuccessful) {
                    item.isRead = true
                    adapter.submitList(items.toList())
                }
            } catch (_: Exception) {
                // Keep UX simple: do not block tap flow on mark-read failure.
            }
        }
    }

    private fun openNotificationTarget(item: NotificationItem) {
        when (item.notificationType) {
            "follow" -> {
                startActivity(Intent(requireContext(), UserProfileActivity::class.java).apply {
                    putExtra(UserProfileActivity.EXTRA_USER_ID, item.senderId)
                })
            }
            "comment", "like" -> {
                if (item.targetType == "collection" || item.targetType == "item") {
                    startActivity(Intent(requireContext(), CommentsActivity::class.java).apply {
                        putExtra(CommentsActivity.EXTRA_TARGET_TYPE, item.targetType)
                        putExtra(CommentsActivity.EXTRA_TARGET_ID, item.targetId)
                        putExtra(CommentsActivity.EXTRA_TITLE, "Comments")
                    })
                } else {
                    Toast.makeText(requireContext(), "This notification detail is not supported yet", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(requireContext(), "This notification detail is not supported yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
