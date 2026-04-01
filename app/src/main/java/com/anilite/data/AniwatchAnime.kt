package com.anilite.data

data class AniwatchAnime(
    val id: String,           // aniwatch slug like "one-piece-100"
    val name: String,
    val img: String,
    val episodes: Episodes? = null,
    val duration: String? = null,
    val quality: String? = null,
    val category: String? = null,
    val releasedDay: String? = null,
    val description: String? = null,
    val rated: Boolean? = null
)

data class Episodes(
    val eps: Int? = null,
    val sub: Int? = null,
    val dub: Int? = null
)

data class HomeResponse(
    val spotlightAnimes: List<AniwatchAnime> = emptyList(),
    val trendingAnimes: List<AniwatchAnime> = emptyList(),
    val latestEpisodes: List<AniwatchAnime> = emptyList(),
    val topUpcomingAnimes: List<AniwatchAnime> = emptyList(),
    val featuredAnimes: FeaturedAnimes? = null
)

data class FeaturedAnimes(
    val topAiringAnimes: List<AniwatchAnime> = emptyList(),
    val mostPopularAnimes: List<AniwatchAnime> = emptyList()
)
