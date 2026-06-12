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
        when (holder) {
            is SelfMessageViewHolder -> holder.bind(message)
            is OtherMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SelfMessageViewHolder(
        private val binding: ItemChatMessageSelfBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) {
            bindSharedPostCard(
                item = item,
                contentView = binding.tvContent,
                cardView = binding.cardSharedPost,
                titleView = binding.tvSharedTitle,
                subtitleView = binding.tvSharedSubtitle,
                noteView = binding.tvSharedNote,
                coverView = binding.ivSharedCover
            )
            binding.tvTime.text = RelativeTime.format(item.createdAt)
        }
    }

    inner class OtherMessageViewHolder(
        private val binding: ItemChatMessageOtherBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) {
            bindSharedPostCard(
                item = item,
                contentView = binding.tvContent,
                cardView = binding.cardSharedPost,
                titleView = binding.tvSharedTitle,
                subtitleView = binding.tvSharedSubtitle,
                noteView = binding.tvSharedNote,
                coverView = binding.ivSharedCover
            )
            binding.tvSender.text = item.senderDisplayName?.takeIf { it.isNotBlank() }
                ?: item.senderUsername
                ?: "Ngu?i dùng"
            binding.tvTime.text = RelativeTime.format(item.createdAt)

            Glide.with(binding.ivAvatar.context)
                .load(item.senderAvatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivAvatar)
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
