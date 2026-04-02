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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.PathFillType
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import com.anilite.data.*
import com.anilite.ui.theme.Purple40
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Telegram SVG Icon ──────────────────────────────────────────────────────
val TelegramIcon: ImageVector = ImageVector.Builder(
    name = "Telegram",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fillAlpha = 1f,
        fill = androidx.compose.ui.graphics.SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(11.944f, 0f)
        arcTo(12f, 12f, 0f, false, false, 0f, 12f)
        arcTo(12f, 12f, 0f, false, false, 12f, 24f)
        arcTo(12f, 12f, 0f, false, false, 24f, 12f)
        arcTo(12f, 12f, 0f, false, false, 11.944f, 0f)
        close()
        moveTo(18.306f, 7.307f)
        lineTo(15.633f, 19.873f)
        curveTo(15.633f, 19.873f, 15.27f, 20.78f, 14.277f, 20.344f)
        lineTo(8.777f, 16.117f)
        lineTo(6.756f, 15.168f)
        lineTo(3.563f, 14.105f)
        curveTo(3.563f, 14.105f, 3.076f, 13.934f, 3.029f, 13.498f)
        curveTo(2.981f, 13.062f, 3.579f, 12.861f, 3.579f, 12.861f)
        lineTo(17.291f, 7.517f)
        curveTo(17.291f, 7.517f, 18.306f, 7.07f, 18.306f, 7.307f)
        close()
    }
    path(
        fillAlpha = 1f,
        fill = androidx.compose.ui.graphics.SolidColor(Color(0xFF0A0A0F)),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(8.556f, 19.71f)
        curveTo(8.556f, 19.71f, 8.328f, 19.688f, 8.044f, 18.826f)
        lineTo(6.756f, 14.48f)
        lineTo(15.019f, 9.333f)
        curveTo(15.019f, 9.333f, 15.507f, 9.043f, 15.488f, 9.333f)
        curveTo(15.488f, 9.333f, 15.574f, 9.385f, 15.3f, 9.641f)
        lineTo(8.864f, 15.445f)
        lineTo(8.556f, 19.71f)
        close()
    }
}.build()

@Composable
fun HomeScreen(onAnimeClick: (String) -> Unit) {
    val context = LocalContext.current

    var homeData by remember { mutableStateOf<HomeResponse?>(HomeCache.cachedHome) }
    var isLoading by remember { mutableStateOf(HomeCache.cachedHome == null) }
    var errorMsg by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadTrigger by remember { mutableStateOf(0) }

    // Pull to refresh state
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            loadTrigger++
        }
    }

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
            pullRefreshState.endRefresh()
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Purple40)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Title + Telegram button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anidaku",
                    color = Purple40,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                // Telegram community button
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/anidaku"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        imageVector = TelegramIcon,
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
                        .padding(16.dp),
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

            if (data.spotlightAnimes.isNotEmpty()) {
                SpotlightCarousel(
                    animes = data.spotlightAnimes,
                    onAnimeClick = onAnimeClick
                )
                Spacer(Modifier.height(16.dp))
            }

            if (data.trendingAnimes.isNotEmpty()) {
                AniwatchAnimeRow("Trending", data.trendingAnimes, onAnimeClick)
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
                        .padding(32.dp),
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
                            "Pull down to refresh",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        // Pull to refresh indicator
        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = Color(0xFF1C1C28),
            contentColor = Purple40
        )
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xDD0A0A0F)),
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
                            .background(
                                if (i == pagerState.currentPage) Purple40 else Color.Gray
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
                    is SpotlightAnime -> item.episodes?.epsInt
                    is AnimeItem -> item.episodes?.epsInt
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
