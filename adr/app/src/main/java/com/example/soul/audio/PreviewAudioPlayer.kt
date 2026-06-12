package com.example.soul.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class PreviewAudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val player = ExoPlayer.Builder(appContext).build()
    private var currentUrl: String? = null
    private var onEnded: (() -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onEnded?.invoke()
                }
            }
        })
    }

    fun toggle(url: String) {
        if (currentUrl == url) {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            return
        }

        currentUrl = url
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    fun release() {
        player.release()
    }

    fun stop() {
        player.stop()
        currentUrl = null
    }

    fun setOnEndedListener(listener: (() -> Unit)?) {
        onEnded = listener
    }

    fun isPlaying(): Boolean = player.isPlaying

    fun getPositionMs(): Long = player.currentPosition

    fun getDurationMs(): Long = player.duration
}
