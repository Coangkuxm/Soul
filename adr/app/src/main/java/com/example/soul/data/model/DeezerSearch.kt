package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

data class DeezerSearchResponse(
    // Deezer trả {"error":{...}} khi lỗi/giới hạn -> không có "data", nên để nullable cho an toàn.
    @SerializedName("data")
    val data: List<DeezerTrack>? = null
)

data class DeezerTrack(
    @SerializedName("id")
    val id: Long,
    @SerializedName("title")
    val title: String,
    @SerializedName("preview")
    val preview: String?,
    @SerializedName("artist")
    val artist: DeezerArtist?
)

data class DeezerArtist(
    @SerializedName("name")
    val name: String?
)
