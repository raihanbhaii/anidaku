package com.anilite.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anilite.data.*
import com.anilite.ui.theme.Purple40
import com.anilite.ui.theme.SurfaceVariant

@Composable
fun AnimeDetailScreen(
    animeId: String,
    onBack: () -> Unit,
    onPlayEpisode: (playerUrl: String, episodeTitle: String, episodeNumber: Int) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AniwatchRepository() }

    var animeDetail by remember { mutableStateOf<AnimeDetailResponse?>(null) }
    var episodesResponse by remember { mutableStateOf<EpisodesResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var inWatchlist by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var loadTrigger by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("sub") }

    LaunchedEffect(animeId, loadTrigger) {
        if (animeId.isBlank()) {
            errorMsg = "Invalid anime ID"
            isLoading = false
            return@LaunchedEffect
        }

        if (loadTrigger == 0 && AnimeDetailCache.isCacheValid(animeId)) {
            animeDetail = AnimeDetailCache.getDetail(animeId)
            episodesResponse = AnimeDetailCache.getEpisodes(animeId)
            inWatchlist = WatchlistManager.isInWatchlist(context, animeId)
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMsg = ""

        try {
            val detail = repository.getAnimeDetails(animeId)
            val episodes = repository.getEpisodes(animeId)
            if (detail != null) {
                AnimeDetailCache.save(animeId, detail, episodes)
            }
            animeDetail = detail
            episodesResponse = episodes
            inWatchlist = WatchlistManager.isInWatchlist(context, animeId)
        } catch (e: Exception) {
            errorMsg = "Failed to load: ${e.message ?: e::class.simpleName}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Purple40)
                Spacer(Modifier.height(12.dp))
                Text("Loading anime...", color = Color.Gray, fontSize = 13.sp)
            }
        }
        return
    }

    if (errorMsg.isNotEmpty() || animeDetail?.info == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text("😕", fontSize = 40.sp)
                Text(
                    text = errorMsg.ifEmpty { "Failed to load anime" },
                    color = Color.Red,
                    fontSize = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Go Back", color = Color.Gray)
                    }
                    Button(
                        onClick = { loadTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple40)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        return
    }

    val info = animeDetail!!.info!!
    val episodeList = episodesResponse?.episodes ?: emptyList()
    val hasDub = (info.episodes?.dubInt ?: 0) > 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                AsyncImage(
                    model = info.img,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x660A0A0F), Color(0xFF0A0A0F)),
                                startY = 120f
                            )
                        )
                )

                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }

                IconButton(
                    onClick = {
                        val watchlistAnime = WatchlistAnime(
                            id = animeId,
                            name = info.name,
                            img = info.img,
                            type = info.category
                        )
                        if (inWatchlist) {
                            WatchlistManager.removeFromWatchlist(context, animeId)
                        } else {
                            WatchlistManager.addToWatchlist(context, watchlistAnime)
                        }
                        inWatchlist = !inWatchlist
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        null,
                        tint = if (inWatchlist) Purple40 else Color.White
                    )
                }

                info.episodes?.epsInt?.let { eps ->
                    if (eps > 0) {
                        Text(
                            text = "Ep $eps",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                                .background(Purple40, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = info.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info.quality.takeIf { it.isNotBlank() }?.let { StatChip(it) }
                    info.duration.takeIf { it.isNotBlank() }?.let { StatChip(it) }
                    info.category.takeIf { it.isNotBlank() }?.let {
                        StatChip(it.replace("-", " ").uppercase())
                    }
                    info.rating.takeIf { it.isNotBlank() }?.let { StatChip(it) }
                }

                Spacer(Modifier.height(12.dp))

                if (info.description.isNotBlank()) {
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        text = info.description
                            .replace("<br>", "\n")
                            .replace("<[^>]*>".toRegex(), ""),
                        color = Color(0xFFB0B0C0),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                    Text(
                        text = if (expanded) "Show less" else "Read more",
                        color = Purple40,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Audio", color = Color.Gray, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1C1C28), RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("sub", "dub").forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val isDisabled = cat == "dub" && !hasDub
                            Text(
                                text = cat.uppercase(),
                                color = when {
                                    isDisabled -> Color(0xFF555555)
                                    isSelected -> Color.White
                                    else -> Color.Gray
                                },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .background(
                                        if (isSelected) Purple40 else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .then(
                                        if (!isDisabled) Modifier.clickable {
                                            selectedCategory = cat
                                        } else Modifier
                                    )
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                if (!hasDub) {
                    Text(
                        "Dub not available for this anime",
                        color = Color(0xFF555555),
                        fontSize = 11.sp
                    )
                } else {
                    val subCount = info.episodes?.subInt ?: 0
                    val dubCount = info.episodes?.dubInt ?: 0
                    Text(
                        "SUB: $subCount eps  •  DUB: $dubCount eps",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Purple40
                ) {
                    listOf("Episodes", "More Info").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) Purple40 else Color.Gray
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        when (selectedTab) {
            0 -> {
                if (episodeList.isNotEmpty()) {
                    item {
                        Text(
                            "All Episodes (${episodeList.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(episodeList) { ep ->
                        EpisodeItem(
                            episode = ep,
                            onClick = {
                                val playerUrl =
                                    "https://megaplay.buzz/stream/s-2/${ep.episodeId}/$selectedCategory"
                                onPlayEpisode(
                                    playerUrl,
                                    ep.name ?: "Episode ${ep.episodeNo}",
                                    ep.episodeNo
                                )
                            }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No episodes available yet",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            1 -> {
                animeDetail?.moreInfo?.let { more ->
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "More Information",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            more.japanese?.let { InfoRow("Japanese", it) }
                            more.aired?.let { InfoRow("Aired", it) }
                            more.premiered?.let { InfoRow("Premiered", it) }
                            more.status?.let { InfoRow("Status", it) }
                            more.studios?.let { InfoRow("Studios", it) }
                            more.malScore?.let { InfoRow("MAL Score", it) }
                            more.duration?.let { InfoRow("Duration", it) }
                            if (more.genres.isNotEmpty()) {
                                InfoRow("Genres", more.genres.joinToString(", "))
                            }
                            if (more.producers.isNotEmpty()) {
                                InfoRow("Producers", more.producers.joinToString(", "))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.6f)
        )
    }
    HorizontalDivider(color = Color(0xFF1C1C28), thickness = 0.5.dp)
}

@Composable
fun EpisodeItem(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(SurfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${episode.episodeNo}",
                color = Purple40,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.name ?: "Episode ${episode.episodeNo}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Episode ${episode.episodeNo}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Icon(
            Icons.Default.PlayArrow,
            null,
            tint = Purple40,
            modifier = Modifier.size(22.dp)
        )
    }
    HorizontalDivider(color = Color(0xFF1C1C28), thickness = 0.5.dp)
}

@Composable
fun StatChip(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .background(SurfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
