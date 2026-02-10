package com.example.soul.ui.comments

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityCommentsBinding
import kotlinx.coroutines.launch

class CommentsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET_TYPE = "target_type"
        const val EXTRA_TARGET_ID = "target_id"
        const val EXTRA_TITLE = "title"
    }

    private lateinit var binding: ActivityCommentsBinding
    private lateinit var adapter: CommentAdapter
    private lateinit var authPreferences: AuthPreferences

    private var targetType: String = ""
    private var targetId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        targetType = intent.getStringExtra(EXTRA_TARGET_TYPE) ?: ""
        targetId = intent.getIntExtra(EXTRA_TARGET_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Comments"

        binding.tvTitle.text = title
        binding.btnBack.setOnClickListener { finish() }

        adapter = CommentAdapter()
        binding.rvComments.layoutManager = LinearLayoutManager(this)
        binding.rvComments.adapter = adapter

        binding.btnSend.setOnClickListener {
            val content = binding.etComment.text.toString().trim()
            if (content.isEmpty()) return@setOnClickListener
            createComment(content)
        }

        loadComments()
    }

    private fun loadComments() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getComments(
                    targetType = targetType,
                    targetId = targetId
                )
                if (response.isSuccessful && response.body() != null) {
                    adapter.submitList(response.body()!!.data)
                } else {
                    Toast.makeText(this@CommentsActivity, "Failed to load comments", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CommentsActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createComment(content: String) {
        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@CommentsActivity, "Not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val body = mapOf(
                    "content" to content,
                    "targetId" to targetId,
                    "targetType" to targetType
                )

                val response = RetrofitClient.apiService.createComment("Bearer $token", body)
                if (response.isSuccessful) {
                    binding.etComment.setText("")
                    loadComments()
                    binding.rvComments.scrollToPosition(0)
                } else {
                    Toast.makeText(this@CommentsActivity, "Failed to send", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CommentsActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
