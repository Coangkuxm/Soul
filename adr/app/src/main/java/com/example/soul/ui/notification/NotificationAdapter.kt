package com.example.soul.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.NotificationItem
import com.example.soul.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemNotificationBinding,
        private val onClick: (NotificationItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItem) {
            binding.root.setOnClickListener { onClick(item) }
            binding.tvTitle.text = "${item.senderUsername ?: "User"} ${typeText(item.notificationType)}"
            binding.tvSubtitle.text = when (item.targetType) {
                "collection" -> "Collection #${item.targetId}"
                "item" -> "Item #${item.targetId}"
                "user" -> "User #${item.targetId}"
                else -> item.targetType
            }
            binding.tvTime.text = RelativeTime.format(item.createdAt)
            binding.unreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE
            Glide.with(binding.ivAvatar)
                .load(item.senderAvatar)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivAvatar)
        }

        private fun typeText(type: String): String {
            return when (type) {
                "follow" -> "started following you"
                "like" -> "liked your content"
                "comment" -> "commented on your content"
                "mention" -> "mentioned you"
                else -> "sent a notification"
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem == newItem
        }
    }
}

