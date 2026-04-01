package com.anilite.data

data class HomeResponse(val data: HomeData?)
data class HomeData(
    val spotlightAnimes: List<SpotlightAnime>?,
    val trendingAnimes: List<AnimeItem>?,
    val latestEpisodeAnimes: List<AnimeItem>?,
    val topAiringAnimes: List<AnimeItem>?,
    val mostPopularAnimes: List<AnimeItem>?,
    val mostFavoriteAnimes: List<AnimeItem>?,
    val latestCompletedAnimes: List<AnimeItem>?
)

data class SpotlightAnime(
    val id: String?,
    val name: String?,
    val description: String?,
    val poster: String?,
    val rank: Int?,
    val otherInfo: List<String>?
)

data class AnimeItem(
    val id: String?,
    val name: String?,
    val poster: String?,
    val duration: String?,
    val type: String?,
    val rating: String?,
    val episodes: EpisodeCount?
)

data class EpisodeCount(val sub: Int?, val dub: Int?)

data class SearchResponse(val data: SearchData?)
data class SearchData(
    val animes: List<AnimeItem>?,
    val totalPages: Int?,
    val currentPage: Int?
)

data class AnimeDetailResponse(val data: AnimeDetailData?)
data class AnimeDetailData(val anime: AnimeDetail?, val relatedAnimes: List<AnimeItem>?)
data class AnimeDetail(
    val info: AnimeInfo?,
    val moreInfo: AnimeMoreInfo?
)
data class AnimeInfo(
    val id: String?,
    val name: String?,
    val poster: String?,
    val description: String?,
    val stats: AnimeStats?
)
data class AnimeStats(
    val rating: String?,
    val quality: String?,
    val episodes: EpisodeCount?,
    val type: String?,
    val duration: String?
)
data class AnimeMoreInfo(
    val genres: List<String>?,
    val studios: String?,
    val status: String?
)

data class EpisodesResponse(val data: EpisodesData?)
data class EpisodesData(val episodes: List<Episode>?, val totalEpisodes: Int?)

// episodeId format from this API: "anime-slug?ep=136197"
// we extract the number after "ep=" to use with megaplay.buzz
data class Episode(
    val episodeId: String?,   // e.g. "steinsgate-3?ep=213"
    val number: Int?,
    val title: String?,
    val isFiller: Boolean?
) {
    // extracts just the numeric ep id for the player
    val epNumericId: String?
        get() = episodeId?.substringAfterLast("ep=")
}

data class ServersResponse(val data: ServersData?)
data class ServersData(
    val sub: List<Server>?,
    val dub: List<Server>?
)
data class Server(val serverId: Int?, val serverName: String?)

data class SourcesResponse(val data: SourcesData?)
data class SourcesData(
    val sources: List<Source>?,
    val tracks: List<Track>?
)
data class Source(val url: String?, val type: String?)
data class Track(val file: String?, val label: String?, val kind: String?, val default: Boolean?)

data class WatchlistAnime(
    val id: String,
    val name: String,
    val poster: String,
    val type: String?
)
