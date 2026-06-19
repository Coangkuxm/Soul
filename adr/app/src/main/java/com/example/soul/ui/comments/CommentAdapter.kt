package com.example.soul.ui.comments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.Comment
import com.example.soul.databinding.ItemCommentBinding
import com.example.soul.utils.AvatarLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
            val replies = repliesByParent[parent.id].orEmpty()
            items += CommentUiModel(
                comment = parent,
                isReply = false,
                replyToUserName = null,
                hasReplies = replies.isNotEmpty(),
                isLastReply = false
            )
            replies.forEachIndexed { index, reply ->
                items += CommentUiModel(
                    comment = reply,
                    isReply = true,
                    replyToUserName = parentMap[reply.parentId]?.username,
                    hasReplies = false,
                    isLastReply = index == replies.lastIndex
                )
            }
        }

        comments
            .filter { it.parentId != null && parentMap[it.parentId] == null }
            .forEach { orphanReply ->
                items += CommentUiModel(orphanReply, true, null, false, isLastReply = true)
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
            binding.tvUsername.text = comment.username ?: "Người dùng"
            binding.tvContent.text = comment.content
            binding.tvTime.text = buildTimeLabel(comment)
            binding.tvLikeCount.text = comment.likeCount.toString()
            binding.btnLike.text = if (comment.isLiked) "Đã thích" else "Thích"
            binding.btnLike.setTextColor(
                binding.root.context.getColor(
                    if (comment.isLiked) R.color.primary else R.color.text_hint
                )
            )

            if (item.isReply) {
                // Reply: hiện gutter có đường nối (nửa trên + khúc nối), nửa dưới chỉ
                // hiện khi còn reply phía sau để nối tiếp luồng.
                binding.threadGutter.visibility = View.VISIBLE
                binding.threadLineBottom.visibility = if (item.isLastReply) View.GONE else View.VISIBLE
                binding.tvReplyMeta.visibility = View.VISIBLE
                binding.tvReplyMeta.text = item.replyToUserName?.let { "Trả lời $it" } ?: "Trả lời"
            } else {
                binding.threadGutter.visibility = View.GONE
                binding.tvReplyMeta.visibility = View.GONE
                binding.tvReplyMeta.text = ""
            }
            // Cmt gốc có reply -> kéo đường nối từ dưới avatar xuống các reply
            binding.parentDownLine.visibility = if (item.hasReplies) View.VISIBLE else View.GONE

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
                menu.add("Sửa")
                menu.add("Xóa")
                setOnMenuItemClickListener { item ->
                    when (item.title.toString()) {
                        "Sửa" -> onEditClick(comment)
                        "Xóa" -> onDeleteClick(comment)
                    }
                    true
                }
                show()
            }
        }
    }

    private fun buildTimeLabel(comment: Comment): String {
        val base = formatTimeAgo(comment.createdAt)
        // Chỉ coi là đã chỉnh sửa khi updated_at lớn hơn created_at đáng kể (>= 60s).
        // Tránh báo nhầm do sai lệch mili-giây hoặc các thao tác khác (vd: thả tim).
        val created = parseUtcMillis(comment.createdAt)
        val updated = parseUtcMillis(comment.updatedAt)
        val edited = created != null && updated != null && (updated - created) >= 60_000L
        return if (edited && base.isNotBlank()) "$base • Đã chỉnh sửa" else base
    }

    private fun parseUtcMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(value.substringBefore("."))?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun formatTimeAgo(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            // Server trả mốc thời gian theo UTC -> phải parse theo UTC, nếu không
            // sẽ lệch đúng bằng chênh múi giờ (VN +7h) khiến cmt vừa đăng hiện "7h".
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
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

    data class CommentUiModel(
        val comment: Comment,
        val isReply: Boolean,
        val replyToUserName: String?,
        val hasReplies: Boolean = false,
        val isLastReply: Boolean = false
    )
}

