package com.example.soul.ui.home.adapter

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.soul.R
import com.example.soul.data.model.FeedItem
import com.example.soul.databinding.ItemFeedBinding
import com.example.soul.utils.AvatarLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedAdapter(
    private val onItemClick: (FeedItem) -> Unit,
    private val onUserClick: (Int) -> Unit,
    private val onLikeClick: (FeedItem, Boolean) -> Unit,
    private val onPlayClick: (FeedItem) -> Unit,
    private val onCommentClick: (FeedItem) -> Unit,
    private val onShareClick: (FeedItem) -> Unit,
    private val onReportClick: (FeedItem, View) -> Unit
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
                val displayName = feedItem.user.displayName ?: feedItem.user.username
                val spannable = SpannableString(displayName)
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    displayName.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                tvActivity.text = spannable
                tvTime.text = getTimeAgo(feedItem.addedAt)

                AvatarLoader.load(ivAvatar, feedItem.user.avatarUrl)

                tvTitle.text = feedItem.item.title
                val artist = feedItem.item.metadata?.artist
                tvSubtitle.text = if (!artist.isNullOrEmpty()) artist else feedItem.collection.name

                val description = feedItem.note?.trim() ?: feedItem.item.description?.trim()
                if (!description.isNullOrEmpty()) {
                    tvDescription.visibility = View.VISIBLE
                    tvDescription.text = description
                } else {
                    tvDescription.visibility = View.GONE
                    tvDescription.text = ""
                }

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

                val isMusic = feedItem.item.type == "music"
                val previewUrl = feedItem.item.metadata?.previewUrl
                val spotifyUrl = feedItem.item.metadata?.spotifyUrl
                if (isMusic && (!previewUrl.isNullOrEmpty() || !spotifyUrl.isNullOrEmpty())) {
                    btnPlay.visibility = View.VISIBLE
                    btnPlay.setImageResource(R.drawable.ic_play_circle)
                    btnPlay.setOnClickListener { onPlayClick(feedItem) }
                } else {
                    btnPlay.visibility = View.GONE
                }

                updateLikeIcon(btnLike, feedItem.isLiked)
                renderCount(tvLikeCount, feedItem.likeCount)
                btnLike.setOnClickListener {
                    val willLike = !feedItem.isLiked
                    feedItem.isLiked = willLike
                    feedItem.likeCount = (feedItem.likeCount + if (willLike) 1 else -1).coerceAtLeast(0)
                    animateLike(btnLike, willLike)
                    renderCount(tvLikeCount, feedItem.likeCount)
                    onLikeClick(feedItem, willLike)
                }

                renderCount(tvCommentCount, feedItem.commentCount)
                btnComment.setOnClickListener { onCommentClick(feedItem) }
                btnShare.setOnClickListener { onShareClick(feedItem) }
                btnMore.setOnClickListener { onReportClick(feedItem, it) }

                root.setOnClickListener { onItemClick(feedItem) }
                ivAvatar.setOnClickListener { onUserClick(feedItem.user.id) }
                tvActivity.setOnClickListener { onUserClick(feedItem.user.id) }
            }
        }

        private fun renderCount(tv: TextView, count: Int) {
            if (count > 0) {
                tv.visibility = View.VISIBLE
                tv.text = count.toString()
            } else {
                tv.visibility = View.GONE
            }
        }

        private fun updateLikeIcon(btn: ImageView, isLiked: Boolean) {
            btn.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
        }

        private fun animateLike(btn: ImageView, isLiked: Boolean) {
            updateLikeIcon(btn, isLiked)
            btn.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(100)
                .withEndAction {
                    btn.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
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
                    days > 7 -> SimpleDateFormat("d 'thg' M", Locale("vi", "VN")).format(date)
                    days > 0 -> "${days}d"
                    hours > 0 -> "${hours}h"
                    minutes > 0 -> "${minutes}m"
                    else -> "v?a xong"
                }
            } catch (_: Exception) {
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
