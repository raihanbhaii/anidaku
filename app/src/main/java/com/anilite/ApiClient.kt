package com.anilite

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    private const val BASE_URL = "https://miruro-api-rho.vercel.app"

    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun getTrending(): AnimeResponse = client.get("$BASE_URL/trending").body()
    suspend fun searchAnime(query: String): AnimeResponse = client.get("$BASE_URL/search?query=$query").body()
    suspend fun getAnimeInfo(id: String): AnimeInfo = client.get("$BASE_URL/info/$id").body()
    suspend fun getEpisodes(id: String): EpisodeResponse = client.get("$BASE_URL/episodes/$id").body()
    suspend fun getStream(episodeId: String): StreamResponse {
        // The episodeId from API is like "watch/kiwi/178005/sub/animepahe-1"
        return client.get("$BASE_URL/$episodeId").body()
    }
}
