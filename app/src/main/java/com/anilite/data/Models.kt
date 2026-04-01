package com.anilite.data

import com.google.gson.annotations.SerializedName

// ── Shared ────────────────────────────────────────────────────────────────

data class EpisodeCount(
    @SerializedName("eps") val eps: Int? = null,
    @SerializedName("sub") val sub: Int? = null,
    @SerializedName("dub") val dub: Int? = null
)

// ── Home ──────────────────────────────────────────────────────────────────

data class HomeResponse(
    @SerializedName("spotLightAnimes") val spotlightAnimes: List<SpotlightAnime> = emptyList(),
    @SerializedName("trendingAnimes")     val trendingAnimes:     List<BasicAnime>     = emptyList(),
    @SerializedName("latestEpisodes")     val latestEpisodes:     List<AnimeItem>      = emptyList(),
    @SerializedName("top10Animes")        val top10Animes:        Top10Animes?         = null,
    @SerializedName("featuredAnimes")     val featuredAnimes:     FeaturedAnimes?      = null,
    @SerializedName("topUpcomingAnimes")  val topUpcomingAnimes:  List<AnimeItem>      = emptyList(),
    @SerializedName("genres")             val genres:             List<String>         = emptyList()
)

data class SpotlightAnime(
    @SerializedName("id")          val id: String          = "",
    @SerializedName("name")        val name: String        = "",
    @SerializedName("rank")        val rank: Int?          = null,
    @SerializedName("img")         val img: String         = "",
    @SerializedName("episodes")    val episodes: EpisodeCount? = null,
    @SerializedName("duration")    val duration: String    = "",
    @SerializedName("quality")     val quality: String?    = null,
    @SerializedName("category")    val category: String?   = null,
    @SerializedName("releasedDay") val releasedDay: String? = null,
    @SerializedName("description") val description: String? = null  // ← fixed: was "descriptions"
)

// Generic anime card used in latest episodes, upcoming, recommended, etc.
data class AnimeItem(
    @SerializedName("id")       val id: String          = "",
    @SerializedName("name")     val name: String        = "",
    @SerializedName("img")      val img: String         = "",
    @SerializedName("episodes") val episodes: EpisodeCount? = null,
    @SerializedName("duration") val duration: String?   = null,
    @SerializedName("rated")    val rated: Boolean      = false
)

// Minimal card (trending, featured sections)
data class BasicAnime(
    @SerializedName("id")   val id: String  = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("img")  val img: String  = ""
)

data class Top10Animes(
    @SerializedName("day")   val day:   List<Top10Anime> = emptyList(),
    @SerializedName("week")  val week:  List<Top10Anime> = emptyList(),
    @SerializedName("month") val month: List<Top10Anime> = emptyList()
)

data class Top10Anime(
    @SerializedName("id")       val id: String          = "",
    @SerializedName("name")     val name: String        = "",
    @SerializedName("rank")     val rank: Int           = 0,
    @SerializedName("img")      val img: String         = "",
    @SerializedName("episodes") val episodes: EpisodeCount? = null
)

data class FeaturedAnimes(
    @SerializedName("topAiringAnimes")       val topAiringAnimes:       List<BasicAnime> = emptyList(),
    @SerializedName("mostPopularAnimes")     val mostPopularAnimes:     List<BasicAnime> = emptyList(),
    @SerializedName("mostFavoriteAnimes")    val mostFavoriteAnimes:    List<BasicAnime> = emptyList(),
    @SerializedName("latestCompletedAnimes") val latestCompletedAnimes: List<BasicAnime> = emptyList()
)

// ── Search / Category ─────────────────────────────────────────────────────

data class SearchResponse(
    @SerializedName("animes")            val animes:           List<AnimeItem>     = emptyList(),
    @SerializedName("mostPopularAnimes") val mostPopularAnimes: List<RelatedAnime> = emptyList(),
    @SerializedName("top10Animes")       val top10Animes:      Top10Animes?        = null,
    @SerializedName("category")          val category:         String              = "",
    @SerializedName("genres")            val genres:           List<String>        = emptyList(),
    @SerializedName("currentPage")       val currentPage:      Int                 = 1,
    @SerializedName("hasNextPage")       val hasNextPage:      Boolean             = false,
    @SerializedName("totalPages")        val totalPages:       Int                 = 1
)

// ── Anime Detail ──────────────────────────────────────────────────────────

