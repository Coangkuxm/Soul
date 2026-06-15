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
import com.example.soul.utils.AvatarLoader

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
            binding.tvTitle.text = "${item.senderUsername ?: "Người dùng"} ${typeText(item.notificationType)}"
            binding.tvSubtitle.text = when (item.targetType) {
                "collection" -> "Bộ sưu tập #${item.targetId}"
                "item" -> "Mục #${item.targetId}"
                "user" -> "Người dùng #${item.targetId}"
                else -> item.targetType
            }
            binding.tvTime.text = RelativeTime.format(item.createdAt)
            binding.unreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE
            AvatarLoader.load(binding.ivAvatar, item.senderAvatar)
        }

        private fun typeText(type: String): String {
            return when (type) {
                "follow" -> "đã theo dõi bạn"
                "like" -> "đã thích nội dung của bạn"
                "comment" -> "đã bình luận về nội dung của bạn"
                "mention" -> "đã nhắc đến bạn"
                else -> "đã gửi một thông báo"
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
