package com.anilite.data

import io.ktor.client.call.*
import io.ktor.client.request.*

object AniwatchRepository {

    private const val BASE_URL = "https://anidexz-api.vercel.app/aniwatch"

    // Home
    suspend fun getHome(): HomeResponse {
        return httpClient.get("$BASE_URL/").body()
    }

    // Search
    suspend fun searchAnime(query: String): SearchResponse {
        val formatted = query.trim().replace(" ", "+")
        return httpClient.get("$BASE_URL/search?keyword=$formatted").body()
    }

    // Anime Detail
    suspend fun getAnimeDetail(id: String): AnimeDetailResponse {
        return httpClient.get("$BASE_URL/anime/$id").body()
    }

    // Episodes
    suspend fun getEpisodes(id: String): EpisodesResponse {
        return httpClient.get("$BASE_URL/episodes/$id").body()
    }
}
