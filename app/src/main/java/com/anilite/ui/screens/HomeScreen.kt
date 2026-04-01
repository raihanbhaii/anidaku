@file:OptIn(ExperimentalFoundationApi::class)

package com.anilite.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import coil.compose.AsyncImage
import com.anilite.data.AnimeItem
import com.anilite.data.BasicAnime
import com.anilite.data.RetrofitClient
import com.anilite.data.SpotlightAnime
import com.anilite.ui.components.AnimeCard
import com.anilite.ui.theme.Purple40
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onAnimeClick: (String) -> Unit) {
    var homeData by remember { mutableStateOf<com.anilite.data.HomeResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            homeData = RetrofitClient.api.getHome() // ← no .data wrapper
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Purple40)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Anidaku",
            color = Purple40,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        homeData?.spotlightAnimes?.takeIf { it.isNotEmpty() }?.let {
            SpotlightCarousel(spotlights = it, onAnimeClick = onAnimeClick)
        }

        Spacer(Modifier.height(16.dp))

        // trendingAnimes → BasicAnime, convert to AnimeItem for AnimeRow
        homeData?.trendingAnimes?.takeIf { it.isNotEmpty() }?.let { list ->
            AnimeRow(
                title = "Trending",
                animes = list.map { AnimeItem(id = it.id, name = it.name, img = it.img) },
                onAnimeClick = onAnimeClick
            )
        }

        // latestEpisodes → AnimeItem directly
        homeData?.latestEpisodes?.takeIf { it.isNotEmpty() }?.let {
            AnimeRow(title = "Latest Episodes", animes = it, onAnimeClick = onAnimeClick)
        }

        // featuredAnimes.topAiringAnimes → BasicAnime → convert
        homeData?.featuredAnimes?.topAiringAnimes?.takeIf { it.isNotEmpty() }?.let { list ->
            AnimeRow(
                title = "Top Airing",
                animes = list.map { AnimeItem(id = it.id, name = it.name, img = it.img) },
                onAnimeClick = onAnimeClick
            )
        }

        // featuredAnimes.mostPopularAnimes → BasicAnime → convert
        homeData?.featuredAnimes?.mostPopularAnimes?.takeIf { it.isNotEmpty() }?.let { list ->
            AnimeRow(
                title = "Most Popular",
                animes = list.map { AnimeItem(id = it.id, name = it.name, img = it.img) },
                onAnimeClick = onAnimeClick
            )
        }

        // topUpcomingAnimes → AnimeItem directly
        homeData?.topUpcomingAnimes?.takeIf { it.isNotEmpty() }?.let {
            AnimeRow(title = "Top Upcoming", animes = it, onAnimeClick = onAnimeClick)
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun SpotlightCarousel(spotlights: List<SpotlightAnime>, onAnimeClick: (String) -> Unit) {
    val pagerState = rememberPagerState { spotlights.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % spotlights.size
            scope.launch { pagerState.animateScrollToPage(next) }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        HorizontalPager(state = pagerState) { page ->
            val anime = spotlights[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { if (anime.id.isNotEmpty()) onAnimeClick(anime.id) }
            ) {
                AsyncImage(
                    model = anime.img,           // ← was anime.poster
                    contentDescription = anime.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xDD0A0A0F)),
                                startY = 80f
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = anime.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Build info row from available fields
                    val info = listOfNotNull(
                        anime.quality,
                        anime.category,
                        anime.duration.takeIf { it.isNotEmpty() }
                    ).take(3)
                    if (info.isNotEmpty()) {
                        Text(
                            text = info.joinToString(" • "),
                            color = Color(0xFFB0B0C0),
                            fontSize = 11.sp
                        )
                    }
                }
                anime.rank?.let {
                    Text(
                        text = "#$it",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Purple40, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            spotlights.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == pagerState.currentPage) 8.dp else 5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i == pagerState.currentPage) Purple40 else Color.Gray)
                )
            }
        }
    }
}

@Composable
fun AnimeRow(title: String, animes: List<AnimeItem>, onAnimeClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(animes, key = { it.id.ifEmpty { it.name } }) { anime ->
                AnimeCard(anime = anime, onClick = { if (anime.id.isNotEmpty()) onAnimeClick(anime.id) })
            }
        }
    }
}
