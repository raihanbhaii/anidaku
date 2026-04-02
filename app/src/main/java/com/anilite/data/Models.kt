package com.anilite.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Shared ────────────────────────────────────────────────────────────────
@Serializable
data class EpisodeCount(
    @SerialName("eps") val eps: Double? = null,
    @SerialName("sub") val sub: Double? = null,
    @SerialName("dub") val dub: Double? = null
) {
    val epsInt: Int? get() = eps?.toInt()?.takeIf { it in 1..9999 }
    val subInt: Int? get() = sub?.toInt()?.takeIf { it in 1..9999 }
    val dubInt: Int? get() = dub?.toInt()?.takeIf { it in 1..9999 }
}

// ── Home Response ─────────────────────────────────────────────────────────
@Serializable
data class HomeResponse(
    @SerialName("spotLightAnimes") val spotlightAnimes: List<SpotlightAnime> = emptyList(),
    @SerialName("trendingAnimes") val trendingAnimes: List<BasicAnime> = emptyList(),
    @SerialName("latestEpisodes") val latestEpisodes: List<AnimeItem> = emptyList(),
    @SerialName("top10Animes") val top10Animes: Top10Animes? = null,
    @SerialName("featuredAnimes") val featuredAnimes: FeaturedAnimes? = null,
    @SerialName("topUpcomingAnimes") val topUpcomingAnimes: List<AnimeItem> = emptyList(),
    @SerialName("genres") val genres: List<String> = emptyList()
)

@Serializable
data class SpotlightAnime(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("rank") val rank: Int? = null,
    @SerialName("img") val img: String = "",
    @SerialName("episodes") val episodes: EpisodeCount? = null,
    @SerialName("duration") val duration: String = "",
    @SerialName("quality") val quality: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("releasedDay") val releasedDay: String? = null,
    @SerialName("description") val description: String? = null
)

@Serializable
data class AnimeItem(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("img") val img: String = "",
    @SerialName("episodes") val episodes: EpisodeCount? = null,
    @SerialName("duration") val duration: String? = null,
    @SerialName("rated") val rated: Boolean = false
)

@Serializable
data class BasicAnime(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("img") val img: String = ""
)

@Serializable
data class Top10Animes(
    @SerialName("day") val day: List<Top10Anime> = emptyList(),
    @SerialName("week") val week: List<Top10Anime> = emptyList(),
    @SerialName("month") val month: List<Top10Anime> = emptyList()
)

@Serializable
data class Top10Anime(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("rank") val rank: Int = 0,
    @SerialName("img") val img: String = "",
    @SerialName("episodes") val episodes: EpisodeCount? = null
)

@Serializable
data class FeaturedAnimes(
    @SerialName("topAiringAnimes") val topAiringAnimes: List<BasicAnime> = emptyList(),
    @SerialName("mostPopularAnimes") val mostPopularAnimes: List<BasicAnime> = emptyList(),
    @SerialName("mostFavoriteAnimes") val mostFavoriteAnimes: List<BasicAnime> = emptyList(),
    @SerialName("latestCompletedAnimes") val latestCompletedAnimes: List<BasicAnime> = emptyList()
)

// ── Search / Category / Detail ────────────────────────────────────────────
@Serializable
data class SearchResponse(
    @SerialName("animes") val animes: List<AnimeItem> = emptyList(),
    @SerialName("mostPopularAnimes") val mostPopularAnimes: List<RelatedAnime> = emptyList(),
    @SerialName("top10Animes") val top10Animes: Top10Animes? = null,
    @SerialName("category") val category: String = "",
    @SerialName("genres") val genres: List<String> = emptyList(),
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("hasNextPage") val hasNextPage: Boolean = false,
    @SerialName("totalPages") val totalPages: Int = 1
)

