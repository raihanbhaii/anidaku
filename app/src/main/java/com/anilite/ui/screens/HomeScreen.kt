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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anilite.data.*
import com.anilite.ui.theme.Purple40
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAnimeClick: (String) -> Unit) {
    val context = LocalContext.current

    var homeData by remember { mutableStateOf<HomeResponse?>(HomeCache.cachedHome) }
    var isLoading by remember { mutableStateOf(HomeCache.cachedHome == null) }
    var errorMsg by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadTrigger by remember { mutableStateOf(0) }

    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(loadTrigger) {
        if (loadTrigger == 0 && HomeCache.isCacheValid()) {
            homeData = HomeCache.cachedHome
            isLoading = false
            return@LaunchedEffect
        }

        if (loadTrigger > 0) isRefreshing = true
        else isLoading = true

        errorMsg = ""
        try {
            val fresh = AniApiService.getHome()
            HomeCache.save(fresh)
            homeData = fresh
        } catch (e: Exception) {
            if (HomeCache.cachedHome == null) {
                errorMsg = "${e::class.simpleName}: ${e.message ?: "unknown error"}"
            }
            e.printStackTrace()
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Purple40)
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { loadTrigger++ },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header with title + community button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anidaku",
                    color = Purple40,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )

                IconButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://t.me/anidaku")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Community",
                        tint = Purple40,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (errorMsg.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Failed to load anime",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(errorMsg, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { loadTrigger++ },
                            colors = ButtonDefaults.buttonColors(containerColor = Purple40),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                return@Column
            }

            val data = homeData ?: return@Column

            if (data.spotlightAnimes.isNotEmpty()) {
                SpotlightCarousel(
                    animes = data.spotlightAnimes,
                    onAnimeClick = onAnimeClick
                )
                Spacer(Modifier.height(20.dp))
            }

            if (data.trendingAnimes.isNotEmpty()) {
                AniwatchAnimeRow("Trending Now", data.trendingAnimes, onAnimeClick)
            }

            data.featuredAnimes?.topAiringAnimes?.let {
                if (it.isNotEmpty()) AniwatchAnimeRow("Currently Airing", it, onAnimeClick)
            }

            data.featuredAnimes?.mostPopularAnimes?.let {
                if (it.isNotEmpty()) AniwatchAnimeRow("Most Popular", it, onAnimeClick)
            }

            if (data.topUpcomingAnimes.isNotEmpty()) {
                AniwatchAnimeRow("Upcoming", data.topUpcomingAnimes, onAnimeClick)
            }

            if (data.spotlightAnimes.isEmpty() && data.trendingAnimes.isEmpty() &&
                data.topUpcomingAnimes.isEmpty() && errorMsg.isEmpty()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "No anime available",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Pull down to refresh",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun SpotlightCarousel(
    animes: List<SpotlightAnime>,
    onAnimeClick: (String) -> Unit
) {
    if (animes.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { animes.size })
    val scope = rememberCoroutineScope()

    // Auto-scroll every 4 seconds, but stop when user interacts
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { isScrolling ->
            if (!isScrolling) {
                delay(4000)
                val next = (pagerState.currentPage + 1) % animes.size
                scope.launch { pagerState.animateScrollToPage(next) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(8.dp, RoundedCornerShape(16.dp), clip = false)
    ) {
        HorizontalPager(state = pagerState) { page ->
            val anime = animes[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAnimeClick(anime.id) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(anime.img)
                        .crossfade(true)
                        .build(),
                    contentDescription = anime.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onLoading = { /* optional shimmer placeholder */ },
                    onError = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                )

                // Gradient overlay for better text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.0f),
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 100f
                            )
                        )
                )

                // Episode badge (top-left)
                anime.episodes?.epsInt?.takeIf { it > 0 }?.let { eps ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Purple40,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "Ep $eps",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                // Title and info (bottom)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = anime.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.shadow(2.dp)
                    )
                    val info = listOfNotNull(anime.category, anime.duration, anime.quality)
                    if (info.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = info.joinToString(" • "),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.shadow(2.dp)
                        )
                    }
                }
            }
        }

        // Page indicators (dots)
        if (animes.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(animes.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 10.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (index == pagerState.currentPage)
                                    Purple40
                                else
                                    Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun AniwatchAnimeRow(
    title: String,
    animes: List<Any>,
    onAnimeClick: (String) -> Unit
) {
    if (animes.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = animes,
                key = { item ->
                    when (item) {
                        is SpotlightAnime -> "spotlight_${item.id}"
                        is BasicAnime -> "basic_${item.id}"
                        is AnimeItem -> "anime_${item.id}"
                        else -> "unknown_${System.currentTimeMillis()}"
                    }
                }
            ) { item ->
                val (id, name, img, eps) = when (item) {
                    is SpotlightAnime -> listOf(item.id, item.name, item.img, item.episodes?.epsInt)
                    is BasicAnime -> listOf(item.id, item.name, item.img, null)
                    is AnimeItem -> listOf(item.id, item.name, item.img, item.episodes?.epsInt)
                    else -> listOf("", "", "", null)
                }

                AniwatchAnimeCard(
                    id = id.toString(),
                    name = name.toString(),
                    img = img.toString(),
                    episodeCount = eps as? Int,
                    onClick = { onAnimeClick(id.toString()) }
                )
            }
        }
    }
}

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
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(10.dp))
                .shadow(4.dp, RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(img)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        episodeCount?.takeIf { it > 0 }?.let { eps ->
            Text(
                text = "$eps eps",
                color = Purple40,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
