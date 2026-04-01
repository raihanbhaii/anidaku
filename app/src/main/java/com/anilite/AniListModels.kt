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
    val status: String? = null,
    val format: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val studios: List<String> = emptyList(),
    val nextAiringEpisode: NextAiring? = null,
    val startDate: FuzzyDate? = null,
    val endDate: FuzzyDate? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val isAdult: Boolean = false,
    val characters: List<Character> = emptyList(),
    val voiceActors: List<VoiceActor> = emptyList(),
    val relations: List<RelatedAnime> = emptyList()
)

data class NextAiring(
    val episode: Int,
    val airingAt: Long
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

data class Character(
    val id: Int,
    val name: String,
    val image: String? = null,
    val description: String? = null
)

data class VoiceActor(
    val id: Int,
    val name: String,
    val image: String? = null,
    val language: String? = null
)

data class RelatedAnime(
    val id: String,
    val name: String,
    val img: String? = null,
    val category: String? = null,
    val relationType: String? = null
)

data class AniListSearchResult(
    val animes: List<AniListAnime>,
    val hasNextPage: Boolean,
    val currentPage: Int,
    val totalPages: Int
)
