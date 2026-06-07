package com.example.soul.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.AdminReportRow
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityAdminReportsBinding
import kotlinx.coroutines.launch

class AdminReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminReportsBinding
    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: AdminReportsAdapter
    private var currentTab: Tab = Tab.POSTS

    private enum class Tab { POSTS, USERS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        if (authPreferences.getUser()?.role != "admin") {
            Toast.makeText(this, "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        adapter = AdminReportsAdapter { row -> onPrimaryAction(row) }

        binding.rvReports.layoutManager = LinearLayoutManager(this)
        binding.rvReports.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }
        binding.btnPosts.setOnClickListener {
            currentTab = Tab.POSTS
            updateTabUi()
            loadReports()
        }
        binding.btnUsers.setOnClickListener {
            currentTab = Tab.USERS
            updateTabUi()
            loadReports()
        }
        binding.swipeRefresh.setOnRefreshListener { loadReports() }

        updateTabUi()
        loadReports()
    }

    private fun updateTabUi() {
        val postsSelected = currentTab == Tab.POSTS
        binding.btnPosts.isEnabled = !postsSelected
        binding.btnUsers.isEnabled = postsSelected
        binding.tvTitle.text = if (postsSelected) "Bài viết bị báo cáo" else "Tài khoản bị báo cáo"
    }

    private fun loadReports() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"
                val rows = when (currentTab) {
                    Tab.POSTS -> {
                        val rs = RetrofitClient.apiService.getReportedPosts(token, "pending")
                        if (!rs.isSuccessful || rs.body() == null) {
                            throw IllegalStateException(rs.errorBody()?.string() ?: "Không tải được danh sách bài viết")
                        }
                        rs.body()!!.data.map { AdminReportRow.Post(it) }
                    }
                    Tab.USERS -> {
                        val rs = RetrofitClient.apiService.getReportedUsers(token, "pending")
                        if (!rs.isSuccessful || rs.body() == null) {
                            throw IllegalStateException(rs.errorBody()?.string() ?: "Không tải được danh sách tài khoản")
                        }
                        rs.body()!!.data.map { AdminReportRow.User(it) }
                    }
                }

                adapter.submitList(rows)
                binding.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                binding.tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@AdminReportsActivity, e.message ?: "Có lỗi xảy ra", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun onPrimaryAction(row: AdminReportRow) {
        when (row) {
            is AdminReportRow.Post -> {
                val locked = row.value.moderationStatus == "locked"
                if (locked) {
                    updatePostLock(row.value.collectionItemId, lock = false, reason = null)
                } else {
                    askReasonAndLockPost(row.value.collectionItemId)
                }
            }
            is AdminReportRow.User -> {
                val locked = row.value.accountStatus == "locked"
                if (locked) {
                    updateUserLock(row.value.userId, lock = false, reason = null)
                } else {
                    askReasonAndLockUser(row.value.userId, row.value.username)
                }
            }
        }
    }

    private fun askReasonAndLockPost(collectionItemId: Int) {
        val reasons = arrayOf("Spam", "Vi phạm nội quy", "Nội dung độc hại", "Khác")
        AlertDialog.Builder(this)
            .setTitle("Khóa bài viết")
            .setItems(reasons) { _, which ->
                updatePostLock(collectionItemId, lock = true, reason = reasons[which])
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun askReasonAndLockUser(userId: Int, username: String) {
        val reasons = arrayOf("Spam", "Quấy rối", "Mạo danh", "Khác")
        AlertDialog.Builder(this)
            .setTitle("Khóa tài khoản @$username")
            .setItems(reasons) { _, which ->
                updateUserLock(userId, lock = true, reason = reasons[which])
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updatePostLock(collectionItemId: Int, lock: Boolean, reason: String?) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"
                val response = if (lock) {
                    RetrofitClient.apiService.lockReportedPost(
                        token,
                        collectionItemId,
                        mapOf("reason" to reason)
                    )
                } else {
                    RetrofitClient.apiService.unlockReportedPost(token, collectionItemId)
                }
                if (!response.isSuccessful) {
                    throw IllegalStateException(response.errorBody()?.string() ?: "Không thể cập nhật trạng thái bài viết")
                }
                Toast.makeText(
                    this@AdminReportsActivity,
                    if (lock) "Đã khóa bài viết" else "Đã mở khóa bài viết",
                    Toast.LENGTH_SHORT
                ).show()
                loadReports()
            } catch (e: Exception) {
                Toast.makeText(this@AdminReportsActivity, e.message ?: "Có lỗi xảy ra", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserLock(userId: Int, lock: Boolean, reason: String?) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"
                val response = if (lock) {
                    RetrofitClient.apiService.lockReportedUser(
                        token,
                        userId,
                        mapOf("reason" to reason)
                    )
                } else {
                    RetrofitClient.apiService.unlockReportedUser(token, userId)
                }
                if (!response.isSuccessful) {
                    throw IllegalStateException(response.errorBody()?.string() ?: "Không thể cập nhật trạng thái tài khoản")
                }
                Toast.makeText(
                    this@AdminReportsActivity,
                    if (lock) "Đã khóa tài khoản" else "Đã mở khóa tài khoản",
                    Toast.LENGTH_SHORT
                ).show()
                loadReports()
            } catch (e: Exception) {
                Toast.makeText(this@AdminReportsActivity, e.message ?: "Có lỗi xảy ra", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
