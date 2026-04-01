package com.anilite.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.anilite.data.AniListRelatedAnime
import com.anilite.data.AniListRepository
import com.anilite.data.Character
import com.anilite.data.VoiceActor
import com.anilite.data.WatchlistAnime
import com.anilite.data.WatchlistManager
import com.anilite.ui.theme.Purple40
import com.anilite.ui.theme.SurfaceVariant
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnimeDetailScreen(
    aniListId: Int,
    onBack: () -> Unit,
    onPlayEpisode: (episodeId: String, episodeTitle: String, episodeNumber: Int) -> Unit
) {
    val context = LocalContext.current
    var anime by remember { mutableStateOf<AniListAnime?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var inWatchlist by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(aniListId) {
        try {
            anime = AniListRepository.getDetail(aniListId)
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
    val isAiring = info.status == "RELEASING"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                AsyncImage(
                    model = info.bannerImage ?: info.coverImage,
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
                Text(info.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                info.titleEnglish?.takeIf { it != info.title }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = Color.Gray, fontSize = 14.sp)
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    info.averageScore?.let { StatChip("⭐ ${it / 10.0}") }
                    info.format?.let { StatChip(it) }
                    info.duration?.let { StatChip("${it}m") }
                    info.status?.let { StatChip(it.replace("_", " ")) }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    airedEpisodes?.let {
                        EpisodeStatChip(
                            icon = Icons.Default.PlayArrow,
                            text = "$it / ${info.episodes ?: "?"} episodes aired"
                        )
                    }
                    if (isAiring && info.nextAiringEpisode != null) {
                        val nextDate = Date(info.nextAiringEpisode!!.airingAt * 1000)
                        val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
                        EpisodeStatChip(
                            icon = Icons.Default.PlayArrow,
                            text = "Next: Ep ${info.nextAiringEpisode!!.episode} (${fmt.format(nextDate)})"
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

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

                Spacer(Modifier.height(12.dp))

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
                    listOf("Episodes", "Characters", "Voice Actors", "Relations").forEachIndexed { index, title ->
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

        when (selectedTab) {
            0 -> item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (airedEpisodes != null && airedEpisodes > 0) {
                        Text(
                            "Aired Episodes",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        for (i in 1..airedEpisodes.coerceAtMost(50)) {
                            EpisodeItem(episodeNumber = i, onClick = {
                                onPlayEpisode("episode-$aniListId-$i", info.title, i)
                            })
                        }
                        if (airedEpisodes > 50) {
                            Text(
                                "+ ${airedEpisodes - 50} more episodes",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        Text(
                            "No episodes aired yet",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }

            1 -> item {
                if (info.characters.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        info.characters.forEach { character ->
                            CharacterCard(character = character)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    Text(
                        "No character information available",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            2 -> item {
                if (info.voiceActors.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        info.voiceActors.forEach { va ->
                            VoiceActorCard(voiceActor = va)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    Text(
                        "No voice actor information available",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            3 -> item {
                if (info.relations.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        info.relations.forEach { relation ->
                            RelationCard(relation = relation)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    Text(
                        "No related anime available",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
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
fun EpisodeStatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .background(SurfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = Purple40, modifier = Modifier.size(12.dp))
        Text(text, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun EpisodeItem(episodeNumber: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("$episodeNumber", color = Purple40, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Episode $episodeNumber", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Aired", color = Color.Gray, fontSize = 11.sp)
        }
        Icon(Icons.Default.PlayArrow, null, tint = Purple40, modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = Color(0xFF1C1C28), thickness = 0.5.dp)
}

@Composable
fun CharacterCard(character: Character) {
    Column(modifier = Modifier.width(120.dp)) {
        AsyncImage(
            model = character.image,
            contentDescription = character.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)
        )
        Spacer(Modifier.height(4.dp))
        Text(character.name, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun VoiceActorCard(voiceActor: VoiceActor) {
    Column(modifier = Modifier.width(120.dp)) {
        AsyncImage(
            model = voiceActor.image,
            contentDescription = voiceActor.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)
        )
        Spacer(Modifier.height(4.dp))
        Text(voiceActor.name, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        voiceActor.language?.let { Text(it, color = Purple40, fontSize = 10.sp) }
    }
}

@Composable
fun RelationCard(relation: AniListRelatedAnime) {
    Column(modifier = Modifier.width(140.dp)) {
        AsyncImage(
            model = relation.img,
            contentDescription = relation.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)
        )
        Spacer(Modifier.height(4.dp))
        Text(relation.name, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        relation.relationType?.let { Text(it, color = Purple40, fontSize = 10.sp) }
    }
}
