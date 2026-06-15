package com.example.soul.ui.comments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.Comment
import com.example.soul.databinding.ItemCommentBinding
import com.example.soul.utils.AvatarLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    private val currentUserId: Int,
    private val onReplyClick: (Comment) -> Unit,
    private val onLikeClick: (Comment, Boolean) -> Unit,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val items = mutableListOf<CommentUiModel>()

    fun submitComments(comments: List<Comment>) {
        val parentMap = comments.associateBy { it.id }
        val parents = comments.filter { it.parentId == null }
        val repliesByParent = comments
            .filter { it.parentId != null }
            .groupBy { it.parentId }

        items.clear()
        parents.forEach { parent ->
            items += CommentUiModel(parent, false, null)
            repliesByParent[parent.id].orEmpty().forEach { reply ->
                items += CommentUiModel(reply, true, parentMap[reply.parentId]?.username)
            }
        }

        comments
            .filter { it.parentId != null && parentMap[it.parentId] == null }
            .forEach { orphanReply ->
                items += CommentUiModel(orphanReply, true, null)
            }

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommentUiModel) {
            val comment = item.comment
            binding.tvUsername.text = comment.username ?: "Nguoi dung"
            binding.tvContent.text = comment.content
            binding.tvTime.text = buildTimeLabel(comment)
            binding.tvLikeCount.text = comment.likeCount.toString()
            binding.btnLike.text = if (comment.isLiked) "Da thich" else "Thich"
            binding.btnLike.setTextColor(
                binding.root.context.getColor(
                    if (comment.isLiked) R.color.primary else R.color.text_hint
                )
            )

            if (item.isReply) {
                binding.rootComment.updatePadding(left = 44)
                binding.tvReplyMeta.visibility = View.VISIBLE
                binding.tvReplyMeta.text = item.replyToUserName?.let { "Tra loi $it" } ?: "Tra loi"
            } else {
                binding.rootComment.updatePadding(left = 0)
                binding.tvReplyMeta.visibility = View.GONE
                binding.tvReplyMeta.text = ""
            }

            val isOwnComment = comment.userId == currentUserId
            binding.btnMore.visibility = if (isOwnComment) View.VISIBLE else View.GONE
            binding.btnMore.setOnClickListener { anchor ->
                showCommentMenu(anchor, comment)
            }

            binding.btnReply.setOnClickListener { onReplyClick(comment) }
            binding.btnLike.setOnClickListener { onLikeClick(comment, !comment.isLiked) }

            AvatarLoader.load(binding.ivAvatar, comment.avatarUrl)
        }

        private fun showCommentMenu(anchor: View, comment: Comment) {
            PopupMenu(anchor.context, anchor).apply {
                menu.add("Sua")
                menu.add("Xoa")
                setOnMenuItemClickListener { item ->
                    when (item.title.toString()) {
                        "Sua" -> onEditClick(comment)
                        "Xoa" -> onDeleteClick(comment)
                    }
                    true
                }
                show()
            }
        }
    }

    private fun buildTimeLabel(comment: Comment): String {
        val base = formatTimeAgo(comment.createdAt)
        val edited = !comment.updatedAt.isNullOrBlank() &&
            !comment.createdAt.isNullOrBlank() &&
            comment.updatedAt.substringBefore(".") != comment.createdAt.substringBefore(".")
        return if (edited && base.isNotBlank()) "$base • Da chinh sua" else base
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
                else -> "Vua xong"
            }
        } catch (_: Exception) {
            ""
        }
    }

    data class CommentUiModel(
        val comment: Comment,
        val isReply: Boolean,
        val replyToUserName: String?
    )
}

