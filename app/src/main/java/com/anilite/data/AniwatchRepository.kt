package com.anilite.data

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class AniwatchRepository {
    private val client = NetworkClient.client
    private val baseUrl = "https://anidexz-api.vercel.app/aniwatch"

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
            val response = client.get("$baseUrl/anime/$animeId")
            val status = response.status.value
            if (status != 200) throw Exception("HTTP $status from detail API")
            response.body()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e  // re-throw so DetailScreen shows the real error
        }
    }

    suspend fun getEpisodes(animeId: String): EpisodesResponse? {
        return try {
            val response = client.get("$baseUrl/episodes/$animeId")
            val status = response.status.value
            if (status != 200) throw Exception("HTTP $status from episodes API")
            response.body()
        } catch (e: Exception) {
            e.printStackTrace()
            null  // episodes failing is non-fatal
        }
    }
}
