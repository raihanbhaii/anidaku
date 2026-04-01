package com.anilite.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WatchlistManager {
    private const val PREF_NAME = "anidaku_watchlist"
    private const val KEY_LIST = "watchlist"
    private val gson = Gson()

    fun getWatchlist(context: Context): List<WatchlistAnime> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, "[]")
        val type = object : TypeToken<List<WatchlistAnime>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addToWatchlist(context: Context, anime: WatchlistAnime) {
        val list = getWatchlist(context).toMutableList()
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
        prefs.edit().putString(KEY_LIST, gson.toJson(list)).apply()
    }
}
