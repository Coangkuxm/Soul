package com.example.soul.ui.comments

import android.os.Bundle
import android.widget.Toast
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.Comment
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityCommentsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET_TYPE = "target_type"
        const val EXTRA_TARGET_ID = "target_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_POST_USER_NAME = "post_user_name"
        const val EXTRA_POST_USER_AVATAR = "post_user_avatar"
        const val EXTRA_POST_NOTE = "post_note"
        const val EXTRA_POST_ITEM_TITLE = "post_item_title"
        const val EXTRA_POST_ITEM_SUBTITLE = "post_item_subtitle"
        const val EXTRA_POST_ITEM_COVER = "post_item_cover"
        const val EXTRA_POST_ADDED_AT = "post_added_at"
    }

    private lateinit var binding: ActivityCommentsBinding
    private lateinit var adapter: CommentAdapter
    private lateinit var authPreferences: AuthPreferences
    private var currentUserId: Int = 0

    private var targetType: String = ""
    private var targetId: Int = 0
    private var replyingToComment: Comment? = null
    private var editingComment: Comment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        currentUserId = authPreferences.getUser()?.id ?: 0
        targetType = intent.getStringExtra(EXTRA_TARGET_TYPE) ?: ""
        targetId = intent.getIntExtra(EXTRA_TARGET_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Bình luận"

        binding.tvTitle.text = title
        binding.btnBack.setOnClickListener { finish() }

        adapter = CommentAdapter(
            currentUserId = currentUserId,
            onReplyClick = { comment -> beginReply(comment) },
            onLikeClick = { comment, shouldLike -> toggleCommentLike(comment, shouldLike) },
            onEditClick = { comment -> beginEdit(comment) },
            onDeleteClick = { comment -> confirmDelete(comment) }
        )
        binding.rvComments.layoutManager = LinearLayoutManager(this)
        binding.rvComments.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadComments()
        }

        binding.btnSend.setOnClickListener {
            val content = binding.etComment.text.toString().trim()
            if (content.isEmpty()) return@setOnClickListener
            submitComment(content)
        }
        binding.btnCancelReply.setOnClickListener {
            clearComposerState()
        }

        bindPostPreview()
        loadComments()
    }

    private fun bindPostPreview() {
        val postTitle = intent.getStringExtra(EXTRA_POST_ITEM_TITLE).orEmpty()
        val postSubtitle = intent.getStringExtra(EXTRA_POST_ITEM_SUBTITLE).orEmpty()
        val postNote = intent.getStringExtra(EXTRA_POST_NOTE).orEmpty()
        val postUserName = intent.getStringExtra(EXTRA_POST_USER_NAME).orEmpty()
        val postUserAvatar = intent.getStringExtra(EXTRA_POST_USER_AVATAR).orEmpty()
        val postCover = intent.getStringExtra(EXTRA_POST_ITEM_COVER).orEmpty()
        val postAddedAt = intent.getStringExtra(EXTRA_POST_ADDED_AT)

        val hasPreview = postTitle.isNotBlank() || postNote.isNotBlank() || postSubtitle.isNotBlank()
        if (!hasPreview) {
            binding.layoutPostPreview.visibility = View.GONE
            return
        }

        binding.layoutPostPreview.visibility = View.VISIBLE

        if (postUserName.isNotBlank()) {
            binding.tvPostUserName.visibility = View.VISIBLE
            binding.tvPostUserName.text = postUserName
        } else {
            binding.tvPostUserName.visibility = View.GONE
        }

        val displayTime = formatTimeAgo(postAddedAt)
        if (displayTime.isNotBlank()) {
            binding.tvPostTime.visibility = View.VISIBLE
            binding.tvPostTime.text = displayTime
        } else {
            binding.tvPostTime.visibility = View.GONE
        }

        if (postNote.isNotBlank()) {
            binding.tvPostNote.visibility = View.VISIBLE
            binding.tvPostNote.text = postNote
        } else {
            binding.tvPostNote.visibility = View.GONE
        }

        binding.tvPostItemTitle.text = postTitle.ifBlank { titleFromFallback() }

        if (postSubtitle.isNotBlank()) {
            binding.tvPostItemSubtitle.visibility = View.VISIBLE
            binding.tvPostItemSubtitle.text = postSubtitle
        } else {
            binding.tvPostItemSubtitle.visibility = View.GONE
        }

        if (postUserAvatar.isNotBlank() && !postUserAvatar.contains("example.com")) {
            Glide.with(this)
                .load(postUserAvatar)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivPostUserAvatar)
        } else {
            binding.ivPostUserAvatar.setImageResource(R.drawable.ic_default_avatar)
        }

        if (postCover.isNotBlank() && !postCover.contains("example.com")) {
            Glide.with(this)
                .load(postCover)
                .placeholder(R.drawable.ic_default_cover)
                .error(R.drawable.ic_default_cover)
                .centerCrop()
                .into(binding.ivPostCover)
        } else {
            binding.ivPostCover.setImageResource(R.drawable.ic_default_cover)
        }
    }

    private fun titleFromFallback(): String {
        return intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Bài viết" }
    }

    private fun formatTimeAgo(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = format.parse(dateString.substringBefore(".")) ?: return ""
            val now = Date()
            val diff = now.time - date.time
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                days > 7 -> SimpleDateFormat("d 'thg' M", Locale("vi", "VN")).format(date)
                days > 0 -> "${days}d"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "Vừa xong"
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun loadComments() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@CommentsActivity, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val response = RetrofitClient.apiService.getComments(
                    token = "Bearer $token",
                    targetType = targetType,
                    targetId = targetId
                )
                if (response.isSuccessful && response.body() != null) {
                    adapter.submitComments(response.body()!!.data)
                } else {
                    Toast.makeText(this@CommentsActivity, "Không thể tải bình luận", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CommentsActivity, "Lỗi mạng", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun submitComment(content: String) {
        binding.btnSend.isEnabled = false
        hideKeyboard()
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@CommentsActivity, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val response = if (editingComment != null) {
                    RetrofitClient.apiService.updateComment(
                        token = "Bearer $token",
                        commentId = editingComment!!.id,
                        body = mapOf("content" to content)
                    )
                } else {
                    val body = buildMap<String, Any?> {
                        put("content", content)
                        put("targetId", targetId)
                        put("targetType", targetType)
                        replyingToComment?.id?.let { put("parentId", it) }
                    }
                    RetrofitClient.apiService.createComment("Bearer $token", body)
                }
                if (response.isSuccessful && response.body() != null) {
                    binding.etComment.setText("")
                    clearComposerState()
                    loadComments()
                } else {
                    val message = if (editingComment != null) "Không thể cập nhật" else "Không thể gửi"
                    Toast.makeText(this@CommentsActivity, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CommentsActivity, "Lỗi mạng", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSend.isEnabled = true
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etComment.windowToken, 0)
    }

    private fun beginReply(comment: Comment) {
        editingComment = null
        replyingToComment = comment
        binding.layoutReplyingTo.visibility = View.VISIBLE
        binding.tvReplyingTo.text = "Đang trả lời ${comment.username ?: "người dùng"}"
        binding.btnSend.text = "Gửi"
        binding.etComment.requestFocus()
    }

    private fun beginEdit(comment: Comment) {
        replyingToComment = null
        editingComment = comment
        binding.layoutReplyingTo.visibility = View.VISIBLE
        binding.tvReplyingTo.text = "Đang chỉnh sửa bình luận"
        binding.btnSend.text = "Lưu"
        binding.etComment.setText(comment.content)
        binding.etComment.setSelection(binding.etComment.text?.length ?: 0)
        binding.etComment.requestFocus()
    }

    private fun toggleCommentLike(comment: Comment, shouldLike: Boolean) {
        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@CommentsActivity, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val body = mapOf<String, Any>(
                    "targetId" to comment.id,
                    "targetType" to "comment"
                )
                val response = if (shouldLike) {
                    RetrofitClient.apiService.likeItem("Bearer $token", body)
                } else {
                    RetrofitClient.apiService.unlikeItem("Bearer $token", body)
                }

                if (response.isSuccessful) {
                    loadComments()
                } else {
                    Toast.makeText(this@CommentsActivity, "Không thể cập nhật lượt thích", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@CommentsActivity, "Lỗi mạng", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(comment: Comment) {
        AlertDialog.Builder(this)
            .setTitle("Xóa bình luận")
            .setMessage("Bình luận này sẽ bị xóa khỏi cuộc trò chuyện.")
            .setPositiveButton("Xóa") { _, _ -> deleteComment(comment) }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteComment(comment: Comment) {
        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@CommentsActivity, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val response = RetrofitClient.apiService.deleteComment("Bearer $token", comment.id)
                if (response.isSuccessful) {
                    if (editingComment?.id == comment.id || replyingToComment?.id == comment.id) {
                        clearComposerState()
                    }
                    loadComments()
                } else {
                    Toast.makeText(this@CommentsActivity, "Không thể xóa bình luận", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@CommentsActivity, "Lỗi mạng", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearComposerState() {
        replyingToComment = null
        editingComment = null
        binding.layoutReplyingTo.visibility = View.GONE
        binding.tvReplyingTo.text = ""
        binding.btnSend.text = "Gửi"
    }
}

