package com.anilite.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import com.anilite.data.AniListAnime
import com.anilite.data.AniListRepository
import com.anilite.ui.theme.Purple40
import com.anilite.ui.theme.SurfaceVariant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy",
    "Horror", "Mystery", "Romance", "Sci-Fi", "Slice of Life",
    "Sports", "Supernatural", "Thriller", "Ecchi", "Mecha"
)

@Composable
fun SearchScreen(onAnimeClick: (aniListId: Int, aniwatchId: String?) -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<AniListAnime>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun doSearch() {
        searchJob?.cancel()
        if (query.isBlank() && selectedGenre == null) {
            results = emptyList()
            return
        }
        searchJob = scope.launch {
            delay(400)
            isLoading = true
            try {
                val searchQuery = if (query.isNotBlank()) query else selectedGenre ?: ""
                results = AniListRepository.search(searchQuery).animes
            } catch (e: Exception) {
                e.printStackTrace()
                results = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Search",
            color = Purple40,
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; doSearch() },
            placeholder = { Text("Search anime...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Purple40) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple40,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Purple40
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GENRES.forEach { genre ->
                val selected = selectedGenre == genre
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedGenre = if (selected) null else genre
                        doSearch()
                    },
                    label = { Text(genre, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Purple40,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceVariant,
                        labelColor = Color(0xFFB0B0C0)
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple40)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.id }) { anime ->
                    AniListSearchCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime.id, null) }
                    )
                }
            }
        }
    }
}

@Composable
fun AniListSearchCard(anime: AniListAnime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = anime.coverImage,
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
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
        // Aired episodes count
        val aired = anime.nextAiringEpisode?.episode?.minus(1) ?: anime.episodes
        aired?.let {
            Text(
                text = "Ep $it",
                color = Purple40,
                fontSize = 10.sp
            )
        }
    }
}
