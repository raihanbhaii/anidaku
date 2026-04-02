package com.anilite.data

object AnimeDetailCache {
    private const val TTL = 10 * 60 * 1000L

    private data class CacheEntry(
        val detail: AnimeDetailResponse,
        val episodes: EpisodesResponse?,
        val fetchTime: Long = System.currentTimeMillis()
    ) {
        fun isValid() = (System.currentTimeMillis() - fetchTime) < TTL
    }

    private val cache = mutableMapOf<String, CacheEntry>()

    fun getDetail(animeId: String): AnimeDetailResponse? =
        cache[animeId]?.takeIf { it.isValid() }?.detail

    fun getEpisodes(animeId: String): EpisodesResponse? =
        cache[animeId]?.takeIf { it.isValid() }?.episodes

    fun isCacheValid(animeId: String): Boolean =
        cache[animeId]?.isValid() == true

    fun save(animeId: String, detail: AnimeDetailResponse, episodes: EpisodesResponse?) {
        cache[animeId] = CacheEntry(detail, episodes)
    }

    fun clear(animeId: String) {
        cache.remove(animeId)
    }

    fun clearAll() {
        cache.clear()
    }
}
