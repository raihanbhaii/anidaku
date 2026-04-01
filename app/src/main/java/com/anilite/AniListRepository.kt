package com.anilite.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object AniListRepository {
    private const val TAG = "AniListRepository"
    private const val GRAPHQL_URL = "https://graphql.anilist.co"

    suspend fun getTrending(): AniListResponse? = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
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
                        }
                    }
                }
            """
            
            val response = executeGraphQLQuery(query)
            Log.d(TAG, "Trending response: ${response?.animes?.size ?: 0} items")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trending", e)
            null
        }
    }

    suspend fun getAiring(): AniListResponse? = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
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
                        }
                    }
                }
            """
            
            executeGraphQLQuery(query)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching airing", e)
            null
        }
    }

    suspend fun getPopular(): AniListResponse? = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
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
                        }
                    }
                }
            """
            
            executeGraphQLQuery(query)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching popular", e)
            null
        }
    }

    suspend fun getUpcoming(): AniListResponse? = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
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
                        }
                    }
                }
            """
            
            executeGraphQLQuery(query)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching upcoming", e)
            null
        }
    }

    private fun executeGraphQLQuery(query: String): AniListResponse? {
        val url = URL(GRAPHQL_URL)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val body = """{"query": "$query"}""".replace("\n", " ").replace("\"", "\\\"")
            connection.outputStream.use { 
                it.write(body.toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                Log.d(TAG, "Response: ${response.take(200)}...")
                return parseAniListResponse(response)
            } else {
                Log.e(TAG, "HTTP Error: $responseCode")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            return null
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAniListResponse(json: String): AniListResponse? {
        return try {
            // Simple JSON parsing - you should use a proper JSON library like kotlinx.serialization or Gson
            val animes = mutableListOf<AniListAnime>()
            
            // Extract media array from JSON
            val mediaStart = json.indexOf("\"media\":[")
            if (mediaStart != -1) {
                var pos = mediaStart + 8
                var bracketCount = 0
                var inString = false
                var escape = false
                val mediaJson = StringBuilder()
                
                while (pos < json.length) {
                    val char = json[pos]
                    if (!inString && char == '[') bracketCount++
                    if (!inString && char == ']') bracketCount--
                    
                    mediaJson.append(char)
                    
                    if (bracketCount == 0 && char == ']') {
                        break
                    }
                    
                    if (char == '"' && !escape) inString = !inString
                    escape = if (char == '\\' && !escape) true else false
                    pos++
                }
                
                // Parse each anime in the array
                val itemsJson = mediaJson.toString().removePrefix("[").removeSuffix("]").split("},{")
                for (item in itemsJson) {
                    val anime = parseAnimeJson("{$item}")
                    if (anime != null) {
                        animes.add(anime)
                    }
                }
            }
            
            if (animes.isNotEmpty()) {
                AniListResponse(animes)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
            null
        }
    }

    private fun parseAnimeJson(json: String): AniListAnime? {
        return try {
            val id = extractInt(json, "\"id\":")
            val title = extractTitle(json)
            val coverImage = extractString(json, "\"large\":\"") ?: extractString(json, "\"extraLarge\":\"")
            val bannerImage = extractString(json, "\"bannerImage\":\"")
            val format = extractString(json, "\"format\":\"")
            val status = extractString(json, "\"status\":\"")
            val duration = extractInt(json, "\"duration\":")
            val episodes = extractInt(json, "\"episodes\":")
            
            // Parse nextAiringEpisode if present
            var nextAiringEpisode: NextAiringEpisode? = null
            if (json.contains("\"nextAiringEpisode\":")) {
                val episode = extractInt(json, "\"episode\":", json.indexOf("\"nextAiringEpisode\""))
                val airingAt = extractLong(json, "\"airingAt\":", json.indexOf("\"nextAiringEpisode\""))
                if (episode != null && airingAt != null) {
                    nextAiringEpisode = NextAiringEpisode(episode, airingAt)
                }
            }
            
            if (id != null && title != null && coverImage != null) {
                AniListAnime(
                    id = id,
                    title = title,
                    coverImage = coverImage,
                    bannerImage = bannerImage,
                    format = format,
                    status = status,
                    duration = duration,
                    episodes = episodes,
                    nextAiringEpisode = nextAiringEpisode
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing anime", e)
            null
        }
    }

    private fun extractInt(json: String, key: String, startPos: Int = 0): Int? {
        val index = json.indexOf(key, startPos)
        if (index == -1) return null
        var pos = index + key.length
        val numStr = StringBuilder()
        while (pos < json.length && json[pos].isDigit()) {
            numStr.append(json[pos])
            pos++
        }
        return if (numStr.isNotEmpty()) numStr.toString().toIntOrNull() else null
    }

    private fun extractLong(json: String, key: String, startPos: Int = 0): Long? {
        val index = json.indexOf(key, startPos)
        if (index == -1) return null
        var pos = index + key.length
        val numStr = StringBuilder()
        while (pos < json.length && json[pos].isDigit()) {
            numStr.append(json[pos])
            pos++
        }
        return if (numStr.isNotEmpty()) numStr.toString().toLongOrNull() else null
    }

    private fun extractString(json: String, key: String): String? {
        val index = json.indexOf(key)
        if (index == -1) return null
        var pos = index + key.length
        val str = StringBuilder()
        var inString = true
        var escape = false
        
        while (pos < json.length && inString) {
            val char = json[pos]
            if (char == '"' && !escape) {
                inString = false
            } else {
                str.append(char)
            }
            escape = if (char == '\\' && !escape) true else false
            pos++
        }
        return if (str.isNotEmpty()) str.toString() else null
    }

    private fun extractTitle(json: String): String? {
        // Try to get userPreferred first, then english, then romaji
        var title = extractString(json, "\"userPreferred\":\"")
        if (title == null) title = extractString(json, "\"english\":\"")
        if (title == null) title = extractString(json, "\"romaji\":\"")
        return title
    }
}
