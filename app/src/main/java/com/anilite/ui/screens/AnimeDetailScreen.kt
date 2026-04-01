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
import com.anilite.data.*
import com.anilite.ui.theme.Purple40
import com.anilite.ui.theme.SurfaceVariant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ==================== DATA CLASSES (Add these in com.anilite.data package) ====================

@Serializable
data class AnimeDetailResponse(
    val info: AnimeInfo? = null
)

@Serializable
data class AnimeInfo(
    val id: String? = null,
    val name: String? = null,
    val img: String? = null,
    val poster: String? = null,
    val description: String? = null,
    val category: String? = null,     // e.g. "TV", "Movie"
    val quality: String? = null,
    val duration: String? = null,
    val type: String? = null,
    val status: String? = null,
    val releaseDate: String? = null,
    val genres: List<String>? = null,
    @Contextual
    val episodes: JsonElement? = null   // Safe for complex episodes data
)

@Serializable
data class Episode(
    val name: String? = null,
    val episodeNo: Int = 0,
    val episodeId: String = ""   // e.g. "one-piece-100?ep=12345"
)

// ==================== MAIN SCREEN ====================

@Composable
fun AnimeDetailScreen(
    animeId: String,                    // Changed to String (recommended)
    onBack: () -> Unit,
    onPlayEpisode: (playerUrl: String, episodeTitle: String, episodeNumber: Int) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AniwatchRepository() }

    var animeDetail by remember { mutableStateOf<AnimeDetailResponse?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var inWatchlist by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(animeId) {
        if (animeId.isBlank()) {
            errorMsg = "Invalid anime ID"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMsg = ""

        try {
            val detailResponse = repository.getAnimeDetails(animeId)
            animeDetail = if (detailResponse != null) {
                AnimeDetailResponse(info = detailResponse)   // Adapt if your API returns flat structure
            } else null

            val rawEpisodes = repository.getEpisodes(animeId)
            // TODO: Convert raw episodes (List<Any> or JsonElement) to List<Episode> if needed
            // For now, keep simple (you may need custom parsing later)
            episodes = emptyList() // Replace with proper mapping once you see the response

            inWatchlist = WatchlistManager.isInWatchlist(context, animeId)
        } catch (e: Exception) {
            errorMsg = "Failed to load anime: ${e.message ?: e::class.simpleName}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // Loading State
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Purple40)
        }
        return
    }

    // Error State
    if (errorMsg.isNotEmpty() || animeDetail?.info == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = errorMsg.ifEmpty { "Failed to load anime details" },
                    color = Color.Red
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { /* TODO: Add retry logic */ }) {
                    Text("Retry")
                }
            }
        }
        return
    }

    val info = animeDetail!!.info!!
    val episodeList = episodes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with Banner
        item {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                AsyncImage(
                    model = info.img ?: info.poster,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color(0x660A0A0F), Color(0xFF0A0A0F)),
                            startY = 120f
                        )
                    )
                )

                // Back Button
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }

                // Watchlist Button
                IconButton(
                    onClick = {
                        val watchlistAnime = WatchlistAnime(
                            id = animeId,
                            name = info.name.orEmpty(),
                            img = info.img.orEmpty(),
                            type = info.category.orEmpty()
                        )
                        if (inWatchlist) {
                            WatchlistManager.removeFromWatchlist(context, animeId)
                        } else {
                            WatchlistManager.addToWatchlist(context, watchlistAnime)
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

        // Title & Info
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = info.name.orEmpty(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info.quality?.let { StatChip(it) }
                    info.duration?.let { StatChip(it) }
                    info.category?.let { StatChip(it.replace("-", " ").uppercase()) }
                }

                Spacer(Modifier.height(12.dp))

                // Description
                info.description?.takeIf { it.isNotEmpty() }?.let { desc ->
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        text = desc.replace("<br>", "\n").replace("<[^>]*>".toRegex(), ""),
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

        // Episodes Tab
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
                                val epNumber = ep.episodeId.substringAfter("?ep=", "").toIntOrNull() ?: ep.episodeNo
                                val playerUrl = "https://megaplay.buzz/stream/s-2/${epNumber}/sub"
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
                        Text(
                            "No episodes available yet",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            1 -> { // More Info Tab
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "More Information",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "More details (studios, genres, etc.) will be added soon...",
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

// Episode Item Composable (unchanged)
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
