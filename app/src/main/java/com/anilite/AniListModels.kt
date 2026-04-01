package com.anilite.data

data class AniListAnime(
    val id: Int,
    val malId: Int? = null,
    val title: String,
    val titleEnglish: String? = null,
    val coverImage: String,
    val bannerImage: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val status: String? = null,       // FINISHED, RELEASING, NOT_YET_RELEASED
    val format: String? = null,       // TV, MOVIE, OVA, etc.
    val episodes: Int? = null,        // total planned episodes
    val duration: Int? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val studios: List<String> = emptyList(),
    val nextAiringEpisode: NextAiring? = null,  // null if not airing
    val startDate: FuzzyDate? = null,
    val endDate: FuzzyDate? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val isAdult: Boolean = false
)

data class NextAiring(
    val episode: Int,           // next episode number
    val airingAt: Long          // unix timestamp
)

data class FuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
) {
    override fun toString(): String {
        if (year == null) return "?"
        return listOfNotNull(
            month?.toString()?.padStart(2, '0'),
            day?.toString()?.padStart(2, '0'),
            year.toString()
        ).joinToString("/")
    }
}

data class AniListSearchResult(
    val animes: List<AniListAnime>,
    val hasNextPage: Boolean,
    val currentPage: Int,
    val totalPages: Int
)

// Episodes from your old API (unchanged)
// Uses existing Episode + EpisodesResponse data classes
