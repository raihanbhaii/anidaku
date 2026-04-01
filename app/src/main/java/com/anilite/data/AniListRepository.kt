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
                        pageInfo { hasNextPage currentPage totalPages }
                        media(sort: TRENDING_DESC, type: ANIME, isAdult: false) {
                            id title { romaji english userPreferred }
                            coverImage { large extraLarge }
                            bannerImage format status duration episodes
                            nextAiringEpisode { episode airingAt }
                            isAdult
                        }
                    }
                }
            """
            parseAnimeSearchResult(AniListClient.query(query))
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
                        pageInfo { hasNextPage currentPage totalPages }
                        media(sort: POPULARITY_DESC, type: ANIME, isAdult: false, status: RELEASING) {
                            id title { romaji english userPreferred }
                            coverImage { large extraLarge }
                            bannerImage format status duration episodes
                            nextAiringEpisode { episode airingAt }
                            isAdult
                        }
                    }
                }
            """
            parseAnimeSearchResult(AniListClient.query(query))
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
                        pageInfo { hasNextPage currentPage totalPages }
                        media(sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                            id title { romaji english userPreferred }
                            coverImage { large extraLarge }
                            bannerImage format status duration episodes
                            nextAiringEpisode { episode airingAt }
                            isAdult
                        }
                    }
                }
            """
            parseAnimeSearchResult(AniListClient.query(query))
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
                        pageInfo { hasNextPage currentPage totalPages }
                        media(sort: POPULARITY_DESC, type: ANIME, isAdult: false, status: NOT_YET_RELEASED) {
                            id title { romaji english userPreferred }
                            coverImage { large extraLarge }
                            bannerImage format status duration episodes
                            nextAiringEpisode { episode airingAt }
                            isAdult
                        }
                    }
                }
            """
            parseAnimeSearchResult(AniListClient.query(query))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching upcoming", e)
            AniListSearchResult(emptyList(), false, 1, 1)
        }
    }

    suspend fun searchAnime(searchQuery: String): AniListSearchResult = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Page(page: 1, perPage: 20) {
                        pageInfo { hasNextPage currentPage totalPages }
                        media(search: "$searchQuery", type: ANIME, isAdult: false) {
                            id title { romaji english userPreferred }
                            coverImage { large extraLarge }
                            bannerImage format status duration episodes
                            nextAiringEpisode { episode airingAt }
                            isAdult
                        }
                    }
                }
            """
            parseAnimeSearchResult(AniListClient.query(query))
        } catch (e: Exception) {
            Log.e(TAG, "Error searching anime", e)
            AniListSearchResult(emptyList(), false, 1, 1)
        }
    }

    suspend fun getDetail(animeId: Int): AniListAnime? = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                    Media(id: $animeId, type: ANIME) {
                        id title { romaji english userPreferred }
                        coverImage { large extraLarge }
                        bannerImage description genres averageScore
                        status format episodes duration season seasonYear
                        studios { nodes { name } }
                        nextAiringEpisode { episode airingAt }
                        startDate { year month day }
                        endDate { year month day }
                        popularity favourites isAdult
                        characters { nodes { id name { full } image { large } description } }
                        relations { nodes { id title { userPreferred } coverImage { large } type format status } }
                    }
                }
            """
            val response = AniListClient.query(query)
            parseAnimeDetail(response.getJSONObject("Media"))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching anime detail", e)
            null
        }
    }

    private fun parseAnimeSearchResult(data: JSONObject): AniListSearchResult {
        val animes = mutableListOf<AniListAnime>()
        val page = data.getJSONObject("Page")
        val mediaArray = page.getJSONArray("media")
        for (i in 0 until mediaArray.length()) {
            animes.add(parseAnimeFromJson(mediaArray.getJSONObject(i)))
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
        val title = titleObj.optString("userPreferred").takeIf { it.isNotEmpty() }
            ?: titleObj.optString("english").takeIf { it.isNotEmpty() }
            ?: titleObj.getString("romaji")

        val coverObj = media.getJSONObject("coverImage")
        val coverImage = coverObj.optString("extraLarge").takeIf { it.isNotEmpty() }
            ?: coverObj.getString("large")

        val nextAiring = if (!media.isNull("nextAiringEpisode")) {
            val n = media.getJSONObject("nextAiringEpisode")
            NextAiring(episode = n.getInt("episode"), airingAt = n.getLong("airingAt"))
        } else null

        return AniListAnime(
            id = media.getInt("id"),
            title = title,
            coverImage = coverImage,
            bannerImage = media.optString("bannerImage").takeIf { it.isNotEmpty() },
            format = media.optString("format").takeIf { it.isNotEmpty() },
            status = media.optString("status").takeIf { it.isNotEmpty() },
            duration = if (!media.isNull("duration")) media.getInt("duration") else null,
            episodes = if (!media.isNull("episodes")) media.getInt("episodes") else null,
            nextAiringEpisode = nextAiring,
            isAdult = media.optBoolean("isAdult", false)
        )
    }

    private fun parseAnimeDetail(media: JSONObject): AniListAnime {
        val titleObj = media.getJSONObject("title")
        val title = titleObj.optString("userPreferred").takeIf { it.isNotEmpty() }
            ?: titleObj.optString("english").takeIf { it.isNotEmpty() }
            ?: titleObj.getString("romaji")
        val titleEnglish = titleObj.optString("english").takeIf { it.isNotEmpty() }

        val coverObj = media.getJSONObject("coverImage")
        val coverImage = coverObj.optString("extraLarge").takeIf { it.isNotEmpty() }
            ?: coverObj.getString("large")

        val genres = mutableListOf<String>()
        if (!media.isNull("genres")) {
            val arr = media.getJSONArray("genres")
            for (i in 0 until arr.length()) genres.add(arr.getString(i))
        }

        val studios = mutableListOf<String>()
        if (!media.isNull("studios")) {
            val nodes = media.getJSONObject("studios").optJSONArray("nodes")
            if (nodes != null) for (i in 0 until nodes.length()) studios.add(nodes.getJSONObject(i).getString("name"))
        }

        val nextAiring = if (!media.isNull("nextAiringEpisode")) {
            val n = media.getJSONObject("nextAiringEpisode")
            NextAiring(episode = n.getInt("episode"), airingAt = n.getLong("airingAt"))
        } else null

        fun parseFuzzyDate(key: String): FuzzyDate? {
            if (media.isNull(key)) return null
            val obj = media.getJSONObject(key)
            return FuzzyDate(
                year = if (!obj.isNull("year")) obj.getInt("year") else null,
                month = if (!obj.isNull("month")) obj.getInt("month") else null,
                day = if (!obj.isNull("day")) obj.getInt("day") else null
            )
        }

        val characters = mutableListOf<Character>()
        if (!media.isNull("characters")) {
            val nodes = media.getJSONObject("characters").optJSONArray("nodes")
            if (nodes != null) {
                for (i in 0 until nodes.length()) {
                    val node = nodes.getJSONObject(i)
                    characters.add(Character(
                        id = node.getInt("id"),
                        name = node.getJSONObject("name").optString("full", ""),
                        image = node.optJSONObject("image")?.optString("large")?.takeIf { it.isNotEmpty() },
                        description = node.optString("description").takeIf { it.isNotEmpty() }
                    ))
                }
            }
        }

        val relations = mutableListOf<AniListRelatedAnime>()
        if (!media.isNull("relations")) {
            val nodes = media.getJSONObject("relations").optJSONArray("nodes")
            if (nodes != null) {
                for (i in 0 until nodes.length()) {
                    val node = nodes.getJSONObject(i)
                    val t = node.optJSONObject("title")?.optString("userPreferred") ?: ""
                    val img = node.optJSONObject("coverImage")?.optString("large")?.takeIf { it.isNotEmpty() }
                    relations.add(AniListRelatedAnime(
                        id = node.getInt("id").toString(),
                        name = t,
                        img = img,
                        category = node.optString("type").takeIf { it.isNotEmpty() },
                        relationType = node.optString("relationType").takeIf { it.isNotEmpty() }
                    ))
                }
            }
        }

        return AniListAnime(
            id = media.getInt("id"),
            title = title,
            titleEnglish = titleEnglish,
            coverImage = coverImage,
            bannerImage = media.optString("bannerImage").takeIf { it.isNotEmpty() },
            description = media.optString("description").takeIf { it.isNotEmpty() },
            genres = genres,
            averageScore = if (!media.isNull("averageScore")) media.getInt("averageScore") else null,
            status = media.optString("status").takeIf { it.isNotEmpty() },
            format = media.optString("format").takeIf { it.isNotEmpty() },
            episodes = if (!media.isNull("episodes")) media.getInt("episodes") else null,
            duration = if (!media.isNull("duration")) media.getInt("duration") else null,
            season = media.optString("season").takeIf { it.isNotEmpty() },
            seasonYear = if (!media.isNull("seasonYear")) media.getInt("seasonYear") else null,
            studios = studios,
            nextAiringEpisode = nextAiring,
            startDate = parseFuzzyDate("startDate"),
            endDate = parseFuzzyDate("endDate"),
            popularity = if (!media.isNull("popularity")) media.getInt("popularity") else null,
            favourites = if (!media.isNull("favourites")) media.getInt("favourites") else null,
            isAdult = media.optBoolean("isAdult", false),
            characters = characters,
            voiceActors = emptyList(), // voiceActors removed from query (not directly on Media)
            relations = relations
        )
    }
}
