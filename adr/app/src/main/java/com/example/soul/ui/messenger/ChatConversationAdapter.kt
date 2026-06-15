package com.example.soul.ui.messenger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.ChatConversation
import com.example.soul.databinding.ItemChatConversationBinding
import com.example.soul.ui.notification.RelativeTime
import com.example.soul.utils.AvatarLoader

class ChatConversationAdapter(
    private val onItemClick: (ChatConversation) -> Unit
) : ListAdapter<ChatConversation, ChatConversationAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemChatConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatConversation) {
            val displayName = item.otherDisplayName
                ?.takeIf { it.isNotBlank() }
                ?: item.otherUsername
                ?: item.title
                ?: "Không rõ"

            binding.tvName.text = displayName
            binding.tvLastMessage.text = item.lastMessageContent?.takeIf { it.isNotBlank() } ?: "Chưa có tin nhắn"
            binding.tvTime.text = RelativeTime.format(item.lastMessageAt ?: item.createdAt)

            val unread = item.unreadCount.coerceAtLeast(0)
            binding.tvUnreadCount.text = unread.toString()
            binding.tvUnreadCount.visibility = if (unread > 0) View.VISIBLE else View.GONE

            AvatarLoader.load(binding.ivAvatar, item.otherAvatarUrl)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private class Diff : DiffUtil.ItemCallback<ChatConversation>() {
        override fun areItemsTheSame(oldItem: ChatConversation, newItem: ChatConversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatConversation, newItem: ChatConversation): Boolean {
            return oldItem == newItem
        }
    }
}
