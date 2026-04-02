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
import com.anilite.data.*
import com.anilite.ui.theme.Purple40
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onAnimeClick: (String) -> Unit) {
    var homeData by remember { mutableStateOf<HomeResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var loadTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(loadTrigger) {
        isLoading = true
        errorMsg = ""
        try {
            homeData = AniApiService.getHome()
        } catch (e: Exception) {
            errorMsg = "${e::class.simpleName}: ${e.message ?: "unknown error"}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

        if (errorMsg.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0A0A))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Error loading anime:",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(errorMsg, color = Color(0xFFFF8080), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { loadTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple40)
                    ) {
                        Text("Retry")
                    }
                }
            }
            return@Column
        }

        val data = homeData ?: return@Column

        // Spotlight Carousel
        if (data.spotlightAnimes.isNotEmpty()) {
            SpotlightCarousel(
                animes = data.spotlightAnimes,
                onAnimeClick = onAnimeClick
            )
            Spacer(Modifier.height(16.dp))
        }

        // Trending
        if (data.trendingAnimes.isNotEmpty()) {
            AniwatchAnimeRow("Trending", data.trendingAnimes, onAnimeClick)
        }

        // Currently Airing
        data.featuredAnimes?.topAiringAnimes?.let {
            if (it.isNotEmpty()) AniwatchAnimeRow("Currently Airing", it, onAnimeClick)
        }

        // Most Popular
        data.featuredAnimes?.mostPopularAnimes?.let {
            if (it.isNotEmpty()) AniwatchAnimeRow("Most Popular", it, onAnimeClick)
        }

        // Upcoming
        if (data.topUpcomingAnimes.isNotEmpty()) {
            AniwatchAnimeRow("Upcoming", data.topUpcomingAnimes, onAnimeClick)
        }

        // Empty state
        if (data.spotlightAnimes.isEmpty() && data.trendingAnimes.isEmpty() &&
            data.topUpcomingAnimes.isEmpty() && errorMsg.isEmpty()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "No anime available",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Check your internet connection",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { loadTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple40)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ====================== Spotlight Carousel ======================
@Composable
fun SpotlightCarousel(
    animes: List<SpotlightAnime>,
    onAnimeClick: (String) -> Unit
) {
    if (animes.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { animes.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % animes.size
            scope.launch { pagerState.animateScrollToPage(next) }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        HorizontalPager(state = pagerState) { page ->
            val anime = animes[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAnimeClick(anime.id) }
            ) {
                AsyncImage(
                    model = anime.img,
                    contentDescription = anime.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xDD0A0A0F)),
                            startY = 80f
                        )
                    )
                )

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        text = anime.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val info = listOfNotNull(anime.category, anime.duration, anime.quality)
                    if (info.isNotEmpty()) {
                        Text(
                            text = info.joinToString(" • "),
                            color = Color(0xFFB0B0C0),
                            fontSize = 11.sp
                        )
                    }
                }

               anime.episodes?.epsInt?.let { eps ->
    if (eps > 0) {
                        Text(
                            text = "Ep $eps",
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
        }

        // Page Indicators
        if (animes.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                animes.indices.forEach { i ->
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
}

// ====================== Reusable Anime Row ======================
@Composable
fun AniwatchAnimeRow(
    title: String,
    animes: List<Any>,
    onAnimeClick: (String) -> Unit
) {
    if (animes.isEmpty()) return

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
            items(animes, key = { item ->
                when (item) {
                    is SpotlightAnime -> item.id
                    is BasicAnime -> item.id
                    is AnimeItem -> item.id
                    else -> System.currentTimeMillis().toString()
                }
            }) { item ->
                val id = when (item) {
                    is SpotlightAnime -> item.id
                    is BasicAnime -> item.id
                    is AnimeItem -> item.id
                    else -> ""
                }
                val name = when (item) {
                    is SpotlightAnime -> item.name
                    is BasicAnime -> item.name
                    is AnimeItem -> item.name
                    else -> ""
                }
                val img = when (item) {
                    is SpotlightAnime -> item.img
                    is BasicAnime -> item.img
                    is AnimeItem -> item.img
                    else -> ""
                }
                val eps = when (item) {
                    is SpotlightAnime -> item.episodes?.eps
                    is AnimeItem -> item.episodes?.eps
                    else -> null
                }

                AniwatchAnimeCard(
                    id = id,
                    name = name,
                    img = img,
                    episodeCount = eps,
                    onClick = { onAnimeClick(id) }
                )
            }
        }
    }
}

// ====================== Anime Card ======================
@Composable
fun AniwatchAnimeCard(
    id: String,
    name: String,
    img: String,
    episodeCount: Int?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = img,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = name,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        episodeCount?.let { eps ->
            if (eps > 0) {
                Text(
                    text = "Ep $eps",
                    color = Purple40,
                    fontSize = 10.sp
                )
            }
        }
    }
}
