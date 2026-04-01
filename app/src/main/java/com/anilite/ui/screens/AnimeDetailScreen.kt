package com.anilite.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.anilite.data.AniwatchAnime
import com.anilite.data.AniwatchRepository
import com.anilite.data.WatchlistAnime
import com.anilite.data.WatchlistManager
import com.anilite.ui.theme.Purple40
import com.anilite.ui.theme.SurfaceVariant
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnimeDetailScreen(
    animeId: String,                    // ← Now String (Aniwatch slug)
    onBack: () -> Unit,
    onPlayEpisode: (episodeId: String, episodeTitle: String, episodeNumber: Int) -> Unit
) {
    val context = LocalContext.current

    var anime by remember { mutableStateOf<AniwatchAnime?>(null) }
    var episodes by remember { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var inWatchlist by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(animeId) {
        isLoading = true
        try {
            // Fetch anime details
            val animeResponse = /* Call your detail endpoint */
                // We'll create a proper function in repository later
                // For now, placeholder - replace with actual call
                fetchAnimeDetails(animeId)

            anime = animeResponse.info   // adjust based on actual response

            // Fetch episodes list
            episodes = fetchEpisodes(animeId)

            inWatchlist = WatchlistManager.isInWatchlist(context, animeId)
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

    val currentAnime = anime ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load anime", color = Color.White)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                AsyncImage(
                    model = currentAnime.img,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color(0x660A0A0F), Color(0xFF0A0A0F)), startY = 120f
                        )
                    )
                )
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                IconButton(
                    onClick = {
                        if (inWatchlist) {
                            WatchlistManager.removeFromWatchlist(context, animeId)
                        } else {
                            WatchlistManager.addToWatchlist(
                                context,
                                WatchlistAnime(
                                    id = animeId,
                                    name = currentAnime.name,
                                    img = currentAnime.img,
                                    type = currentAnime.category ?: "TV"
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
                Text(currentAnime.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentAnime.quality?.let { StatChip(it) }
                    currentAnime.duration?.let { StatChip("${it}") }
                    currentAnime.category?.let { StatChip(it.replace("-", " ").uppercase()) }
                }

                Spacer(Modifier.height(12.dp))

                // Description
                currentAnime.description?.takeIf { it.isNotEmpty() }?.let { desc ->
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
                    listOf("Episodes", "Info").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) Purple40 else Color.Gray,
                                    fontSize = 13.sp
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
            0 -> item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (episodes.isNotEmpty()) {
                        Text(
                            "Episodes",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        episodes.forEach { ep ->
                            EpisodeItem(
                                episodeNumber = ep.episodeNo,
                                episodeTitle = ep.name,
                                onClick = {
                                    onPlayEpisode(ep.episodeId, ep.name, ep.episodeNo)
                                }
                            )
                        }
                    } else {
                        Text(
                            "No episodes available",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            1 -> item {
                // More info can be added here later (studios, genres, etc.)
                Text(
                    "More information coming soon...",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// Helper data class for episodes
data class EpisodeItem(
    val name: String,
    val episodeNo: Int,
    val episodeId: String   // full "slug?ep=12345"
)

// Temporary fetch functions - we'll improve this in repository
private suspend fun fetchAnimeDetails(id: String): Any {  // Replace 'Any' with proper response type later
    // TODO: Implement proper call to /aniwatch/anime/{id}
    return Unit // placeholder
}

private suspend fun fetchEpisodes(id: String): List<EpisodeItem> {
    // TODO: Call https://anidexz-api.vercel.app/aniwatch/episodes/{id}
    return emptyList()
}

// Keep your existing helper composables
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
fun EpisodeItem(episodeNumber: Int, episodeTitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(SurfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("$episodeNumber", color = Purple40, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(episodeTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Episode $episodeNumber", color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.Default.PlayArrow, null, tint = Purple40, modifier = Modifier.size(22.dp))
    }
    HorizontalDivider(color = Color(0xFF1C1C28), thickness = 0.5.dp)
}
