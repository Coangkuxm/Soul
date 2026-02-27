package com.example.soul.ui.collection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.model.CollectionContentItem
import com.example.soul.databinding.ItemCollectionContentBinding
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class CollectionContentAdapter(
    private val onItemClick: (CollectionContentItem) -> Unit
) : ListAdapter<CollectionContentItem, CollectionContentAdapter.ViewHolder>(Diff()) {

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
        fun bind(item: CollectionContentItem) {
            val title = resolveTitle(item)
            val subtitle = resolveSubtitle(item)
            val cover = resolveCoverUrl(item)

            binding.tvTitle.text = title
            binding.tvSubtitle.text = subtitle
            Glide.with(binding.ivCover.context)
                .load(cover)
                .placeholder(R.drawable.ic_default_cover)
                .error(R.drawable.ic_default_cover)
                .centerCrop()
                .into(binding.ivCover)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private fun resolveTitle(item: CollectionContentItem): String {
        if (!item.title.isNullOrBlank()) return item.title
        val meta = item.metadata
        return when {
            meta?.has("name") == true -> meta.get("name").asString
            !item.spotifyType.isNullOrBlank() -> "Spotify ${item.spotifyType}"
            else -> "Untitled"
        }
    }

    private fun resolveSubtitle(item: CollectionContentItem): String {
        if (item.type == "spotify") {
            val meta = item.metadata ?: return "Spotify item"
            if (meta.has("artists")) {
                return parseArtists(meta.getAsJsonArray("artists"))
            }
            return item.spotifyType ?: "Spotify item"
        }

        val meta = item.metadata
        if (meta != null && meta.has("artist")) return meta.get("artist").asString
        return item.type ?: "Item"
    }

    private fun resolveCoverUrl(item: CollectionContentItem): String? {
        if (!item.coverImageUrl.isNullOrBlank()) return item.coverImageUrl
        val meta = item.metadata ?: return null

        if (meta.has("album")) {
            val album = meta.getAsJsonObject("album")
            val images = album?.getAsJsonArray("images")
            val url = parseFirstImage(images)
            if (!url.isNullOrBlank()) return url
        }

        if (meta.has("images")) {
            val images = meta.get("images")
            if (images.isJsonArray) return parseFirstImage(images.asJsonArray)
            if (images.isJsonObject) {
                val imageObj: JsonObject = images.asJsonObject
                if (imageObj.has("url")) return imageObj.get("url").asString
            }
        }
        return null
    }

    private fun parseArtists(artists: JsonArray?): String {
        if (artists == null || artists.size() == 0) return "Spotify item"
        return try {
            val names = artists.take(2).mapNotNull { element ->
                if (!element.isJsonObject) return@mapNotNull null
                val obj = element.asJsonObject
                if (obj.has("name")) obj.get("name").asString else null
            }
            if (names.isEmpty()) "Spotify item" else names.joinToString(", ")
        } catch (_: Exception) {
            "Spotify item"
        }
    }

    private fun parseFirstImage(images: JsonArray?): String? {
        if (images == null || images.size() == 0) return null
        return try {
            val first = images[0]
            if (!first.isJsonObject) return null
            val obj = first.asJsonObject
            if (obj.has("url")) obj.get("url").asString else null
        } catch (_: Exception) {
            null
        }
    }

    class Diff : DiffUtil.ItemCallback<CollectionContentItem>() {
        override fun areItemsTheSame(oldItem: CollectionContentItem, newItem: CollectionContentItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CollectionContentItem, newItem: CollectionContentItem): Boolean {
            return oldItem == newItem
        }
    }
}
