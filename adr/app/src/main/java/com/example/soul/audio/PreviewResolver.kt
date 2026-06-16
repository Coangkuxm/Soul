package com.example.soul.audio

import android.util.Log
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

    private const val TAG = "PreviewResolver"

    private val cache = ConcurrentHashMap<String, String>()

    suspend fun resolve(title: String?, artist: String?, spotifyPreviewUrl: String?): String? {
        if (!spotifyPreviewUrl.isNullOrEmpty()) {
            Log.d(TAG, "Dùng preview Spotify cho: $title")
            return spotifyPreviewUrl
        }

        val key = (title.orEmpty() + "|" + artist.orEmpty()).lowercase().trim()
        cache[key]?.let {
            Log.d(TAG, "Dùng preview Deezer đã cache cho: $title")
            return it
        }

        // Thử lần lượt: "tên + nghệ sĩ", rồi chỉ "tên bài".
        val candidates = listOf(
            listOfNotNull(title, artist).joinToString(" ").trim(),
            title?.trim().orEmpty()
        ).filter { it.isNotEmpty() }.distinct()

        if (candidates.isEmpty()) {
            Log.w(TAG, "Không có tên bài để tra Deezer")
            return null
        }

        for (query in candidates) {
            repeat(2) { attempt ->
                searchDeezer(query)?.let {
                    cache[key] = it
                    return it
                }
                if (attempt == 0) delay(450)
            }
        }
        Log.w(TAG, "Không tìm được preview nào cho: $title (${candidates.joinToString(" | ")})")
        return null
    }

    private suspend fun searchDeezer(query: String): String? {
        return try {
            val response = DeezerRetrofitClient.apiService.search(query)
            val tracks = response.data ?: emptyList()
            val preview = tracks.firstOrNull { !it.preview.isNullOrEmpty() }?.preview
            Log.d(TAG, "Deezer '$query': ${tracks.size} kết quả, preview=${if (preview != null) "có" else "không"}")
            preview?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "Deezer '$query' lỗi: ${e.message}")
            null
        }
    }
}
