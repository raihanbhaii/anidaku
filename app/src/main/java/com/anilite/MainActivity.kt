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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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

// ── Brand colours ──────────────────────────────────────────────────────────────
val BrandPurple       = Color(0xFF7B2FBE)   // primary
val BrandPurpleDark   = Color(0xFF5B1FA0)
val BrandPurpleLight  = Color(0xFFA64EE0)
val AccentOrange      = Color(0xFFFF6B00)   // Crunchyroll-style accent for badges
val BackgroundDark    = Color(0xFF0D0D0F)
val SurfaceDark       = Color(0xFF1A1A1F)
val SurfaceCard       = Color(0xFF222228)
val TextPrimary       = Color(0xFFF5F5F5)
val TextSecondary     = Color(0xFFAAAAAA)
val TextMuted         = Color(0xFF666666)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnidakuAppUI()
        }
    }
}

@Composable
fun AnidakuAppUI() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
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

// ── Home Screen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAnimeClick: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var trendingAnime   by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }
    var popularAnime    by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }
    var spotlightAnime  by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }
    var recentAnime     by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }
    var searchQuery     by remember { mutableStateOf("") }
    var searchResults   by remember { mutableStateOf<List<AnimeSummary>>(emptyList()) }
    var isSearching     by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            trendingAnime  = ApiClient.getTrending().results
            popularAnime   = ApiClient.getPopular().results
            spotlightAnime = ApiClient.getSpotlight().results
            recentAnime    = ApiClient.getRecent().results
        } catch (e: Exception) { e.printStackTrace() }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(BackgroundDark)) {
                // ── Logo bar ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Logo left
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.linearGradient(listOf(BrandPurple, BrandPurpleLight)),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ANIDAKU",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 3.sp
                        )
                    }
                }
                // ── Search bar ────────────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length > 2) {
                            isSearching = true
                            scope.launch {
                                try {
                                    searchResults = ApiClient.searchAnime(it).results
                                } catch (e: Exception) {}
                                isSearching = false
                            }
                        } else {
                            searchResults = emptyList()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    placeholder = { Text("Search anime, genres, studios...", color = TextMuted, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = BrandPurpleLight, modifier = Modifier.size(20.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor       = TextPrimary,
                        unfocusedTextColor     = TextPrimary,
                        focusedBorderColor     = BrandPurple,
                        unfocusedBorderColor   = SurfaceCard,
                        focusedContainerColor  = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor            = BrandPurpleLight
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Divider(color = SurfaceCard, thickness = 1.dp)
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (searchQuery.isEmpty()) {
                // ── Spotlight Hero ─────────────────────────────────────────────
                if (spotlightAnime.isNotEmpty()) {
                    item {
                        SpotlightHero(anime = spotlightAnime.first(), onClick = onAnimeClick)
                    }
                    item {
                        SpotlightDots(count = minOf(spotlightAnime.size, 5), selected = 0)
                    }
                }
                // ── Trending ───────────────────────────────────────────────────
                item {
                    SectionHeader(title = "Trending Now", badge = "TRENDING")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trendingAnime) { anime -> AnimeCard(anime, onAnimeClick) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                // ── Popular ────────────────────────────────────────────────────
                item {
                    SectionHeader(title = "Most Popular", badge = null)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(popularAnime) { anime -> AnimeCard(anime, onAnimeClick) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                // ── Recently Added ─────────────────────────────────────────────
                if (recentAnime.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Recently Added", badge = "NEW")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentAnime) { anime -> AnimeCard(anime, onAnimeClick) }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            } else {
                // ── Search Results ─────────────────────────────────────────────
                item {
                    Text(
                        "Results for \"$searchQuery\"",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                if (isSearching) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandPurple, modifier = Modifier.size(32.dp))
                        }
                    }
                } else {
                    items(searchResults) { anime -> SearchItem(anime, onAnimeClick) }
                }
            }
        }
    }
}

// ── Spotlight Hero ─────────────────────────────────────────────────────────────

@Composable
fun SpotlightHero(anime: AnimeSummary, onClick: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .clickable { onClick(anime.id) }
    ) {
        // Background image
        AsyncImage(
            model = anime.bannerImage ?: anime.coverImage?.getBestImage(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Gradient overlays
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.3f),
                    0.4f to Color.Transparent,
                    0.75f to BackgroundDark.copy(alpha = 0.8f),
                    1f to BackgroundDark
                )
            )
        )
        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Genre chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // placeholder genre chips
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = anime.title.getDisplayTitle(),
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                anime.seasonYear?.let {
                    Text(it.toString(), color = TextSecondary, fontSize = 13.sp)
                    Text("•", color = TextMuted, fontSize = 13.sp)
                }
                anime.format?.let {
                    Text(it, color = TextSecondary, fontSize = 13.sp)
                    Text("•", color = TextMuted, fontSize = 13.sp)
                }
                anime.averageScore?.let {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(13.dp))
                    Text("$it%", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Play button
                Button(
                    onClick = { onClick(anime.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Watch Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                // Watchlist button
                OutlinedButton(
                    onClick = { /* TODO: add to watchlist */ },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(SurfaceCard, SurfaceCard))
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", color = TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SpotlightDots(count: Int, selected: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == selected) 20.dp else 6.dp, 6.dp)
                    .background(
                        if (i == selected) BrandPurple else TextMuted,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

// ── Section Header ─────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, badge: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(18.dp)
                .background(BrandPurple, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(badge, color = AccentOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("See all", color = BrandPurpleLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Anime Card ─────────────────────────────────────────────────────────────────

@Composable
fun AnimeCard(anime: AnimeSummary, onClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick(anime.id) }
    ) {
        Box {
            AsyncImage(
                model = anime.coverImage?.getBestImage(),
                contentDescription = null,
                modifier = Modifier
                    .height(185.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            // Score badge
            anime.averageScore?.let { score ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("$score", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Format badge
            anime.format?.let { fmt ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(BrandPurple.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(fmt, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = anime.title.getDisplayTitle(),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp
        )
        anime.seasonYear?.let {
            Text(it.toString(), color = TextMuted, fontSize = 11.sp)
        }
    }
}

// ── Search Item ────────────────────────────────────────────────────────────────

@Composable
fun SearchItem(anime: AnimeSummary, onClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .clickable { onClick(anime.id) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = anime.coverImage?.getBestImage(),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp, 85.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                anime.title.getDisplayTitle(),
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip(text = anime.format ?: "TV")
                anime.status?.let { Chip(text = it) }
            }
            Spacer(Modifier.height(4.dp))
            anime.averageScore?.let { score ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("$score%", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Box(
        modifier = Modifier
            .background(SurfaceDark, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = TextSecondary, fontSize = 10.sp)
    }
}

// ── Detail Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(id: Int, onBack: () -> Unit, onEpisodeClick: (String) -> Unit) {
    var info             by remember { mutableStateOf<AnimeInfo?>(null) }
    var episodeResponse  by remember { mutableStateOf<EpisodeResponse?>(null) }
    var selectedProvider by remember { mutableStateOf("kiwi") }
    var selectedCategory by remember { mutableStateOf("sub") }

    LaunchedEffect(id) {
        try {
            info            = ApiClient.getAnimeInfo(id)
            episodeResponse = ApiClient.getEpisodes(id)
            // pick first available provider
            episodeResponse?.providers?.keys?.firstOrNull()?.let { selectedProvider = it }
        } catch (e: Exception) { e.printStackTrace() }
    }

    if (info == null) {
        Box(
            Modifier.fillMaxSize().background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = BrandPurple)
                Spacer(Modifier.height(12.dp))
                Text("Loading...", color = TextSecondary, fontSize = 14.sp)
            }
        }
    } else {
        val availableProviders = episodeResponse?.providers?.keys?.toList() ?: emptyList()
        val episodes = episodeResponse
            ?.providers?.get(selectedProvider)
            ?.episodes?.get(selectedCategory) ?: emptyList()

        LazyColumn(Modifier.fillMaxSize().background(BackgroundDark)) {
            // ── Hero Banner ────────────────────────────────────────────────────
            item {
                Box {
                    AsyncImage(
                        model = info?.bannerImage ?: info?.coverImage?.getBestImage(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        Modifier.fillMaxWidth().height(280.dp).background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(0.4f),
                                    Color.Transparent,
                                    BackgroundDark.copy(0.9f),
                                    BackgroundDark
                                )
                            )
                        )
                    )
                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(0.6f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── Anime Info ─────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-24).dp),
                    horizontalArrangement = Arrangement.Start, // Fixed: using correct parameter
                    verticalAlignment = Alignment.Bottom // Fixed: using correct parameter
                ) {
                    // Cover image
                    AsyncImage(
                        model = info?.coverImage?.getBestImage(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp, 145.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f).padding(top = 40.dp)) {
                        Text(
                            info?.title?.getDisplayTitle() ?: "",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        // Meta row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            info?.averageScore?.let {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                Text("$it%", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("•", color = TextMuted)
                            Text("${info?.episodes ?: "?"} eps", color = TextSecondary, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        // Studio
                        info?.studios?.firstOrNull { it.isAnimationStudio }?.let { studio ->
                            Text(studio.name ?: "", color = BrandPurpleLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Genre Chips ────────────────────────────────────────────────────
            item {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-16).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(info?.genres ?: emptyList()) { genre ->
                        Box(
                            modifier = Modifier
                                .background(BrandPurple.copy(0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(genre, color = BrandPurpleLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Play Button ────────────────────────────────────────────────────
            item {
                Button(
                    onClick = {
                        episodes.firstOrNull()?.let { onEpisodeClick(it.id) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Watching", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // ── Description ────────────────────────────────────────────────────
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Synopsis", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        info?.description?.replace(Regex("<.*?>"), "") ?: "",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // ── Provider & Category Tabs ───────────────────────────────────────
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Divider(color = SurfaceCard)
                    Spacer(Modifier.height(12.dp))
                    // Sub / Dub tabs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("sub", "dub").forEach { cat ->
                            val selected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) BrandPurple else SurfaceCard)
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    cat.uppercase(),
                                    color = if (selected) Color.White else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Provider chips
                    if (availableProviders.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableProviders) { provider ->
                                val sel = selectedProvider == provider
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sel) BrandPurpleDark else SurfaceDark)
                                        .clickable { selectedProvider = provider }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        provider.replaceFirstChar { it.uppercase() },
                                        color = if (sel) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        "Episodes (${episodes.size})",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
            }

            // ── Episodes ──────────────────────────────────────────────────────
            items(episodes) { ep ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .clickable { onEpisodeClick(ep.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail / play button
                    Box(
                        modifier = Modifier
                            .size(64.dp, 44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        if (ep.image != null) {
                            AsyncImage(
                                model = ep.image,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.35f)))
                        }
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(BrandPurple.copy(0.9f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ep ${ep.number}" + if (ep.filler) "  [FILLER]" else "",
                            color = if (ep.filler) TextMuted else TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        if (!ep.title.isNullOrBlank()) {
                            Text(
                                ep.title,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        ep.airDate?.let {
                            Text(it, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    // Duration - Fixed: Check if duration exists in Episode data class
                    ep.duration?.let { dur ->
                        val mins = dur / 60
                        Text("${mins}m", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
