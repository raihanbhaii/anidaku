package com.anilite.data

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
data class AniwatchAnime(
    val id: String? = null,
    val name: String? = null,
    val img: String? = null,
    val episodes: List<Any>? = null   // You can make this more specific later
)

class AniwatchRepository {

    private val client = NetworkClient.client
    private val baseUrl = "https://api.aniwatch.to"   // Change if your actual API base is different

    suspend fun searchAnime(query: String): List<AniwatchAnime> {
        return client.get("$baseUrl/search") {
            parameter("q", query)
        }.body()
    }

    suspend fun getAnimeDetails(animeId: String): AniwatchAnime {
        return client.get("$baseUrl/anime/$animeId").body()
    }

    // Add more functions as needed (e.g. getEpisodes, etc.)
    suspend fun getEpisodes(animeId: String): List<Any> {
        return client.get("$baseUrl/anime/$animeId/episodes").body()
    }
}
