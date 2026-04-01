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
import com.anilite.data.Episode
import com.anilite.data.RetrofitClient
import com.anilite.data.WatchlistAnime
import com.anilite.data.WatchlistManager
import com.anilite.ui.theme.Purple40
import com.anilite.ui.theme.SurfaceVariant

@Composable
fun AnimeDetailScreen(
    animeId: String,
    onBack: () -> Unit,
    onPlayEpisode: (animeId: String, episodeId: String, title: String) -> Unit
) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<com.anilite.data.AnimeDetail?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var inWatchlist by remember { mutableStateOf(WatchlistManager.isInWatchlist(context, animeId)) }

    LaunchedEffect(animeId) {
        try {
            val detailResp = RetrofitClient.api.getAnimeDetail(animeId)
            detail = detailResp.data?.anime
            val epResp = RetrofitClient.api.getEpisodes(animeId)
            episodes = epResp.data?.episodes ?: emptyList()
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                AsyncImage(
                    model = detail?.info?.poster,
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
                        val info = detail?.info
                        if (inWatchlist) {
                            WatchlistManager.removeFromWatchlist(context, animeId)
                        } else {
                            WatchlistManager.addToWatchlist(
                                context,
                                WatchlistAnime(
                                    id = animeId,
                                    name = info?.name ?: "",
                                    poster = info?.poster ?: "",
                                    type = info?.stats?.type
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
                    text = detail?.info?.name ?: "",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                val stats = detail?.info?.stats
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats?.rating?.let { StatChip(it) }
                    stats?.type?.let { StatChip(it) }
                    stats?.duration?.let { StatChip(it) }
                }
                Spacer(Modifier.height(8.dp))
                detail?.moreInfo?.genres?.let { genres ->
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        genres.forEach { genre ->
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
                detail?.info?.description?.let { desc ->
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        text = desc,
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
            }
        }

        items(episodes, key = { it.episodeId ?: it.number ?: 0 }) { ep ->
            EpisodeRow(
                episode = ep,
                onClick = {
                    ep.epNumericId?.let { numId ->
                        onPlayEpisode(animeId, numId, "Episode ${ep.number}")
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
                text = "${episode.number}",
                color = Purple40,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title ?: "Episode ${episode.number}",
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.isFiller == true) {
                Text("Filler", color = Color(0xFFFF6B6B), fontSize = 10.sp)
            }
        }
        Icon(Icons.Default.PlayArrow, null, tint = Purple40, modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = Color(0xFF1C1C28), thickness = 0.5.dp)
}
