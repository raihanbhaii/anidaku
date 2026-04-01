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
                        characters {
                            nodes {
                                id
                                name {
                                    full
                                }
                                image {
                                    large
                                }
                                description
                            }
                        }
                        voiceActors {
                            nodes {
                                id
                                name {
                                    full
                                }
                                image {
                                    large
                                }
                                language
                            }
                        }
                        relations {
                            nodes {
                                id
                                title {
                                    userPreferred
                                }
                                coverImage {
                                    large
                                }
                                type
                                format
                                status
                            }
                        }
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
        
        // Parse characters
        val characters = mutableListOf<Character>()
        if (media.has("characters") && !media.isNull("characters")) {
            val charsObj = media.getJSONObject("characters")
            if (charsObj.has("nodes") && !charsObj.isNull("nodes")) {
                val nodesArray = charsObj.getJSONArray("nodes")
                for (i in 0 until nodesArray.length()) {
                    val node = nodesArray.getJSONObject(i)
                    val nameObj = node.getJSONObject("name")
                    val name = if (nameObj.has("full") && !nameObj.isNull("full")) {
                        nameObj.getString("full")
                    } else ""
                    
                    val image = if (node.has("image") && !node.isNull("image")) {
                        node.getJSONObject("image").getString("large")
                    } else null
                    
                    val description = if (node.has("description") && !node.isNull("description")) {
                        node.getString("description")
                    } else null
                    
                    characters.add(Character(
                        id = node.getInt("id"),
                        name = name,
                        image = image,
                        description = description
                    ))
                }
            }
        }
        
        // Parse voice actors
        val voiceActors = mutableListOf<VoiceActor>()
        if (media.has("voiceActors") && !media.isNull("voiceActors")) {
            val vasObj = media.getJSONObject("voiceActors")
            if (vasObj.has("nodes") && !vasObj.isNull("nodes")) {
                val nodesArray = vasObj.getJSONArray("nodes")
                for (i in 0 until nodesArray.length()) {
                    val node = nodesArray.getJSONObject(i)
                    val nameObj = node.getJSONObject("name")
                    val name = if (nameObj.has("full") && !nameObj.isNull("full")) {
                        nameObj.getString("full")
                    } else ""
                    
                    val image = if (node.has("image") && !node.isNull("image")) {
                        node.getJSONObject("image").getString("large")
                    } else null
                    
                    val language = if (node.has("language") && !node.isNull("language")) {
                        node.getString("language")
                    } else null
                    
                    voiceActors.add(VoiceActor(
                        id = node.getInt("id"),
                        name = name,
                        image = image,
                        language = language
                    ))
                }
            }
        }
        
        // Parse relations
        val relations = mutableListOf<RelatedAnime>()
        if (media.has("relations") && !media.isNull("relations")) {
            val relationsObj = media.getJSONObject("relations")
            if (relationsObj.has("nodes") && !relationsObj.isNull("nodes")) {
                val nodesArray = relationsObj.getJSONArray("nodes")
                for (i in 0 until nodesArray.length()) {
                    val node = nodesArray.getJSONObject(i)
                    val titleObj2 = node.getJSONObject("title")
                    val title2 = if (titleObj2.has("userPreferred") && !titleObj2.isNull("userPreferred")) {
                        titleObj2.getString("userPreferred")
                    } else ""
                    
                    val coverObj2 = if (node.has("coverImage") && !node.isNull("coverImage")) {
                        node.getJSONObject("coverImage")
                    } else null
                    
                    val coverImage2 = if (coverObj2 != null && coverObj2.has("large") && !coverObj2.isNull("large")) {
                        coverObj2.getString("large")
                    } else null
                    
                    relations.add(RelatedAnime(
                        id = node.getInt("id").toString(),
                        name = title2,
                        img = coverImage2,
                        category = if (node.has("type") && !node.isNull("type")) node.getString("type") else null,
                        relationType = if (node.has("relationType") && !node.isNull("relationType")) node.getString("relationType") else null
                    ))
                }
            }
        }
        
        return AniListAnime(
            id = media.getInt("id"),
            title = title,
            titleEnglish = if (titleObj.has("english") && !titleObj.isNull("english")) titleObj.getString("english") else null,
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
            isAdult = if (media.has("isAdult")) media.getBoolean("isAdult") else false,
            characters = characters,
            voiceActors = voiceActors,
            relations = relations
        )
    }
}
