package com.anilite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class Anime(
    val title: String,
    val score: String,
    val episodes: String,
    val synopsis: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AniLiteApp()
        }
    }
}

@Composable
fun AniLiteApp() {
    var animeList by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        animeList = fetchTopAnime()
        isLoading = false
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "AniLite",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search anime...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = animeList.filter {
                    it.title.contains(searchQuery, ignoreCase = true)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { anime ->
                        AnimeCard(anime)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeCard(anime: Anime) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(anime.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("⭐ ${anime.score}  |  ${anime.episodes} eps", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(anime.synopsis, fontSize = 12.sp, maxLines = 3)
        }
    }
}

suspend fun fetchTopAnime(): List<Anime> {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://api.jikan.moe/v4/top/anime?limit=25"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val data = json.getJSONArray("data")
            val list = mutableListOf<Anime>()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                list.add(
                    Anime(
                        title = item.optString("title", "Unknown"),
                        score = item.optString("score", "N/A"),
                        episodes = item.optString("episodes", "?"),
                        synopsis = item.optString("synopsis", "No description.")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
