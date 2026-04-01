package com.anilite.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

object AniwatchRepository {

    private const val BASE_URL = "https://anidexz-api.vercel.app/aniwatch"

    // Home Page
    suspend fun getHome(): HomeResponse {
        return httpClient.get("$BASE_URL/").body()
    }

    // Search Anime
    suspend fun searchAnime(query: String): SearchResponse {
        val formattedQuery = query.trim().replace(" ", "+")
        return httpClient.get("$BASE_URL/search?keyword=$formattedQuery").body()
    }

    // Anime Detail (Important for Detail Screen)
    suspend fun getAnimeDetail(id: String): AnimeDetailResponse {
        return httpClient.get("$BASE_URL/anime/$id").body()
    }

    // Episodes List
    suspend fun getEpisodes(id: String): EpisodesResponse {
        return httpClient.get("$BASE_URL/episodes/$id").body()
    }
}

// ==================== Add these data classes in AniwatchAnime.kt ====================

// Update your existing AniwatchAnime.kt with these

data class HomeResponse(
    val spotLightAnimes: List<AniwatchAnime> = emptyList(),   // Note: spotLightAnimes (with capital L)
    val trendingAnimes: List<AniwatchAnime> = emptyList(),
    val latestEpisodes: List<AniwatchAnime> = emptyList(),
    val topUpcomingAnimes: List<AniwatchAnime> = emptyList(),
    val featuredAnimes: FeaturedAnimes? = null,
    val top10Animes: Top10Animes? = null,
    val genres: List<String> = emptyList()
)

data class FeaturedAnimes(
    val topAiringAnimes: List<AniwatchAnime> = emptyList(),
    val mostPopularAnimes: List<AniwatchAnime> = emptyList(),
    val mostFavoriteAnimes: List<AniwatchAnime> = emptyList(),
    val latestCompletedAnimes: List<AniwatchAnime> = emptyList()
)

data class Top10Animes(
    val day: List<AniwatchAnime> = emptyList(),
    val week: List<AniwatchAnime> = emptyList(),
    val month: List<AniwatchAnime> = emptyList()
)

data class AniwatchAnime(
    val id: String,
    val name: String,
    val img: String,
    val episodes: Episodes? = null,
    val duration: String? = null,
    val quality: String? = null,
    val category: String? = null,
    val releasedDay: String? = null,
    val description: String? = null,
    val rated: Boolean? = null,
    val rank: Int? = null
)

data class Episodes(
    val eps: Int? = null,
    val sub: Int? = null,
    val dub: Int? = null
)

// Search Response
data class SearchResponse(
    val animes: List<AniwatchAnime> = emptyList(),
    val mostPopularAnimes: List<AniwatchAnime> = emptyList(),
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val totalPages: Int = 1,
    val genres: List<String> = emptyList()
)

// Anime Detail Response
data class AnimeDetailResponse(
    val info: AniwatchAnimeInfo,
    val moreInfo: Map<String, Any?>? = null,
    val seasons: List<Season>? = null,
    val relatedAnimes: List<AniwatchAnime>? = null,
    val recommendedAnimes: List<AniwatchAnime>? = null
)

data class AniwatchAnimeInfo(
    val id: String,
    val name: String,
    val img: String,
    val rating: String? = null,
    val episodes: Episodes? = null,
    val category: String? = null,
    val quality: String? = null,
    val duration: String? = null,
    val description: String? = null,
    val mal_id: Int? = null,
    val al_id: Int? = null
)

data class Season(
    val id: String,
    val name: String,
    val seasonTitle: String? = null,
    val img: String? = null,
    val isCurrent: Boolean = false
)

// Episodes Response
data class EpisodesResponse(
    val totalEpisodes: Int,
    val episodes: List<Episode>
)

data class Episode(
    val name: String,
    val episodeNo: Int,
    val episodeId: String,   // Important: this contains "slug?ep=12345"
    val filler: Boolean = false
)
