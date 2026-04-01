package com.anilite.data

import org.json.JSONObject

object AniListRepository {

    // ── Fragments ────────────────────────────────────────────────────────

    private val MEDIA_FIELDS = """
        id
        idMal
        title { romaji english }
        coverImage { extraLarge large }
        bannerImage
        description(asHtml: false)
        genres
        averageScore
        status
        format
        episodes
        duration
        season
        seasonYear
        studios(isMain: true) { nodes { name } }
        nextAiringEpisode { episode airingAt }
        startDate { year month day }
        endDate { year month day }
        popularity
        favourites
        isAdult
    """.trimIndent()

    // ── Home ─────────────────────────────────────────────────────────────

    suspend fun getTrending(page: Int = 1): AniListSearchResult {
        val query = """
            query (${'$'}page: Int) {
              Page(page: ${'$'}page, perPage: 20) {
                pageInfo { hasNextPage currentPage lastPage }
                media(sort: TRENDING_DESC, type: ANIME, isAdult: false) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()
        val data = AniListClient.query(query, mapOf("page" to page))
        return parseSearchResult(data.getJSONObject("Page"))
    }

    suspend fun getPopular(page: Int = 1): AniListSearchResult {
        val query = """
            query (${'$'}page: Int) {
              Page(page: ${'$'}page, perPage: 20) {
                pageInfo { hasNextPage currentPage lastPage }
                media(sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()
        val data = AniListClient.query(query, mapOf("page" to page))
        return parseSearchResult(data.getJSONObject("Page"))
    }

    suspend fun getAiring(page: Int = 1): AniListSearchResult {
        val query = """
            query (${'$'}page: Int) {
              Page(page: ${'$'}page, perPage: 20) {
                pageInfo { hasNextPage currentPage lastPage }
                media(status: RELEASING, sort: TRENDING_DESC, type: ANIME, isAdult: false) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()
        val data = AniListClient.query(query, mapOf("page" to page))
        return parseSearchResult(data.getJSONObject("Page"))
    }

    suspend fun getUpcoming(page: Int = 1): AniListSearchResult {
        val query = """
            query (${'$'}page: Int) {
              Page(page: ${'$'}page, perPage: 20) {
                pageInfo { hasNextPage currentPage lastPage }
                media(status: NOT_YET_RELEASED, sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()
        val data = AniListClient.query(query, mapOf("page" to page))
        return parseSearchResult(data.getJSONObject("Page"))
    }

    // ── Search ────────────────────────────────────────────────────────────

    suspend fun search(keyword: String, page: Int = 1): AniListSearchResult {
        val query = """
            query (${'$'}search: String, ${'$'}page: Int) {
              Page(page: ${'$'}page, perPage: 20) {
                pageInfo { hasNextPage currentPage lastPage }
                media(search: ${'$'}search, type: ANIME, isAdult: false) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()
        val data = AniListClient.query(query, mapOf("search" to keyword, "page" to page))
        return parseSearchResult(data.getJSONObject("Page"))
    }

    // ── Detail ────────────────────────────────────────────────────────────

    suspend fun getDetail(aniListId: Int): AniListAnime? {
        val query = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                $MEDIA_FIELDS
              }
            }
        """.trimIndent()
        val data = AniListClient.query(query, mapOf("id" to aniListId))
        return if (data.has("Media")) parseMedia(data.getJSONObject("Media")) else null
    }

    // ── Old API: episodes aired so far ────────────────────────────────────
    // Uses your existing RetrofitClient — only for episode list + aired count

    suspend fun getEpisodes(aniwatchId: String): EpisodesResponse {
        return try {
            RetrofitClient.api.getEpisodes(aniwatchId)
        } catch (e: Exception) {
            EpisodesResponse()
        }
    }

    // ── Old API: get al_id from anime detail ──────────────────────────────
    // We use this to get the AniList ID from an aniwatch anime slug

    suspend fun getAlId(aniwatchId: String): Int? {
        return try {
            val detail = RetrofitClient.api.getAnimeDetail(aniwatchId)
            val alId = detail.info?.alId ?: 0
            if (alId > 0) alId else null
        } catch (e: Exception) {
            null
        }
    }

    // ── Parsers ───────────────────────────────────────────────────────────

    private fun parseSearchResult(page: JSONObject): AniListSearchResult {
        val pageInfo = page.getJSONObject("pageInfo")
        val mediaArray = page.getJSONArray("media")
        val animes = (0 until mediaArray.length()).mapNotNull {
            try { parseMedia(mediaArray.getJSONObject(it)) } catch (e: Exception) { null }
        }
        return AniListSearchResult(
            animes = animes,
            hasNextPage = pageInfo.optBoolean("hasNextPage", false),
            currentPage = pageInfo.optInt("currentPage", 1),
            totalPages = pageInfo.optInt("lastPage", 1)
        )
    }

    fun parseMedia(m: JSONObject): AniListAnime {
        val title = m.getJSONObject("title")
        val coverImage = m.getJSONObject("coverImage")
        val studiosObj = m.optJSONObject("studios")
        val studios = studiosObj?.optJSONArray("nodes")?.let { nodes ->
            (0 until nodes.length()).map { nodes.getJSONObject(it).optString("name", "") }
        } ?: emptyList()

        val nextAiring = m.optJSONObject("nextAiringEpisode")?.let {
            NextAiring(
                episode = it.optInt("episode"),
                airingAt = it.optLong("airingAt")
            )
        }

        val startDateObj = m.optJSONObject("startDate")
        val endDateObj = m.optJSONObject("endDate")

        val genresArray = m.optJSONArray("genres")
        val genres = genresArray?.let {
            (0 until it.length()).map { i -> it.optString(i) }
        } ?: emptyList()

        return AniListAnime(
            id = m.optInt("id"),
            malId = m.optInt("idMal").takeIf { it > 0 },
            title = title.optString("english").takeIf { it.isNotEmpty() }
                ?: title.optString("romaji", "Unknown"),
            titleEnglish = title.optString("english").takeIf { it.isNotEmpty() },
            coverImage = coverImage.optString("extraLarge").takeIf { it.isNotEmpty() }
                ?: coverImage.optString("large", ""),
            bannerImage = m.optString("bannerImage").takeIf { it.isNotEmpty() },
            description = m.optString("description").takeIf { it.isNotEmpty() }
                ?.replace(Regex("<[^>]*>"), ""), // strip HTML tags
            genres = genres,
            averageScore = m.optInt("averageScore").takeIf { it > 0 },
            status = m.optString("status").takeIf { it.isNotEmpty() },
            format = m.optString("format").takeIf { it.isNotEmpty() },
            episodes = m.optInt("episodes").takeIf { it > 0 },
            duration = m.optInt("duration").takeIf { it > 0 },
            season = m.optString("season").takeIf { it.isNotEmpty() },
            seasonYear = m.optInt("seasonYear").takeIf { it > 0 },
            studios = studios,
            nextAiringEpisode = nextAiring,
            startDate = startDateObj?.let {
                FuzzyDate(
                    it.optInt("year").takeIf { y -> y > 0 },
                    it.optInt("month").takeIf { mo -> mo > 0 },
                    it.optInt("day").takeIf { d -> d > 0 }
                )
            },
            endDate = endDateObj?.let {
                FuzzyDate(
                    it.optInt("year").takeIf { y -> y > 0 },
                    it.optInt("month").takeIf { mo -> mo > 0 },
                    it.optInt("day").takeIf { d -> d > 0 }
                )
            },
            popularity = m.optInt("popularity").takeIf { it > 0 },
            favourites = m.optInt("favourites").takeIf { it > 0 },
            isAdult = m.optBoolean("isAdult", false)
        )
    }
}
