package com.anilite.data

import io.ktor.client.call.*
import io.ktor.client.request.*

object AniwatchRepository {

    private const val BASE_URL = "https://anidexz-api.vercel.app/aniwatch"

    suspend fun getHome(): HomeResponse {
        return httpClient.get("$BASE_URL/").body()
    }

    suspend fun searchAnime(query: String): SearchResponse {
        val q = query.replace(" ", "+")
        return httpClient.get("$BASE_URL/search?keyword=$q").body()
    }

    suspend fun getAnimeDetail(id: String): AnimeDetailResponse {
        return httpClient.get("$BASE_URL/anime/$id").body()
    }

    suspend fun getEpisodes(id: String): EpisodesResponse {
        return httpClient.get("$BASE_URL/episodes/$id").body()
    }
}
