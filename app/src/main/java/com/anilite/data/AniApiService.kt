package com.anilite.data

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

object AniApiService {

    private const val BASE_URL = "https://anidexz-api.vercel.app/"

    // Home
    suspend fun getHome(): HomeResponse {
        return NetworkClient.client.get("${BASE_URL}aniwatch/").body()
    }

    // Search
    suspend fun searchAnime(query: String, page: Int = 1): SearchResponse {
        return NetworkClient.client.get("${BASE_URL}aniwatch/search") {
            parameter("keyword", query)
            parameter("page", page)
        }.body()
    }

    // Anime Detail
    suspend fun getAnimeDetail(id: String): AnimeDetailResponse {
        return NetworkClient.client.get("${BASE_URL}aniwatch/anime/$id").body()
    }

    // Episodes
    suspend fun getEpisodes(id: String): EpisodesResponse {
        return NetworkClient.client.get("${BASE_URL}aniwatch/episodes/$id").body()
    }

    // Servers
    suspend fun getServers(episodeId: String): ServersResponse {
        return NetworkClient.client.get("${BASE_URL}aniwatch/servers") {
            parameter("id", episodeId)
        }.body()
    }

    // Sources
    suspend fun getSources(
        episodeId: String,
        server: String = "vidstreaming",
        category: String = "sub"
    ): SourcesResponse {
        return NetworkClient.client.get("${BASE_URL}aniwatch/episode-srcs") {
            parameter("id", episodeId)
            parameter("server", server)
            parameter("category", category)
        }.body()
    }

    // Category
    suspend fun getCategory(category: String, page: Int = 1): SearchResponse {
        return NetworkClient.client.get("${BASE_URL}aniwatch/$category") {
            parameter("page", page)
        }.body()
    }

    // AZ List
    suspend fun getAzList(page: Int = 1): List<RelatedAnime> {
        return NetworkClient.client.get("${BASE_URL}aniwatch/az-list") {
            parameter("page", page)
        }.body()
    }
}
