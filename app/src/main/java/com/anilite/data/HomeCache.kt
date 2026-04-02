package com.anilite.data

object HomeCache {
    var cachedHome: HomeResponse? = null
    var lastFetchTime: Long = 0

    // Cache is valid for 10 minutes
    fun isCacheValid(): Boolean {
        return cachedHome != null &&
            (System.currentTimeMillis() - lastFetchTime) < 10 * 60 * 1000
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
