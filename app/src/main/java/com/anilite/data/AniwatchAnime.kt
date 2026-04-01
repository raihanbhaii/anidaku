package com.anilite.data

// Main Anime model
data class AniwatchAnime(
    val id: String,
    val name: String,
    val img: String,
    val episodes: Episodes? = null,
    val duration: String? = null,
    val quality: String? = null,
    val category: String? = null,
    val description: String? = null
)

data class Episodes(
    val eps: Int? = null,
    val sub: Int? = null,
    val dub: Int? = null
)

// Home Response
data class HomeResponse(
    val spotlightAnimes: List<AniwatchAnime> = emptyList(),   // Important: lowercase s
    val trendingAnimes: List<AniwatchAnime> = emptyList(),
    val topUpcomingAnimes: List<AniwatchAnime> = emptyList(),
    val featuredAnimes: FeaturedAnimes? = null
)

data class FeaturedAnimes(
    val topAiringAnimes: List<AniwatchAnime> = emptyList(),
    val mostPopularAnimes: List<AniwatchAnime> = emptyList()
)

// Search Response
data class SearchResponse(
    val animes: List<AniwatchAnime> = emptyList()
)

// Detail Response
data class AnimeDetailResponse(
    val info: AniwatchAnimeInfo
)

data class AniwatchAnimeInfo(
    val id: String,
    val name: String,
    val img: String,
    val description: String? = null,
    val duration: String? = null,
    val quality: String? = null,
    val category: String? = null
)

// Episodes Response
data class EpisodesResponse(
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val name: String,
    val episodeNo: Int,
    val episodeId: String   // contains ?ep=XXXXX
)
