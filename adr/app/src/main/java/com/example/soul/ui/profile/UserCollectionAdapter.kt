package com.example.soul.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.Collection
import com.example.soul.databinding.ItemCollectionContentBinding

class UserCollectionAdapter(
    private val onCollectionClick: (Collection) -> Unit
) : ListAdapter<Collection, UserCollectionAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCollectionContentBinding.inflate(
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
        private val binding: ItemCollectionContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(collection: Collection) {
            binding.tvTitle.text = collection.name
            binding.tvSubtitle.text = collection.description ?: ""
            Glide.with(binding.ivCover.context)
                .load(collection.coverImageUrl)
                .placeholder(R.drawable.ic_default_cover)
                .error(R.drawable.ic_default_cover)
                .centerCrop()
                .into(binding.ivCover)
            binding.root.setOnClickListener { onCollectionClick(collection) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Collection>() {
        override fun areItemsTheSame(oldItem: Collection, newItem: Collection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Collection, newItem: Collection): Boolean {
            return oldItem == newItem
        }
    }
}
