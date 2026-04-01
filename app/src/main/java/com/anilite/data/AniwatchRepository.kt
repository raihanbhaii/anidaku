package com.anilite.data

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Main anime model used for search results and basic details.
 * 'episodes' is kept flexible because the API response for episodes is often complex.
 */
@Serializable
data class AniwatchAnime(
    val id: String? = null,
    val name: String? = null,
    val img: String? = null,
    // Use JsonElement for truly dynamic/unknown structure (recommended)
    @Contextual
    val episodes: JsonElement? = null
    // Alternative (if you prefer List<Any>):
    // @Contextual
    // val episodes: List<Any>? = null
)

/**
 * Optional: More specific model for episodes if you know the structure later.
 * Example (uncomment and adjust based on actual API response):
 */
// @Serializable
// data class AnimeEpisode(
//     val episodeId: String? = null,
//     val title: String? = null,
//     val episodeNumber: Int? = null,
//     val url: String? = null
// )

class AniwatchRepository {

    private val client = NetworkClient.client
    private val baseUrl = "https://api.aniwatch.to"

    /**
     * Search for anime
     */
    suspend fun searchAnime(query: String): List<AniwatchAnime> {
        return client.get("$baseUrl/search") {
            parameter("q", query)
        }.body()
    }

    /**
     * Get anime details by ID
     */
    suspend fun getAnimeDetails(animeId: String): AniwatchAnime {
        return client.get("$baseUrl/anime/$animeId").body()
    }

    /**
     * Get episodes for an anime.
     * Returns List<Any> for maximum flexibility (or change to JsonElement if preferred).
     */
    suspend fun getEpisodes(animeId: String): List<Any> {
        return client.get("$baseUrl/anime/$animeId/episodes").body()
    }

    // You can add more functions here as needed
}
