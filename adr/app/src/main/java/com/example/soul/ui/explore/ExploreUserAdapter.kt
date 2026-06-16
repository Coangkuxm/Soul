package com.example.soul.ui.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.soul.R
import com.example.soul.data.model.ExploreUser
import com.example.soul.databinding.ItemExploreHeaderBinding
import com.example.soul.databinding.ItemExploreUserBinding
import com.example.soul.utils.AvatarLoader

class ExploreUserAdapter(
    private val onFollowClick: (ExploreUser) -> Unit,
    private val onUserClick: (ExploreUser) -> Unit,
    // ID người dùng hiện tại: ẩn nút "Theo dõi" trên chính mình (không ai tự follow mình).
    private val currentUserId: Int = -1
) : ListAdapter<ExploreRow, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_USER = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ExploreRow.Header -> TYPE_HEADER
        is ExploreRow.UserItem -> TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemExploreHeaderBinding.inflate(inflater, parent, false))
        } else {
            UserViewHolder(ItemExploreUserBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is ExploreRow.Header -> (holder as HeaderViewHolder).bind(row)
            is ExploreRow.UserItem -> (holder as UserViewHolder).bind(row.user)
        }
    }

    inner class HeaderViewHolder(
        private val binding: ItemExploreHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ExploreRow.Header) {
            binding.tvHeader.text = header.title
        }
    }

    inner class UserViewHolder(
        private val binding: ItemExploreUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: ExploreUser) {
            binding.tvName.text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username
            binding.tvSubtitle.text = "@${user.username}"
            AvatarLoader.load(binding.ivAvatar, user.avatarUrl)

            // Không hiển thị nút theo dõi trên chính mình.
            val isSelf = user.id == currentUserId
            binding.btnFollow.visibility = if (isSelf) View.GONE else View.VISIBLE

            if (!isSelf) {
                applyFollowVisual(user.isFollowing, animate = false)
                binding.btnFollow.setOnClickListener {
                    // Immediate optimistic UI feedback while API request is in flight.
                    applyFollowVisual(!user.isFollowing, animate = true)
                    onFollowClick(user)
                }
            } else {
                binding.btnFollow.setOnClickListener(null)
            }
            binding.root.setOnClickListener { onUserClick(user) }
        }

        private fun applyFollowVisual(isFollowing: Boolean, animate: Boolean) {
            val context = binding.root.context
            val startBg = ((binding.btnFollow.backgroundTintList?.defaultColor)
                ?: context.getColor(R.color.primary))
            val endBg = if (isFollowing) {
                context.getColor(R.color.primary_light)
            } else {
                context.getColor(R.color.primary)
            }
            val startText = binding.btnFollow.currentTextColor
            val endText = if (isFollowing) {
                context.getColor(R.color.primary_dark)
            } else {
                context.getColor(R.color.white)
            }

            binding.btnFollow.text = if (isFollowing) "Đang theo dõi" else "+ Theo dõi"

            if (!animate) {
                binding.btnFollow.backgroundTintList = ColorStateList.valueOf(endBg)
                binding.btnFollow.setTextColor(endText)
                return
            }

            binding.btnFollow.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(80)
                .withEndAction {
                    binding.btnFollow.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                .start()

            ValueAnimator.ofArgb(startBg, endBg).apply {
                duration = 220
                addUpdateListener { animator ->
                    binding.btnFollow.backgroundTintList =
                        ColorStateList.valueOf(animator.animatedValue as Int)
                }
                start()
            }
            ValueAnimator.ofArgb(startText, endText).apply {
                duration = 220
                addUpdateListener { animator ->
                    binding.btnFollow.setTextColor(animator.animatedValue as Int)
                }
                start()
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<ExploreRow>() {
        override fun areItemsTheSame(oldItem: ExploreRow, newItem: ExploreRow): Boolean {
            return when {
                oldItem is ExploreRow.Header && newItem is ExploreRow.Header ->
                    oldItem.title == newItem.title
                oldItem is ExploreRow.UserItem && newItem is ExploreRow.UserItem ->
                    oldItem.user.id == newItem.user.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ExploreRow, newItem: ExploreRow): Boolean {
            return oldItem == newItem
        }
    }
}
