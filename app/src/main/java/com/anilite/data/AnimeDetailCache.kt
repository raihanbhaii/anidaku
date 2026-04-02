package com.anilite.data

object HomeCache {
    private const val TTL = 10 * 60 * 1000L

    var cachedHome: HomeResponse? = null
    var lastFetchTime: Long = 0

    fun isCacheValid(): Boolean {
        return cachedHome != null &&
            (System.currentTimeMillis() - lastFetchTime) < TTL
    }

    fun save(data: HomeResponse) {
        cachedHome = data
        lastFetchTime = System.currentTimeMillis()
    }

    fun clear() {
        cachedHome = null
        lastFetchTime = 0
    }
}
