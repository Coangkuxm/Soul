package com.example.soul.ui.add.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.soul.R
import com.example.soul.data.model.SearchResult
import com.example.soul.databinding.ItemSearchResultBinding

class SearchResultAdapter(
    private val onItemClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchResultAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchResult) {
            binding.apply {
                tvTitle.text = item.title
                tvSubtitle.text = item.subtitle
                
                if (!item.extra.isNullOrEmpty()) {
                    tvExtra.text = item.extra
                    tvExtra.visibility = View.VISIBLE
                } else {
                    tvExtra.visibility = View.GONE
                }

                // Load cover image
                if (!item.coverUrl.isNullOrEmpty()) {
                    Glide.with(ivCover.context)
                        .load(item.coverUrl)
                        .placeholder(R.drawable.ic_default_cover)
                        .error(R.drawable.ic_default_cover)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(ivCover)
                } else {
                    ivCover.setImageResource(R.drawable.ic_default_cover)
                }

                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem == newItem
        }
    }
}