@Serializable
data class AnimeDetailResponse(
    @SerialName("info") val info: AnimeInfo? = null,
    @SerialName("moreInfo") val moreInfo: AnimeMoreInfo? = null,
    @SerialName("seasons") val seasons: List<AnimeSeason> = emptyList(),
    @SerialName("relatedAnimes") val relatedAnimes: List<RelatedAnime> = emptyList(),
    @SerialName("recommendedAnimes") val recommendedAnimes: List<AnimeItem> = emptyList(),
    @SerialName("mostPopularAnimes") val mostPopularAnimes: List<RelatedAnime> = emptyList()
)

@Serializable
data class AnimeInfo(
    @SerialName("id") val id: String = "",
    @SerialName("anime_id") val animeId: Int = 0,
    @SerialName("mal_id") val malId: Int = 0,
    @SerialName("al_id") val alId: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("img") val img: String = "",
    @SerialName("rating") val rating: String = "",
    @SerialName("episodes") val episodes: EpisodeCount? = null,
    @SerialName("category") val category: String = "",
    @SerialName("quality") val quality: String = "",
    @SerialName("duration") val duration: String = "",
    @SerialName("description") val description: String = ""
)

@Serializable
data class AnimeMoreInfo(
    @SerialName("Japanese:") val japanese: String? = null,
    @SerialName("Synonyms:") val synonyms: String? = null,
    @SerialName("Aired:") val aired: String? = null,
    @SerialName("Premiered:") val premiered: String? = null,
    @SerialName("Duration:") val duration: String? = null,
    @SerialName("Status:") val status: String? = null,
    @SerialName("MAL Score:") val malScore: String? = null,
    @SerialName("Studios:") val studios: String? = null,
    @SerialName("Genres") val genres: List<String> = emptyList(),
    @SerialName("Producers") val producers: List<String> = emptyList()
)

@Serializable
data class AnimeSeason(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("seasonTitle") val seasonTitle: String = "",
    @SerialName("img") val img: String = "",
    @SerialName("isCurrent") val isCurrent: Boolean = false
)

@Serializable
data class RelatedAnime(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("img") val img: String = "",
    @SerialName("episodes") val episodes: EpisodeCount? = null
)

// ── Episodes ──────────────────────────────────────────────────────────────
@Serializable
data class EpisodesResponse(
    @SerialName("totalEpisodes") val totalEpisodes: Int = 0,
    @SerialName("episodes") val episodes: List<Episode> = emptyList()
)

@Serializable
data class Episode(
    @SerialName("name") val name: String? = null,
    @SerialName("episodeNo") val episodeNo: Int = 0,
    @SerialName("episodeId") val episodeId: String = "",
    @SerialName("filler") val filler: Boolean = false
)

// ── Servers & Sources ─────────────────────────────────────────────────────
@Serializable
data class ServersResponse(
    @SerialName("episodeId") val episodeId: String = "",
    @SerialName("episodeNo") val episodeNo: Int = 0,
    @SerialName("sub") val sub: List<Server> = emptyList(),
    @SerialName("dub") val dub: List<Server> = emptyList()
)

@Serializable
data class Server(
    @SerialName("serverName") val serverName: String = "",
    @SerialName("serverId") val serverId: Int = 0
)

@Serializable
data class SourcesResponse(
    @SerialName("headers") val headers: Map<String, String> = emptyMap(),
    @SerialName("sources") val sources: List<VideoSource> = emptyList(),
    @SerialName("subtitles") val subtitles: List<Subtitle> = emptyList(),
    @SerialName("anilistID") val anilistId: Int? = null,
    @SerialName("malID") val malId: Int? = null
)

@Serializable
data class VideoSource(
    @SerialName("url") val url: String = "",
    @SerialName("isM3U8") val isM3U8: Boolean = false,
    @SerialName("quality") val quality: String? = null
)

@Serializable
data class Subtitle(
    @SerialName("lang") val lang: String = "",
    @SerialName("url") val url: String = ""
)

// ── Watchlist (local only) ────────────────────────────────────────────────
@Serializable
data class WatchlistAnime(
    val id: String = "",
    val name: String = "",
    val img: String = "",
    val type: String? = null
)
