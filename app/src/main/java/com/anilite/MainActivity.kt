package com.anilite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun Bundle? onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnidakuApp()
        }
    }
}

@Composable
fun AnidakuApp() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") { HomeScreen(onAnimeClick = { id -> navController.navigate("detail/$id") }) }
            composable(
                "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                DetailScreen(id = id, onBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAnimeClick: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var trendingAnime by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            trendingAnime = ApiClient.getTrending().results
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ANIDAKU", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    if (it.length > 2) {
                        scope.launch {
                            try {
                                searchResults = ApiClient.searchAnime(it).results
                            } catch (e: Exception) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search Anime...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            LazyColumn {
                if (searchQuery.isEmpty()) {
                    item {
                        Text(
                            "Trending Now",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(trendingAnime) { anime ->
                                AnimeCard(anime, onAnimeClick)
                            }
                        }
                    }
                } else {
                    items(searchResults) { anime ->
                        SearchItem(anime, onAnimeClick)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeCard(anime: AnimeSummary, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .padding(end = 12.dp)
            .clickable { onClick(anime.id) }
    ) {
        AsyncImage(
            model = anime.poster ?: anime.cover,
            contentDescription = null,
            modifier = Modifier
                .height(220.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = anime.title.getDisplayTitle(),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SearchItem(anime: AnimeSummary, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick(anime.id) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = anime.poster ?: anime.cover,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp, 120.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(anime.title.getDisplayTitle(), color = Color.White, fontWeight = FontWeight.Bold)
            Text(anime.type ?: "", color = Color.Gray, fontSize = 12.sp)
            Text(anime.status ?: "", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun DetailScreen(id: String, onBack: () -> Unit) {
    var info by remember { mutableStateOf<AnimeInfo?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }

    LaunchedEffect(id) {
        try {
            info = ApiClient.getAnimeInfo(id)
            val epRes = ApiClient.getEpisodes(id)
            // Pick first provider and sub for simplicity
            episodes = epRes.providers.values.firstOrNull()?.episodes?.get("sub") ?: emptyList()
        } catch (e: Exception) {}
    }

    if (info == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().background(Color.Black)) {
            item {
                Box {
                    AsyncImage(
                        model = info?.bannerImage ?: info?.coverImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxWidth().height(250.dp).background(Color.Black.copy(alpha = 0.4f)))
                }
            }
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(info?.title?.getDisplayTitle() ?: "", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(info?.description?.replace(Regex("<.*?>"), "") ?: "", color = Color.Gray, fontSize = 14.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(16.dp))
                    Text("Episodes", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            items(episodes) { ep ->
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).clickable { /* Handle Play */ },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(100.dp, 60.dp).background(Color.DarkGray, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Text("EP ${ep.number}", color = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(ep.title ?: "Episode ${ep.number}", color = Color.White)
                }
            }
        }
    }
}
