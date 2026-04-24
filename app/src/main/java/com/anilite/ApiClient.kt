package com.anilite

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Data Classes ──────────────────────────────────────────────────────────────

@Serializable
data class SuggestionsResponse(
    val suggestions: List<Suggestion> = emptyList()
)

@Serializable
data class Suggestion(
    val id: Int = 0,
    val name: String = "",
    val image: String? = null,
    val type: String? = null
)

@Serializable
data class CharactersResponse(
    val characters: List<Character> = emptyList()
)

@Serializable
data class Character(
    val id: Int = 0,
    val name: String = "",
    val image: String? = null,
    val role: String? = null,
    val voiceActors: List<VoiceActor> = emptyList()
)

@Serializable
data class VoiceActor(
    val id: Int = 0,
    val name: String = "",
    val image: String? = null,
    val language: String? = null
)

// ── API Client ────────────────────────────────────────────────────────────────

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
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis  = 15_000
        }
        defaultRequest {
            header("Accept", "application/json")
        }
    }

    // ── Collections ────────────────────────────────────────────────────────────
    suspend fun getTrending(page: Int = 1, perPage: Int = 20): AnimeResponse =
        client.get("$BASE_URL/trending") {
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()

    suspend fun getPopular(page: Int = 1, perPage: Int = 20): AnimeResponse =
        client.get("$BASE_URL/popular") {
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()

    suspend fun getUpcoming(page: Int = 1, perPage: Int = 20): AnimeResponse =
        client.get("$BASE_URL/upcoming") {
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()

    suspend fun getRecent(page: Int = 1, perPage: Int = 20): AnimeResponse =
        client.get("$BASE_URL/recent") {
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()

    suspend fun getSpotlight(): AnimeResponse =
        client.get("$BASE_URL/spotlight").body()

    suspend fun getSchedule(page: Int = 1): AnimeResponse =
        client.get("$BASE_URL/schedule") {
            parameter("page", page)
        }.body()

    // ── Search ─────────────────────────────────────────────────────────────────
    suspend fun searchAnime(query: String, page: Int = 1, perPage: Int = 20): AnimeResponse =
        client.get("$BASE_URL/search") {
            parameter("query", query)
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()

    suspend fun getSuggestions(query: String): SuggestionsResponse =
        client.get("$BASE_URL/suggestions") {
            parameter("query", query)
        }.body()

    // ── Filter ─────────────────────────────────────────────────────────────────
    suspend fun filterAnime(
        genre: String? = null,
        format: String? = null,
        status: String? = null,
        season: String? = null,
        year: Int? = null,
        sort: String? = "POPULARITY_DESC",
        page: Int = 1,
        perPage: Int = 20
    ): AnimeResponse = client.get("$BASE_URL/filter") {
        genre?.let  { parameter("genre", it) }
        format?.let { parameter("format", it) }
        status?.let { parameter("status", it) }
        season?.let { parameter("season", it) }
        year?.let   { parameter("year", it) }
        sort?.let   { parameter("sort", it) }
        parameter("page", page)
        parameter("per_page", perPage)
    }.body()

    // ── Details ────────────────────────────────────────────────────────────────
    suspend fun getAnimeInfo(id: Int): AnimeInfo =
        client.get("$BASE_URL/info/$id").body()

    suspend fun getEpisodes(id: Int): EpisodeResponse =
        client.get("$BASE_URL/episodes/$id").body()

    suspend fun getCharacters(id: Int, page: Int = 1): CharactersResponse =
        client.get("$BASE_URL/anime/$id/characters") {
            parameter("page", page)
        }.body()

    suspend fun getRecommendations(id: Int): AnimeResponse =
        client.get("$BASE_URL/anime/$id/recommendations").body()

    // ── Streaming ──────────────────────────────────────────────────────────────
    /**
     * episodeId is already formatted like "watch/kiwi/178005/sub/animepahe-1"
     * so we just append it to the base URL.
     */
    suspend fun getStream(episodeId: String): StreamResponse =
        client.get("$BASE_URL/$episodeId").body()
}
