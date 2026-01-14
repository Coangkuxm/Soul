package com.example.soul.ui.main.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.soul.R
import com.example.soul.data.model.Collection
import com.example.soul.databinding.ItemAddCollectionBinding
import com.example.soul.databinding.ItemCollectionCardBinding

/**
 * Adapter for displaying collections in a grid
 * Supports both collection items and "Add" placeholder items
 */
class CollectionAdapter(
    private val onCollectionClick: (Collection) -> Unit,
    private val onAddClick: () -> Unit,
    private val onMenuClick: (Collection, View) -> Unit
) : ListAdapter<CollectionAdapter.CollectionItem, RecyclerView.ViewHolder>(CollectionDiffCallback()) {

    companion object {
        private const val TAG = "CollectionAdapter"
        private const val VIEW_TYPE_COLLECTION = 0
        private const val VIEW_TYPE_ADD = 1
        private const val MAX_ITEMS = 6 // 6 items in grid (including add placeholders)
    }

    sealed class CollectionItem {
        data class Data(val collection: Collection) : CollectionItem()
        object AddPlaceholder : CollectionItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CollectionItem.Data -> VIEW_TYPE_COLLECTION
            is CollectionItem.AddPlaceholder -> VIEW_TYPE_ADD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_COLLECTION -> {
                val binding = ItemCollectionCardBinding.inflate(inflater, parent, false)
                CollectionViewHolder(binding)
            }
            VIEW_TYPE_ADD -> {
                val binding = ItemAddCollectionBinding.inflate(inflater, parent, false)
                AddViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CollectionItem.Data -> (holder as CollectionViewHolder).bind(item.collection)
            is CollectionItem.AddPlaceholder -> (holder as AddViewHolder).bind()
        }
    }

    /**
     * Submit collections and fill remaining slots with Add placeholders
     */
    fun submitCollections(collections: List<Collection>) {
        Log.d(TAG, "submitCollections: received ${collections.size} collections")
        val items = mutableListOf<CollectionItem>()
        
        // Add existing collections
        collections.take(MAX_ITEMS).forEach { collection ->
            Log.d(TAG, "Adding collection: ${collection.name}, cover: ${collection.coverImageUrl}")
            items.add(CollectionItem.Data(collection))
        }
        
        // Fill remaining slots with Add placeholders
        while (items.size < MAX_ITEMS) {
            items.add(CollectionItem.AddPlaceholder)
        }
        
        Log.d(TAG, "Total items to display: ${items.size}")
        submitList(items)
    }

    inner class CollectionViewHolder(
        private val binding: ItemCollectionCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(collection: Collection) {
            Log.d(TAG, "Binding collection: ${collection.name}")
            binding.apply {
                // Set collection name
                tvCollectionName.text = collection.name

                // Load cover image - check if URL is valid (not example.com or empty)
                val imageUrl = collection.coverImageUrl
                val isValidUrl = !imageUrl.isNullOrEmpty() && 
                    !imageUrl.contains("example.com") &&
                    (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))
                
                if (isValidUrl) {
                    Glide.with(ivCover.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_default_cover)
                        .error(R.drawable.ic_default_cover)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(ivCover)
                } else {
                    ivCover.setImageResource(R.drawable.ic_default_cover)
                }

                // Show play button for music/song collections
                val isMusicType = collection.name.lowercase().contains("song") ||
                        collection.name.lowercase().contains("music") ||
                        collection.name.lowercase().contains("album")
                ivPlayButton.visibility = if (isMusicType) View.VISIBLE else View.GONE

                // Click listeners
                root.setOnClickListener { onCollectionClick(collection) }
                btnMenu.setOnClickListener { view -> onMenuClick(collection, view) }
            }
        }
    }

    inner class AddViewHolder(
        private val binding: ItemAddCollectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.btnAdd.setOnClickListener { onAddClick() }
            binding.root.setOnClickListener { onAddClick() }
        }
    }

    class CollectionDiffCallback : DiffUtil.ItemCallback<CollectionItem>() {
        override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem): Boolean {
            return when {
                oldItem is CollectionItem.Data && newItem is CollectionItem.Data ->
                    oldItem.collection.id == newItem.collection.id
                oldItem is CollectionItem.AddPlaceholder && newItem is CollectionItem.AddPlaceholder -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem): Boolean {
            return oldItem == newItem
        }
    }
}
