package com.anilite.data

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class AniwatchRepository {

    private val client = NetworkClient.client
    private val baseUrl = "https://api-anime-rouge.vercel.app/aniwatch"   // Correct base URL

    suspend fun searchAnime(query: String): List<AnimeItem> {
        return try {
            client.get("$baseUrl/search") {
                parameter("keyword", query)
            }.body<SearchResponse>().animes
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAnimeDetails(animeId: String): AnimeDetailResponse? {
        return try {
            client.get("$baseUrl/anime/$animeId").body()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getEpisodes(animeId: String): EpisodesResponse? {
        return try {
            client.get("$baseUrl/episodes/$animeId").body()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
