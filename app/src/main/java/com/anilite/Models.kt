package com.anilite

import kotlinx.serialization.Serializable

@Serializable
data class AnimeResponse(
    val page: Int = 1,
    val perPage: Int = 20,
    val total: Int = 0,
    val hasNextPage: Boolean = false,
    val results: List<AnimeSummary> = emptyList()
)

@Serializable
data class AnimeSummary(
    val id: Int,
    val title: AnimeTitle,
    val coverImage: ImageSources? = null,
    val bannerImage: String? = null,
    val format: String? = null,
    val status: String? = null,
    val averageScore: Int? = null,
    val episodes: Int? = null,
    val season: String? = null,
    val seasonYear: Int? = null
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
data class ImageSources(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val color: String? = null
) {
    fun getBestImage(): String? = extraLarge ?: large ?: medium
}

@Serializable
data class AnimeInfo(
    val id: Int,
    val idMal: Int? = null,
    val title: AnimeTitle,
    val description: String? = null,
    val coverImage: ImageSources? = null,
    val bannerImage: String? = null,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val averageScore: Int? = null,
    val episodes: Int? = null,
    val format: String? = null,
    val duration: Int? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val studios: List<Studio>? = null,
    val nextAiringEpisode: NextAiringEpisode? = null,
    val trailer: Trailer? = null,
    val recommendations: List<Recommendation>? = null,
    val relations: List<Relation>? = null
)

@Serializable
data class Studio(
    val id: Int? = null,
    val name: String? = null,
    val isAnimationStudio: Boolean = false
)

@Serializable
data class NextAiringEpisode(
    val airingAt: Long? = null,
    val timeUntilAiring: Long? = null,
    val episode: Int? = null
)

@Serializable
data class Trailer(
    val id: String? = null,
    val site: String? = null,
    val thumbnail: String? = null
)

@Serializable
data class Recommendation(
    val id: Int,
    val title: AnimeTitle,
    val coverImage: ImageSources? = null,
    val rating: Int? = null
)

@Serializable
data class Relation(
    val id: Int,
    val relationType: String? = null,
    val title: AnimeTitle,
    val coverImage: ImageSources? = null,
    val status: String? = null
)

@Serializable
data class EpisodeResponse(
    val mappings: Mappings? = null,
    val providers: Map<String, ProviderEpisodes> = emptyMap()
)

@Serializable
data class Mappings(
    val id: Int? = null,
    val malId: Int? = null,
    val aniId: Int? = null
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
    val image: String? = null,
    val airDate: String? = null,
    val description: String? = null,
    val filler: Boolean = false
)

@Serializable
data class StreamResponse(
    val streams: List<StreamSource> = emptyList(),
    val subtitles: List<Subtitle>? = null,
    val intro: SkipTime? = null,
    val outro: SkipTime? = null
)

@Serializable
data class StreamSource(
    val url: String,
    val quality: String? = null,
    val type: String? = null
)

@Serializable
data class Subtitle(
    val file: String,
    val label: String? = null,
    val kind: String? = null
)

@Serializable
data class SkipTime(
    val start: Int,
    val end: Int
)
