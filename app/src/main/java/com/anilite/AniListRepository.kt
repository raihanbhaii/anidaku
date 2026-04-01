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

    suspend fun getDetail(animeId: Int): AniListAnime? = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Media(id: $animeId, type: ANIME) {
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
                        description
                        genres
                        averageScore
                        status
                        format
                        episodes
                        duration
                        season
                        seasonYear
                        studios {
                            nodes {
                                name
                            }
                        }
                        nextAiringEpisode {
                            episode
                            airingAt
                        }
                        startDate {
                            year
                            month
                            day
                        }
                        endDate {
                            year
                            month
                            day
                        }
                        popularity
                        favourites
                        isAdult
                    }
                }
            """
            
            val response = AniListClient.query(query)
            val media = response.getJSONObject("Media")
            parseAnimeDetail(media)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching anime detail", e)
            null
        }
    }

    suspend fun searchAnime(searchQuery: String): AniListSearchResult = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
                        pageInfo {
                            hasNextPage
                            currentPage
                            totalPages
                        }
                        media(search: "$searchQuery", type: ANIME, isAdult: false) {
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
            Log.e(TAG, "Error searching anime", e)
            AniListSearchResult(emptyList(), false, 1, 1)
        }
    }

    suspend fun getEpisodes(animeId: Int): List<Episode> = withContext(Dispatchers.IO) {
        try {
            // For now, return empty list
            // You can implement this with a separate API if needed
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching episodes", e)
            emptyList()
        }
    }

    private fun parseAnimeSearchResult(data: JSONObject): AniListSearchResult {
        val animes = mutableListOf<AniListAnime>()
        val page = data.getJSONObject("Page")
        val mediaArray = page.getJSONArray("media")
        
        for (i in 0 until mediaArray.length()) {
            val media = mediaArray.getJSONObject(i)
            val anime = parseAnimeFromJson(media)
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

    private fun parseAnimeFromJson(media: JSONObject): AniListAnime {
        val titleObj = media.getJSONObject("title")
        val title = when {
            titleObj.has("userPreferred") && !titleObj.isNull("userPreferred") -> 
                titleObj.getString("userPreferred")
            titleObj.has("english") && !titleObj.isNull("english") -> 
                titleObj.getString("english")
            else -> titleObj.getString("romaji")
        }
        
        val coverObj = media.getJSONObject("coverImage")
        val coverImage = when {
            coverObj.has("extraLarge") && !coverObj.isNull("extraLarge") -> 
                coverObj.getString("extraLarge")
            else -> coverObj.getString("large")
        }
        
        val bannerImage = if (media.has("bannerImage") && !media.isNull("bannerImage")) {
            media.getString("bannerImage")
        } else null
        
        var nextAiring: NextAiring? = null
        if (media.has("nextAiringEpisode") && !media.isNull("nextAiringEpisode")) {
            val nextObj = media.getJSONObject("nextAiringEpisode")
            nextAiring = NextAiring(
                episode = nextObj.getInt("episode"),
                airingAt = nextObj.getLong("airingAt")
            )
        }
        
        return AniListAnime(
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
    }

    private fun parseAnimeDetail(media: JSONObject): AniListAnime {
        val titleObj = media.getJSONObject("title")
        val title = when {
            titleObj.has("userPreferred") && !titleObj.isNull("userPreferred") -> 
                titleObj.getString("userPreferred")
            titleObj.has("english") && !titleObj.isNull("english") -> 
                titleObj.getString("english")
            else -> titleObj.getString("romaji")
        }
        
        val coverObj = media.getJSONObject("coverImage")
        val coverImage = when {
            coverObj.has("extraLarge") && !coverObj.isNull("extraLarge") -> 
                coverObj.getString("extraLarge")
            else -> coverObj.getString("large")
        }
        
        val bannerImage = if (media.has("bannerImage") && !media.isNull("bannerImage")) {
            media.getString("bannerImage")
        } else null
        
        val description = if (media.has("description") && !media.isNull("description")) {
            media.getString("description")
        } else null
        
        val genres = mutableListOf<String>()
        if (media.has("genres") && !media.isNull("genres")) {
            val genresArray = media.getJSONArray("genres")
            for (i in 0 until genresArray.length()) {
                genres.add(genresArray.getString(i))
            }
        }
        
        val averageScore = if (media.has("averageScore") && !media.isNull("averageScore")) {
            media.getInt("averageScore")
        } else null
        
        val studios = mutableListOf<String>()
        if (media.has("studios") && !media.isNull("studios")) {
            val studiosObj = media.getJSONObject("studios")
            if (studiosObj.has("nodes") && !studiosObj.isNull("nodes")) {
                val nodesArray = studiosObj.getJSONArray("nodes")
                for (i in 0 until nodesArray.length()) {
                    val node = nodesArray.getJSONObject(i)
                    studios.add(node.getString("name"))
                }
            }
        }
        
        var nextAiring: NextAiring? = null
        if (media.has("nextAiringEpisode") && !media.isNull("nextAiringEpisode")) {
            val nextObj = media.getJSONObject("nextAiringEpisode")
            nextAiring = NextAiring(
                episode = nextObj.getInt("episode"),
                airingAt = nextObj.getLong("airingAt")
            )
        }
        
        var startDate: FuzzyDate? = null
        if (media.has("startDate") && !media.isNull("startDate")) {
            val startObj = media.getJSONObject("startDate")
            startDate = FuzzyDate(
                year = if (startObj.has("year") && !startObj.isNull("year")) startObj.getInt("year") else null,
                month = if (startObj.has("month") && !startObj.isNull("month")) startObj.getInt("month") else null,
                day = if (startObj.has("day") && !startObj.isNull("day")) startObj.getInt("day") else null
            )
        }
        
        var endDate: FuzzyDate? = null
        if (media.has("endDate") && !media.isNull("endDate")) {
            val endObj = media.getJSONObject("endDate")
            endDate = FuzzyDate(
                year = if (endObj.has("year") && !endObj.isNull("year")) endObj.getInt("year") else null,
                month = if (endObj.has("month") && !endObj.isNull("month")) endObj.getInt("month") else null,
                day = if (endObj.has("day") && !endObj.isNull("day")) endObj.getInt("day") else null
            )
        }
        
        return AniListAnime(
            id = media.getInt("id"),
            title = title,
            coverImage = coverImage,
            bannerImage = bannerImage,
            description = description,
            genres = genres,
            averageScore = averageScore,
            status = if (media.has("status") && !media.isNull("status")) media.getString("status") else null,
            format = if (media.has("format") && !media.isNull("format")) media.getString("format") else null,
            episodes = if (media.has("episodes") && !media.isNull("episodes")) media.getInt("episodes") else null,
            duration = if (media.has("duration") && !media.isNull("duration")) media.getInt("duration") else null,
            season = if (media.has("season") && !media.isNull("season")) media.getString("season") else null,
            seasonYear = if (media.has("seasonYear") && !media.isNull("seasonYear")) media.getInt("seasonYear") else null,
            studios = studios,
            nextAiringEpisode = nextAiring,
            startDate = startDate,
            endDate = endDate,
            popularity = if (media.has("popularity") && !media.isNull("popularity")) media.getInt("popularity") else null,
            favourites = if (media.has("favourites") && !media.isNull("favourites")) media.getInt("favourites") else null,
            isAdult = if (media.has("isAdult")) media.getBoolean("isAdult") else false
        )
    }
}
