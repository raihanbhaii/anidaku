package com.anilite.data

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Flexible anime model to prevent crashes from unknown fields in the API response.
 */
@Serializable
data class AniwatchAnime(
    val id: String? = null,
    val name: String? = null,
    val img: String? = null,
    val poster: String? = null,      // Some APIs use "poster"
    val description: String? = null,
    val type: String? = null,
    val status: String? = null,
    val releaseDate: String? = null,
    val genres: List<String>? = null,

    // Most common crash source - episodes can be object, array, or complex structure
    @Contextual
    val episodes: JsonElement? = null
)

class AniwatchRepository {

    private val client = NetworkClient.client
    private val baseUrl = "https://api.aniwatch.to"

    /**
     * Search anime - returns empty list instead of crashing on error
     */
    suspend fun searchAnime(query: String): List<AniwatchAnime> {
        return try {
            client.get("$baseUrl/search") {
                parameter("q", query)
            }.body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get anime details - returns null on failure (you should handle this in UI)
     */
    suspend fun getAnimeDetails(animeId: String): AniwatchAnime? {
        return try {
            client.get("$baseUrl/anime/$animeId").body()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get episodes - returns empty list on failure
     */
    suspend fun getEpisodes(animeId: String): List<Any> {
        return try {
            client.get("$baseUrl/anime/$animeId/episodes").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
