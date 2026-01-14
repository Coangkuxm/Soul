package com.example.soul.ui.home.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.soul.R
import com.example.soul.data.model.FeedItem
import com.example.soul.databinding.ItemFeedBinding
import java.text.SimpleDateFormat
import java.util.*

class FeedAdapter(
    private val onItemClick: (FeedItem) -> Unit,
    private val onUserClick: (Int) -> Unit
) : ListAdapter<FeedItem, FeedAdapter.FeedViewHolder>(FeedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemFeedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FeedViewHolder(
        private val binding: ItemFeedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(feedItem: FeedItem) {
            binding.apply {
                // User activity text: "Username is listening to a new song"
                val displayName = feedItem.user.displayName ?: feedItem.user.username
                val activityText = feedItem.activityText ?: "added something new"
                val fullText = "$displayName $activityText"
                
                // Make username bold
                val spannable = SpannableString(fullText)
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    displayName.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                tvActivity.text = spannable

                // Time ago
                tvTime.text = getTimeAgo(feedItem.addedAt)

                // Load avatar
                val avatarUrl = feedItem.user.avatarUrl
                if (!avatarUrl.isNullOrEmpty() && !avatarUrl.contains("example.com")) {
                    Glide.with(ivAvatar.context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(ivAvatar)
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                }

                // Item info
                tvTitle.text = feedItem.item.title
                
                // Subtitle (artist from metadata)
                val artist = feedItem.item.metadata?.artist
                if (!artist.isNullOrEmpty()) {
                    tvSubtitle.text = artist
                } else {
                    tvSubtitle.text = feedItem.collection.name
                }

                // Load cover image
                val coverUrl = feedItem.item.coverImageUrl
                if (!coverUrl.isNullOrEmpty() && !coverUrl.contains("example.com")) {
                    Glide.with(ivCover.context)
                        .load(coverUrl)
                        .placeholder(R.drawable.ic_default_cover)
                        .error(R.drawable.ic_default_cover)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(ivCover)
                } else {
                    ivCover.setImageResource(R.drawable.ic_default_cover)
                }

                // Click listeners
                root.setOnClickListener { onItemClick(feedItem) }
                ivAvatar.setOnClickListener { onUserClick(feedItem.user.id) }
                tvActivity.setOnClickListener { onUserClick(feedItem.user.id) }
            }
        }

        private fun getTimeAgo(dateString: String?): String {
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
                    days > 0 -> "${days}d"
                    hours > 0 -> "${hours}h"
                    minutes > 0 -> "${minutes}m"
                    else -> "now"
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return oldItem == newItem
        }
    }
}
