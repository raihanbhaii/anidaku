package com.anilite

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.anilite.data.RetrofitClient
import com.anilite.ui.theme.AnidakuTheme
import com.anilite.ui.theme.Purple40

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val animeId = intent.getStringExtra("animeId") ?: ""
        val ep = intent.getStringExtra("ep") ?: ""

        setContent {
            AnidakuTheme {
                PlayerScreen(
                    animeId = animeId,
                    ep = ep,
                    onBack = { finish() }
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(animeId: String, ep: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("sub") }
    var server by remember { mutableStateOf("hd-1") }

    suspend fun loadSource() {
        isLoading = true
        error = null
        streamUrl = null
        try {
            // Try hd-1 first, fallback to hd-2
            val servers = listOf("hd-1", "hd-2", "hd-3")
            var found = false
            for (s in servers) {
                try {
                    val resp = RetrofitClient.api.getSources(
                        animeEpisodeId = animeId,
                        ep = ep,
                        server = s,
                        category = category
                    )
                    val url = resp.data?.sources?.firstOrNull { it.url?.contains(".m3u8") == true }?.url
                        ?: resp.data?.sources?.firstOrNull()?.url
                    if (url != null) {
                        streamUrl = url
                        server = s
                        found = true
                        break
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            if (!found) error = "No stream found for this episode"
        } catch (e: Exception) {
            error = "Failed to load: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(animeId, ep, category) {
        loadSource()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Purple40)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading stream...", color = Color.White, fontSize = 13.sp)
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚠️", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = Color.White, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { /* reload triggered by state change */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple40)
                    ) {
                        Text("Try other server")
                    }
                }
            }
            streamUrl != null -> {
                val player = remember(streamUrl) {
                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(
                            mapOf(
                                "Referer" to "https://hianime.to/",
                                "Origin" to "https://hianime.to"
                            )
                        )
                    ExoPlayer.Builder(context).build().apply {
                        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(streamUrl!!))
                        setMediaSource(mediaSource)
                        prepare()
                        playWhenReady = true
                    }
                }

                DisposableEffect(streamUrl) {
                    onDispose { player.release() }
                }

                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            this.player = player
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top bar with back + sub/dub toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            // Sub/Dub toggle
            Row(
                modifier = Modifier
                    .background(Color(0x99000000), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("sub", "dub").forEach { cat ->
                    val selected = category == cat
                    Text(
                        text = cat.uppercase(),
                        color = if (selected) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .background(
                                if (selected) Purple40 else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { category = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
