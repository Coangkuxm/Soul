package com.example.soul.ui.messenger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.ChatMessage
import com.example.soul.databinding.ItemChatMessageOtherBinding
import com.example.soul.databinding.ItemChatMessageSelfBinding
import com.example.soul.ui.notification.RelativeTime
import com.example.soul.utils.AvatarLoader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ChatMessageAdapter(
    private val currentUserId: Int,
    private val onSharedPostClick: (ChatMessage) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(Diff()) {

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_TYPE_SELF else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SELF) {
            SelfMessageViewHolder(
                ItemChatMessageSelfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            OtherMessageViewHolder(
                ItemChatMessageOtherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val prev = if (position > 0) getItem(position - 1) else null
        // Hiện nhãn thời gian khi: là tin đầu, hoặc cách tin trước >= 15 phút.
        val showTime = prev == null || gapMillis(prev.createdAt, message.createdAt) >= GROUP_THRESHOLD_MS
        // Gom sát khi cùng người gửi và trong cùng cụm thời gian.
        val groupedWithPrev = prev != null && prev.senderId == message.senderId && !showTime
        when (holder) {
            is SelfMessageViewHolder -> holder.bind(message, showTime, groupedWithPrev)
            is OtherMessageViewHolder -> holder.bind(message, showTime, groupedWithPrev)
        }
    }

    inner class SelfMessageViewHolder(
        private val binding: ItemChatMessageSelfBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage, showTime: Boolean, groupedWithPrev: Boolean) {
            bindSharedPostCard(
                item = item,
                contentView = binding.tvContent,
                cardView = binding.cardSharedPost,
                titleView = binding.tvSharedTitle,
                subtitleView = binding.tvSharedSubtitle,
                noteView = binding.tvSharedNote,
                coverView = binding.ivSharedCover
            )
            if (showTime) {
                binding.tvTime.visibility = View.VISIBLE
                binding.tvTime.text = RelativeTime.format(item.createdAt)
            } else {
                binding.tvTime.visibility = View.GONE
            }
            applyTopSpacing(itemView, groupedWithPrev)
        }
    }

    inner class OtherMessageViewHolder(
        private val binding: ItemChatMessageOtherBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage, showTime: Boolean, groupedWithPrev: Boolean) {
            bindSharedPostCard(
                item = item,
                contentView = binding.tvContent,
                cardView = binding.cardSharedPost,
                titleView = binding.tvSharedTitle,
                subtitleView = binding.tvSharedSubtitle,
                noteView = binding.tvSharedNote,
                coverView = binding.ivSharedCover
            )
            // Khi gom nhóm cùng người gửi thì ẩn tên cho gọn (giống Messenger)
            if (groupedWithPrev) {
                binding.tvSender.visibility = View.GONE
            } else {
                binding.tvSender.visibility = View.VISIBLE
                binding.tvSender.text = item.senderDisplayName?.takeIf { it.isNotBlank() }
                    ?: item.senderUsername
                    ?: "Người dùng"
            }
            if (showTime) {
                binding.tvTime.visibility = View.VISIBLE
                binding.tvTime.text = RelativeTime.format(item.createdAt)
            } else {
                binding.tvTime.visibility = View.GONE
            }
            // Ẩn avatar ở các tin gộp để tránh lặp; vẫn giữ chỗ cho thẳng hàng
            binding.ivAvatar.visibility = if (groupedWithPrev) View.INVISIBLE else View.VISIBLE
            AvatarLoader.load(binding.ivAvatar, item.senderAvatarUrl)

            applyTopSpacing(itemView, groupedWithPrev)
        }
    }

    private fun bindSharedPostCard(
        item: ChatMessage,
        contentView: TextView,
        cardView: View,
        titleView: TextView,
        subtitleView: TextView,
        noteView: TextView,
        coverView: ImageView
    ) {
        if (item.messageType == "shared_post") {
            contentView.visibility = View.VISIBLE
            contentView.text = item.content
            cardView.visibility = View.VISIBLE

            val meta = item.metadata.orEmpty()
            titleView.text = meta["itemTitle"]?.toString().orEmpty()
            subtitleView.text = meta["itemSubtitle"]?.toString().orEmpty()

            val note = meta["postNote"]?.toString().orEmpty()
            if (note.isBlank()) {
                noteView.visibility = View.GONE
            } else {
                noteView.visibility = View.VISIBLE
                noteView.text = note
            }

            Glide.with(coverView.context)
                .load(meta["coverUrl"]?.toString())
                .placeholder(R.drawable.ic_default_cover)
                .error(R.drawable.ic_default_cover)
                .centerCrop()
                .into(coverView)

            cardView.setOnClickListener { onSharedPostClick(item) }
        } else {
            contentView.visibility = View.VISIBLE
            contentView.text = item.content
            cardView.visibility = View.GONE
            cardView.setOnClickListener(null)
        }
    }

    /** Giãn cách trên của item: gộp thì sát (1dp), tin đầu cụm thì thoáng hơn (10dp). */
    private fun applyTopSpacing(itemView: View, groupedWithPrev: Boolean) {
        val density = itemView.resources.displayMetrics.density
        val top = ((if (groupedWithPrev) 1 else 10) * density).toInt()
        val bottom = (1 * density).toInt()
        itemView.setPadding(itemView.paddingLeft, top, itemView.paddingRight, bottom)
    }

    /** Khoảng cách (ms) giữa 2 mốc thời gian ISO; trả về Long.MAX nếu không phân tích được. */
    private fun gapMillis(prev: String?, current: String?): Long {
        val a = parseMillis(prev)
        val b = parseMillis(current)
        if (a == null || b == null) return Long.MAX_VALUE
        return kotlin.math.abs(b - a)
    }

    private fun parseMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(value.substringBefore("."))?.time
        } catch (_: Exception) {
            null
        }
    }

    private class Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_SELF = 1
        private const val VIEW_TYPE_OTHER = 2
        // Cách nhau >= 15 phút thì mới tách hiển thị mốc thời gian
        private const val GROUP_THRESHOLD_MS = 15 * 60 * 1000L
    }
}

