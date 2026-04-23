package com.anilite

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
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
                prettyPrint = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
        defaultRequest {
            header("Accept", "application/json")
        }
    }

    // Collections
    suspend fun getTrending(page: Int = 1): AnimeResponse = 
        client.get("$BASE_URL/trending") { parameter("page", page) }.body()

    suspend fun getPopular(page: Int = 1): AnimeResponse = 
        client.get("$BASE_URL/popular") { parameter("page", page) }.body()

    suspend fun getUpcoming(page: Int = 1): AnimeResponse = 
        client.get("$BASE_URL/upcoming") { parameter("page", page) }.body()

    suspend fun getRecent(page: Int = 1): AnimeResponse = 
        client.get("$BASE_URL/recent") { parameter("page", page) }.body()

    suspend fun getSpotlight(): AnimeResponse = 
        client.get("$BASE_URL/spotlight").body()

    // Search
    suspend fun searchAnime(query: String, page: Int = 1): AnimeResponse = 
        client.get("$BASE_URL/search") {
            parameter("query", query)
            parameter("page", page)
        }.body()

    // Details
    suspend fun getAnimeInfo(id: Int): AnimeInfo = 
        client.get("$BASE_URL/info/$id").body()

    suspend fun getEpisodes(id: Int): EpisodeResponse = 
        client.get("$BASE_URL/episodes/$id").body()

    // Streaming
    suspend fun getStream(episodeId: String): StreamResponse {
        // episodeId is already formatted like "watch/arc/21/sub/animekai-1"
        return client.get("$BASE_URL/$episodeId").body()
    }
}
