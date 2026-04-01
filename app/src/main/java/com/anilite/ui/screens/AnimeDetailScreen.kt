package com.anilite.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anilite.data.AniListAnime
import com.anilite.data.AniListRepository
import com.anilite.data.Episode
import com.anilite.data.RetrofitClient
import com.anilite.data.WatchlistAnime
import com.anilite.data.WatchlistManager
import com.anilite.ui.theme.Purple40
import com.anilite.ui.theme.SurfaceVariant

@Composable
fun AnimeDetailScreen(
    aniListId: Int,
    aniwatchId: String?,
    onBack: () -> Unit,
    onPlayEpisode: (aniwatchId: String, episodeId: String, title: String) -> Unit
) {
    val context = LocalContext.current
    var anime by remember { mutableStateOf<AniListAnime?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var resolvedAniwatchId by remember { mutableStateOf(aniwatchId) }
    var isLoading by remember { mutableStateOf(true) }
    var inWatchlist by remember { mutableStateOf(false) }

    LaunchedEffect(aniListId) {
        try {
            // 1. Get full detail from AniList
            anime = AniListRepository.getDetail(aniListId)

            // 2. If no aniwatchId passed, search old API by title
            if (resolvedAniwatchId == null && anime != null) {
                try {
                    val searchResult = RetrofitClient.api.search(
                        query = anime?.title ?: "",
                        page = 1
                    )
                    resolvedAniwatchId = searchResult.animes.firstOrNull()?.id
                } catch (_: Exception) {}
            }

            // 3. Get episodes from old API - FIXED: removed .episodes
            resolvedAniwatchId?.let { slug ->
                val episodesResponse = AniListRepository.getEpisodes(slug)
                episodes = episodesResponse.episodes
            }

            inWatchlist = WatchlistManager.isInWatchlist(context, aniListId.toString())
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

    val info = anime ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load anime", color = Color.White)
        }
        return
    }

    val airedEpisodes = info.nextAiringEpisode?.episode?.minus(1) ?: info.episodes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                AsyncImage(
                    model = info.bannerImage ?: info.coverImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color(0x660A0A0F), Color(0xFF0A0A0F)),
                            startY = 100f
                        )
                    )
                )
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                IconButton(
                    onClick = {
                        if (inWatchlist) {
                            WatchlistManager.removeFromWatchlist(context, aniListId.toString())
                        } else {
                            WatchlistManager.addToWatchlist(
                                context,
                                WatchlistAnime(
                                    id = aniListId.toString(),
                                    name = info.title,
                                    img = info.coverImage,
                                    type = info.format ?: "TV"
                                )
                            )
                        }
                        inWatchlist = !inWatchlist
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(
                        if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        null,
                        tint = if (inWatchlist) Purple40 else Color.White
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = info.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info.averageScore?.let { StatChip("⭐ ${it / 10.0}") }
                    info.format?.let { StatChip(it) }
                    info.duration?.let { StatChip("${it}m") }
                    info.status?.let { StatChip(it.replace("_", " ")) }
                }

                Spacer(Modifier.height(6.dp))

                airedEpisodes?.let {
                    Text(
                        text = "Episodes aired: $it" +
                            (info.episodes?.let { total -> " / $total" } ?: ""),
                        color = Purple40,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (info.genres.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        info.genres.forEach { genre ->
                            Text(
                                text = genre,
                                color = Purple40,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .border(1.dp, Purple40, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                info.description?.takeIf { it.isNotEmpty() }?.let { desc ->
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        text = desc.replace("<br>", "\n").replace("<[^>]*>".toRegex(), ""),
                        color = Color(0xFFB0B0C0),
                        fontSize = 12.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
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

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Episodes (${episodes.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (resolvedAniwatchId == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Episode list unavailable for this title",
                        color = Color(0xFFB0B0C0),
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(episodes, key = { it.episodeId }) { ep ->
            EpisodeRow(
                episode = ep,
                onClick = {
                    resolvedAniwatchId?.let { slug ->
                        onPlayEpisode(slug, ep.episodeId, "Episode ${ep.episodeNo}")
                    }
                }
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun StatChip(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .background(SurfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Composable
fun EpisodeRow(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(SurfaceVariant, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${episode.episodeNo}",
                color = Purple40,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.name ?: "Episode ${episode.episodeNo}",
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.filler) {
                Text("Filler", color = Color(0xFFFF6B6B), fontSize = 10.sp)
            }
        }
        Icon(Icons.Default.PlayArrow, null, tint = Purple40, modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = Color(0xFF1C1C28), thickness = 0.5.dp)
}
