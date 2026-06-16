package com.example.soul.audio

import com.example.soul.data.remote.DeezerRetrofitClient
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * Tìm URL preview để phát thử một bài hát.
 *
 * Ưu tiên preview của Spotify (nếu có), nếu không thì tra Deezer theo "tên + nghệ sĩ".
 * - Cache kết quả theo bài để lần sau phát lại là tức thì (không gọi mạng lại).
 * - Thử lại 1 lần khi Deezer trả rỗng/lỗi (tránh trượt ở lần gọi đầu / cold-start),
 *   đây là nguyên nhân khiến "ấn vài bài đầu bị nhảy hộp thoại, ấn lại thì chạy".
 */
object PreviewResolver {

    private val cache = ConcurrentHashMap<String, String>()

    suspend fun resolve(title: String?, artist: String?, spotifyPreviewUrl: String?): String? {
        if (!spotifyPreviewUrl.isNullOrEmpty()) return spotifyPreviewUrl

        val key = (title.orEmpty() + "|" + artist.orEmpty()).lowercase().trim()
        cache[key]?.let { return it }

        val query = listOfNotNull(title, artist)
            .joinToString(" ")
            .trim()
            .ifEmpty { return null }

        repeat(2) { attempt ->
            try {
                val response = DeezerRetrofitClient.apiService.search(query)
                val preview = response.data.firstOrNull { !it.preview.isNullOrEmpty() }?.preview
                if (!preview.isNullOrEmpty()) {
                    cache[key] = preview
                    return preview
                }
            } catch (_: Exception) {
                // bỏ qua, thử lại
            }
            if (attempt == 0) delay(450)
        }
        return null
    }
}
