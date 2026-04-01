package com.anilite.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WatchlistManager {

    private const val PREF_NAME = "anidaku_watchlist"
    private const val KEY_LIST = "watchlist"

    // JSON serializer (same style as Ktor)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    fun getWatchlist(context: Context): List<WatchlistAnime> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_LIST, "[]") ?: "[]"

        return try {
            json.decodeFromString<List<WatchlistAnime>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addToWatchlist(context: Context, anime: WatchlistAnime) {
        val list = getWatchlist(context).toMutableList()

        // Avoid duplicates
        if (list.none { it.id == anime.id }) {
            list.add(anime)
            save(context, list)
        }
    }

    fun removeFromWatchlist(context: Context, animeId: String) {
        val list = getWatchlist(context).filter { it.id != animeId }
        save(context, list)
    }

    fun isInWatchlist(context: Context, animeId: String): Boolean {
        return getWatchlist(context).any { it.id == animeId }
    }

    private fun save(context: Context, list: List<WatchlistAnime>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = json.encodeToString(list)
        prefs.edit().putString(KEY_LIST, jsonString).apply()
    }
}
