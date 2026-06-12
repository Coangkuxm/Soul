package com.example.soul.ui.messenger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.ShareRecipient
import com.example.soul.databinding.ItemShareRecipientBinding

class ShareRecipientAdapter(
    private val onClick: (ShareRecipient) -> Unit
) : ListAdapter<ShareRecipient, ShareRecipientAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShareRecipientBinding.inflate(
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
        private val binding: ItemShareRecipientBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShareRecipient) {
            binding.tvTitle.text = item.title
            binding.tvSubtitle.text = item.subtitle.orEmpty()

            Glide.with(binding.ivAvatar.context)
                .load(item.avatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivAvatar)

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private class Diff : DiffUtil.ItemCallback<ShareRecipient>() {
        override fun areItemsTheSame(oldItem: ShareRecipient, newItem: ShareRecipient): Boolean {
            return oldItem.mode == newItem.mode &&
                oldItem.conversationId == newItem.conversationId &&
                oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: ShareRecipient, newItem: ShareRecipient): Boolean {
            return oldItem == newItem
        }
    }
}
