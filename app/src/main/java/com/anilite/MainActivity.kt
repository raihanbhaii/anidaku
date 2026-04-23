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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
            composable("home") { 
                HomeScreen(onAnimeClick = { id -> navController.navigate("detail/$id") }) 
            }
            composable(
                "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: 0
                DetailScreen(
                    id = id, 
                    onBack = { navController.popBackStack() },
                    onEpisodeClick = { episodeId -> navController.navigate("player/$episodeId") }
                )
            }
            composable(
                "player/{episodeId}",
                arguments = listOf(navArgument("episodeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val episodeId = backStackEntry.arguments?.getString("episodeId") ?: ""
                PlayerScreen(episodeId = episodeId, onBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAnimeClick: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var trendingAnime by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }
    var popularAnime by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            trendingAnime = ApiClient.getTrending().results
            popularAnime = ApiClient.getPopular().results
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ANIDAKU", fontWeight = FontWeight.ExtraBold, color = Color.Red, letterSpacing = 4.sp) },
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
                    focusedBorderColor = Color.Red,
                    unfocusedBorderColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            LazyColumn {
                if (searchQuery.isEmpty()) {
                    item {
                        SectionTitle("Trending Now")
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(trendingAnime) { anime ->
                                AnimeCard(anime, onAnimeClick)
                            }
                        }
                    }
                    item {
                        SectionTitle("Most Popular")
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(popularAnime) { anime ->
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
fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(16.dp),
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun AnimeCard(anime: AnimeSummary, onClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .padding(end = 12.dp)
            .clickable { onClick(anime.id) }
    ) {
        AsyncImage(
            model = anime.coverImage?.getBestImage(),
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
fun SearchItem(anime: AnimeSummary, onClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick(anime.id) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = anime.coverImage?.getBestImage(),
            contentDescription = null,
            modifier = Modifier
                .size(70.dp, 100.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(anime.title.getDisplayTitle(), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${anime.format ?: "TV"} • ${anime.status ?: "Unknown"}", color = Color.Gray, fontSize = 12.sp)
            Text("Score: ${anime.averageScore ?: "N/A"}%", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(id: Int, onBack: () -> Unit, onEpisodeClick: (String) -> Unit) {
    var info by remember { mutableStateOf<AnimeInfo?>(null) }
    var episodeResponse by remember { mutableStateOf<EpisodeResponse?>(null) }
    var selectedProvider by remember { mutableStateOf("kiwi") }
    var selectedCategory by remember { mutableStateOf("sub") }

    LaunchedEffect(id) {
        try {
            info = ApiClient.getAnimeInfo(id)
            episodeResponse = ApiClient.getEpisodes(id)
        } catch (e: Exception) {}
    }

    if (info == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Red)
        }
    } else {
        val episodes = episodeResponse?.providers?.get(selectedProvider)?.episodes?.get(selectedCategory) ?: emptyList()

        LazyColumn(Modifier.fillMaxSize().background(Color.Black)) {
            item {
                Box {
                    AsyncImage(
                        model = info?.bannerImage ?: info?.coverImage?.getBestImage(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        Modifier.fillMaxWidth().height(300.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    )
                    IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
            }
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(info?.title?.getDisplayTitle() ?: "", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${info?.season ?: ""} ${info?.seasonYear ?: ""}", color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("•", color = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text("${info?.episodes ?: "?"} Eps", color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("•", color = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text("${info?.averageScore ?: "?"}%", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(info?.description?.replace(Regex("<.*?>"), "") ?: "", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                    
                    // Provider/Category Selection
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Episodes", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        // In a real app, add chips to select provider/category
                    }
                }
            }
            items(episodes) { ep ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                        .clickable { onEpisodeClick(ep.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp).background(Color.Red, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Episode ${ep.number}", color = Color.White, fontWeight = FontWeight.Bold)
                        if (!ep.title.isNullOrBlank()) {
                            Text(ep.title, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// PlayerScreen moved to separate file
