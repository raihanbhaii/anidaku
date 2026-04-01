package com.anilite.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AniListRepository {
    private const val TAG = "AniListRepository"

    suspend fun getTrending(): AniListSearchResult = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
                        pageInfo {
                            hasNextPage
                            currentPage
                            totalPages
                        }
                        media(sort: TRENDING_DESC, type: ANIME, isAdult: false) {
                            id
                            title {
                                romaji
                                english
                                userPreferred
                            }
                            coverImage {
                                large
                                extraLarge
                            }
                            bannerImage
                            format
                            status
                            duration
                            episodes
                            nextAiringEpisode {
                                episode
                                airingAt
                            }
                            isAdult
                        }
                    }
                }
            """
            
            val response = AniListClient.query(query)
            parseAnimeSearchResult(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trending", e)
            AniListSearchResult(emptyList(), false, 1, 1)
        }
    }

    suspend fun getAiring(): AniListSearchResult = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
                        pageInfo {
                            hasNextPage
                            currentPage
                            totalPages
                        }
                        media(sort: POPULARITY_DESC, type: ANIME, isAdult: false, status: RELEASING) {
                            id
                            title {
                                romaji
                                english
                                userPreferred
                            }
                            coverImage {
                                large
                                extraLarge
                            }
                            bannerImage
                            format
                            status
                            duration
                            episodes
                            nextAiringEpisode {
                                episode
                                airingAt
                            }
                            isAdult
                        }
                    }
                }
            """
            
            val response = AniListClient.query(query)
            parseAnimeSearchResult(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching airing", e)
            AniListSearchResult(emptyList(), false, 1, 1)
        }
    }

    suspend fun getPopular(): AniListSearchResult = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
                        pageInfo {
                            hasNextPage
                            currentPage
                            totalPages
                        }
                        media(sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                            id
                            title {
                                romaji
                                english
                                userPreferred
                            }
                            coverImage {
                                large
                                extraLarge
                            }
                            bannerImage
                            format
                            status
                            duration
                            episodes
                            nextAiringEpisode {
                                episode
                                airingAt
                            }
                            isAdult
                        }
                    }
                }
            """
            
            val response = AniListClient.query(query)
            parseAnimeSearchResult(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching popular", e)
            AniListSearchResult(emptyList(), false, 1, 1)
        }
    }

    suspend fun getUpcoming(): AniListSearchResult = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
                        pageInfo {
                            hasNextPage
                            currentPage
                            totalPages
                        }
                        media(sort: POPULARITY_DESC, type: ANIME, isAdult: false, status: NOT_YET_RELEASED) {
                            id
                            title {
                                romaji
                                english
                                userPreferred
                            }
                            coverImage {
                                large
                                extraLarge
                            }
                            bannerImage
                            format
                            status
                            duration
                            episodes
                            nextAiringEpisode {
                                episode
                                airingAt
                            }
                            isAdult
                        }
                    }
                }
            """
            
            val response = AniListClient.query(query)
            parseAnimeSearchResult(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching upcoming", e)
            AniListSearchResult(emptyList(), false, 1, 1)
        }
    }

    private fun parseAnimeSearchResult(data: JSONObject): AniListSearchResult {
        val animes = mutableListOf<AniListAnime>()
        val page = data.getJSONObject("Page")
        val mediaArray = page.getJSONArray("media")
        
        for (i in 0 until mediaArray.length()) {
            val media = mediaArray.getJSONObject(i)
            val titleObj = media.getJSONObject("title")
            
            // Get title (prefer userPreferred, then english, then romaji)
            val title = when {
                titleObj.has("userPreferred") && !titleObj.isNull("userPreferred") -> 
                    titleObj.getString("userPreferred")
                titleObj.has("english") && !titleObj.isNull("english") -> 
                    titleObj.getString("english")
                else -> titleObj.getString("romaji")
            }
            
            // Get cover image
            val coverObj = media.getJSONObject("coverImage")
            val coverImage = when {
                coverObj.has("extraLarge") && !coverObj.isNull("extraLarge") -> 
                    coverObj.getString("extraLarge")
                else -> coverObj.getString("large")
            }
            
            // Get banner image (optional)
            val bannerImage = if (media.has("bannerImage") && !media.isNull("bannerImage")) {
                media.getString("bannerImage")
            } else null
            
            // Get next airing episode
            var nextAiring: NextAiring? = null
            if (media.has("nextAiringEpisode") && !media.isNull("nextAiringEpisode")) {
                val nextObj = media.getJSONObject("nextAiringEpisode")
                nextAiring = NextAiring(
                    episode = nextObj.getInt("episode"),
                    airingAt = nextObj.getLong("airingAt")
                )
            }
            
            val anime = AniListAnime(
                id = media.getInt("id"),
                title = title,
                coverImage = coverImage,
                bannerImage = bannerImage,
                format = if (media.has("format") && !media.isNull("format")) media.getString("format") else null,
                status = if (media.has("status") && !media.isNull("status")) media.getString("status") else null,
                duration = if (media.has("duration") && !media.isNull("duration")) media.getInt("duration") else null,
                episodes = if (media.has("episodes") && !media.isNull("episodes")) media.getInt("episodes") else null,
                nextAiringEpisode = nextAiring,
                isAdult = if (media.has("isAdult")) media.getBoolean("isAdult") else false
            )
            animes.add(anime)
        }
        
        val pageInfo = page.getJSONObject("pageInfo")
        return AniListSearchResult(
            animes = animes,
            hasNextPage = pageInfo.getBoolean("hasNextPage"),
            currentPage = pageInfo.getInt("currentPage"),
            totalPages = pageInfo.getInt("totalPages")
        )
    }
}
