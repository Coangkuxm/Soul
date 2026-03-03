package com.example.soul.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.ExploreUser
import com.example.soul.databinding.ItemExploreUserBinding

class ExploreUserAdapter(
    private val onFollowClick: (ExploreUser) -> Unit,
    private val onUserClick: (ExploreUser) -> Unit
) : ListAdapter<ExploreUser, ExploreUserAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExploreUserBinding.inflate(
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
        private val binding: ItemExploreUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: ExploreUser) {
            binding.tvName.text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username
            binding.tvSubtitle.text = "@${user.username}"
            Glide.with(binding.ivAvatar.context)
                .load(user.avatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivAvatar)

            applyFollowVisual(user.isFollowing, animate = false)

            binding.btnFollow.setOnClickListener {
                // Immediate optimistic UI feedback while API request is in flight.
                applyFollowVisual(!user.isFollowing, animate = true)
                onFollowClick(user)
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

            binding.btnFollow.text = if (isFollowing) "Following" else "+ Follow"

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

    class Diff : DiffUtil.ItemCallback<ExploreUser>() {
        override fun areItemsTheSame(oldItem: ExploreUser, newItem: ExploreUser): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExploreUser, newItem: ExploreUser): Boolean {
            return oldItem == newItem
        }
    }
}
