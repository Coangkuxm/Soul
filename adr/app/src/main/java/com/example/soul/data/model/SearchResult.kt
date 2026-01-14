package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

/**
 * Spotify search result item
 */
data class SpotifyTrack(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("artists")
    val artists: String,
    
    @SerializedName("album")
    val album: String,
    
    @SerializedName("preview_url")
    val previewUrl: String?,
    
    @SerializedName("external_url")
    val externalUrl: String?,
    
    @SerializedName("cover_url")
    val coverUrl: String?
)

data class SpotifySearchResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: List<SpotifyTrack>
)

/**
 * TMDB search result item
 */
data class TMDBResult(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("name")
    val name: String?, // for TV shows
    
    @SerializedName("overview")
    val overview: String?,
    
    @SerializedName("poster_path")
    val posterPath: String?,
    
    @SerializedName("release_date")
    val releaseDate: String?,
    
    @SerializedName("first_air_date")
    val firstAirDate: String?,
    
    @SerializedName("media_type")
    val mediaType: String?,
    
    @SerializedName("vote_average")
    val voteAverage: Double?
) {
    fun getDisplayTitle(): String = title ?: name ?: "Unknown"
    
    fun getYear(): String? {
        val date = releaseDate ?: firstAirDate
        return date?.take(4)
    }
    
    fun getPosterUrl(): String? {
        return posterPath?.let { "https://image.tmdb.org/t/p/w200$it" }
    }
}

data class TMDBSearchData(
    @SerializedName("results")
    val results: List<TMDBResult>,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("total_pages")
    val totalPages: Int,
    
    @SerializedName("total_results")
    val totalResults: Int
)

data class TMDBSearchResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: TMDBSearchData
)

/**
 * Generic search result for UI
 */
data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val extra: String?,
    val coverUrl: String?,
    val type: String, // "music", "movie", "tv"
    val externalId: String,
    val metadata: Map<String, Any?>
)
