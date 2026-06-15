package com.example.soul.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.soul.R

object AvatarLoader {
    fun load(imageView: ImageView, rawUrl: String?) {
        val url = rawUrl?.trim().orEmpty()
        val isValidUrl = url.isNotBlank() &&
            !url.contains("example.com", ignoreCase = true) &&
            (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true))

        if (!isValidUrl) {
            Glide.with(imageView.context).clear(imageView)
            imageView.setImageResource(R.drawable.ic_default_avatar)
            return
        }

        Glide.with(imageView.context).clear(imageView)
        Glide.with(imageView.context)
            .load(url)
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(url))
            .dontAnimate()
            .circleCrop()
            .into(imageView)
    }
}
