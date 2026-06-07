package com.example.soul.ui.messenger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.ChatMessage
import com.example.soul.databinding.ItemChatMessageOtherBinding
import com.example.soul.databinding.ItemChatMessageSelfBinding
import com.example.soul.ui.notification.RelativeTime

class ChatMessageAdapter(
    private val currentUserId: Int
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(Diff()) {

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) {
            VIEW_TYPE_SELF
        } else {
            VIEW_TYPE_OTHER
        }
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
        when (holder) {
            is SelfMessageViewHolder -> holder.bind(message)
            is OtherMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SelfMessageViewHolder(
        private val binding: ItemChatMessageSelfBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) {
            binding.tvContent.text = item.content
            binding.tvTime.text = RelativeTime.format(item.createdAt)
        }
    }

    inner class OtherMessageViewHolder(
        private val binding: ItemChatMessageOtherBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) {
            binding.tvContent.text = item.content
            binding.tvSender.text = item.senderDisplayName?.takeIf { it.isNotBlank() }
                ?: item.senderUsername
                ?: "Người dùng"
            binding.tvTime.text = RelativeTime.format(item.createdAt)

            Glide.with(binding.ivAvatar.context)
                .load(item.senderAvatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivAvatar)
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
    }
}