data class AnimeDetailResponse(
    @SerializedName("info")              val info:             AnimeInfo?          = null,
    @SerializedName("moreInfo")          val moreInfo:         AnimeMoreInfo?      = null,
    @SerializedName("seasons")           val seasons:          List<AnimeSeason>   = emptyList(),
    @SerializedName("relatedAnimes")     val relatedAnimes:    List<RelatedAnime>  = emptyList(),
    @SerializedName("recommendedAnimes") val recommendedAnimes: List<AnimeItem>    = emptyList(),
    @SerializedName("mostPopularAnimes") val mostPopularAnimes: List<RelatedAnime> = emptyList()
)

data class AnimeInfo(
    @SerializedName("id")          val id: String          = "",
    @SerializedName("anime_id")    val animeId: Int        = 0,
    @SerializedName("mal_id")      val malId: Int          = 0,
    @SerializedName("al_id")       val alId: Int           = 0,
    @SerializedName("name")        val name: String        = "",
    @SerializedName("img")         val img: String         = "",
    @SerializedName("rating")      val rating: String      = "",
    @SerializedName("episodes")    val episodes: EpisodeCount? = null,
    @SerializedName("category")    val category: String    = "",
    @SerializedName("quality")     val quality: String     = "",
    @SerializedName("duration")    val duration: String    = "",
    @SerializedName("description") val description: String = ""
)

data class AnimeMoreInfo(
    @SerializedName("Japanese:")  val japanese: String?       = null,
    @SerializedName("Synonyms:")  val synonyms: String?       = null,
    @SerializedName("Aired:")     val aired: String?          = null,
    @SerializedName("Premiered:") val premiered: String?      = null,
    @SerializedName("Duration:")  val duration: String?       = null,
    @SerializedName("Status:")    val status: String?         = null,
    @SerializedName("MAL Score:") val malScore: String?       = null,
    @SerializedName("Studios:")   val studios: String?        = null,  // ← fixed: was List<String>
    @SerializedName("Genres")     val genres: List<String>    = emptyList(),
    @SerializedName("Producers")  val producers: List<String> = emptyList()
)

data class AnimeSeason(
    @SerializedName("id")          val id: String         = "",
    @SerializedName("name")        val name: String       = "",
    @SerializedName("seasonTitle") val seasonTitle: String = "",
    @SerializedName("img")         val img: String        = "",
    @SerializedName("isCurrent")   val isCurrent: Boolean = false
)

data class RelatedAnime(
    @SerializedName("id")       val id: String          = "",
    @SerializedName("name")     val name: String        = "",
    @SerializedName("category") val category: String    = "",
    @SerializedName("img")      val img: String         = "",
    @SerializedName("episodes") val episodes: EpisodeCount? = null
)

// ── Episodes ──────────────────────────────────────────────────────────────

data class EpisodesResponse(
    @SerializedName("totalEpisodes") val totalEpisodes: Int         = 0,
    @SerializedName("episodes")      val episodes:      List<Episode> = emptyList()
)

data class Episode(
    @SerializedName("name")      val name: String?      = null,
    @SerializedName("episodeNo") val episodeNo: Int     = 0,
    @SerializedName("episodeId") val episodeId: String  = "",
    @SerializedName("filler")    val filler: Boolean    = false
)

// ── Servers ───────────────────────────────────────────────────────────────

data class ServersResponse(
    @SerializedName("episodeId") val episodeId: String  = "",
    @SerializedName("episodeNo") val episodeNo: Int     = 0,
    @SerializedName("sub")       val sub: List<Server>  = emptyList(),
    @SerializedName("dub")       val dub: List<Server>  = emptyList()
)

data class Server(
    @SerializedName("serverName") val serverName: String = "",
    @SerializedName("serverId")   val serverId: Int      = 0
)

// ── Sources ───────────────────────────────────────────────────────────────

data class SourcesResponse(
    @SerializedName("headers")   val headers: Map<String, String> = emptyMap(),
    @SerializedName("sources")   val sources: List<VideoSource>   = emptyList(),
    @SerializedName("subtitles") val subtitles: List<Subtitle>    = emptyList(),
    @SerializedName("anilistID") val anilistId: Int?              = null,
    @SerializedName("malID")     val malId: Int?                  = null
)

data class VideoSource(
    @SerializedName("url")     val url: String      = "",
    @SerializedName("isM3U8")  val isM3U8: Boolean  = false,
    @SerializedName("quality") val quality: String? = null
)

data class Subtitle(
    @SerializedName("lang") val lang: String = "",
    @SerializedName("url")  val url: String  = ""
)

// ── Watchlist (local only) ────────────────────────────────────────────────

data class WatchlistAnime(
    val id: String     = "",
    val name: String   = "",
    val img: String    = "",
    val type: String?  = null
)
