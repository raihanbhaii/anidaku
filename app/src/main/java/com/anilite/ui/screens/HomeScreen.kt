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
import com.anilite.data.AniListAnime
import com.anilite.data.AniListRepository
import com.anilite.ui.theme.Purple40
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onAnimeClick: (aniListId: Int, aniwatchId: String?) -> Unit) {
    var trending by remember { mutableStateOf<List<AniListAnime>>(emptyList()) }
    var airing by remember { mutableStateOf<List<AniListAnime>>(emptyList()) }
    var popular by remember { mutableStateOf<List<AniListAnime>>(emptyList()) }
    var upcoming by remember { mutableStateOf<List<AniListAnime>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val trendingResult = AniListRepository.getTrending()
            trending = trendingResult.animes
            
            val airingResult = AniListRepository.getAiring()
            airing = airingResult.animes
            
            val popularResult = AniListRepository.getPopular()
            popular = popularResult.animes
            
            val upcomingResult = AniListRepository.getUpcoming()
            upcoming = upcomingResult.animes
            
        } catch (e: Exception) {
            e.printStackTrace()
            trending = emptyList()
            airing = emptyList()
            popular = emptyList()
            upcoming = emptyList()
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

        if (trending.isNotEmpty()) {
            SpotlightCarousel(
                animes = trending.take(10),
                onAnimeClick = { onAnimeClick(it.id, null) }
            )
            Spacer(Modifier.height(16.dp))
        }

        if (trending.isNotEmpty()) {
            AniListAnimeRow(title = "Trending", animes = trending, onAnimeClick = { onAnimeClick(it.id, null) })
        }
        
        if (airing.isNotEmpty()) {
            AniListAnimeRow(title = "Currently Airing", animes = airing, onAnimeClick = { onAnimeClick(it.id, null) })
        }
        
        if (popular.isNotEmpty()) {
            AniListAnimeRow(title = "Most Popular", animes = popular, onAnimeClick = { onAnimeClick(it.id, null) })
        }
        
        if (upcoming.isNotEmpty()) {
            AniListAnimeRow(title = "Upcoming", animes = upcoming, onAnimeClick = { onAnimeClick(it.id, null) })
        }

        if (trending.isEmpty() && airing.isEmpty() && popular.isEmpty() && upcoming.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No anime available\nCheck your internet connection",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun SpotlightCarousel(animes: List<AniListAnime>, onAnimeClick: (AniListAnime) -> Unit) {
    if (animes.isEmpty()) return
    
    val pagerState = rememberPagerState { animes.size }
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
                    .clickable { onAnimeClick(anime) }
            ) {
                AsyncImage(
                    model = anime.bannerImage?.takeIf { it.isNotBlank() } ?: anime.coverImage,
                    contentDescription = anime.title,
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
                        text = anime.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val info = listOfNotNull(
                        anime.format,
                        anime.status?.replace("_", " "),
                        anime.duration?.let { "${it}m" }
                    ).take(3)
                    if (info.isNotEmpty()) {
                        Text(
                            text = info.joinToString(" • "),
                            color = Color(0xFFB0B0C0),
                            fontSize = 11.sp
                        )
                    }
                }
                val aired = anime.nextAiringEpisode?.let { it.episode - 1 } ?: anime.episodes
                aired?.let {
                    if (it > 0) {
                        Text(
                            text = "Ep $it",
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

        if (animes.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
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

@Composable
fun AniListAnimeRow(
    title: String,
    animes: List<AniListAnime>,
    onAnimeClick: (AniListAnime) -> Unit
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
            items(animes, key = { it.id }) { anime ->
                AniListAnimeCard(anime = anime, onClick = { onAnimeClick(anime) })
            }
        }
    }
}

@Composable
fun AniListAnimeCard(anime: AniListAnime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = anime.coverImage,
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = anime.title,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val aired = anime.nextAiringEpisode?.let { it.episode - 1 } ?: anime.episodes
        aired?.let {
            if (it > 0) {
                Text(
                    text = "Ep $it aired",
                    color = Purple40,
                    fontSize = 10.sp
                )
            }
        }
    }
}
