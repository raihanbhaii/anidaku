package com.anilite

import kotlinx.serialization.Serializable

@Serializable
data class AnimeResponse(
    val results: List<AnimeSummary> = emptyList(),
    val hasNextPage: Boolean = false
)

@Serializable
data class AnimeSummary(
    val id: String,
    val title: AnimeTitle,
    val poster: String? = null,
    val cover: String? = null,
    val type: String? = null,
    val rating: Double? = null,
    val status: String? = null
)

@Serializable
data class AnimeTitle(
    val english: String? = null,
    val romaji: String? = null,
    val native: String? = null
) {
    fun getDisplayTitle(): String = english ?: romaji ?: native ?: "Unknown Title"
}

@Serializable
data class AnimeInfo(
    val id: String,
    val title: AnimeTitle,
    val description: String? = null,
    val coverImage: String? = null,
    val bannerImage: String? = null,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val averageScore: Int? = null,
    val episodes: Int? = null
)

@Serializable
data class EpisodeResponse(
    val providers: Map<String, ProviderEpisodes> = emptyMap()
)

@Serializable
data class ProviderEpisodes(
    val episodes: Map<String, List<Episode>> = emptyMap()
)

@Serializable
data class Episode(
    val id: String,
    val number: Int,
    val title: String? = null,
    val image: String? = null
)

@Serializable
data class StreamResponse(
    val streams: List<StreamSource> = emptyList()
)

@Serializable
data class StreamSource(
    val url: String,
    val quality: String? = null,
    val type: String? = null
)
