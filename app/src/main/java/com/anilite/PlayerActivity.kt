package com.anilite

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.anilite.data.RetrofitClient
import com.anilite.ui.theme.AnidakuTheme

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val animeId = intent.getStringExtra("animeId") ?: ""
        val ep = intent.getStringExtra("ep") ?: ""

        setContent {
            AnidakuTheme {
                PlayerScreen(animeId = animeId, ep = ep)
            }
        }
    }
}

@Composable
fun PlayerScreen(animeId: String, ep: String) {
    val context = LocalContext.current
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(animeId, ep) {
        try {
            val resp = RetrofitClient.api.getSources(
                animeEpisodeId = animeId,
                ep = ep,
                server = "hd-1",
                category = "sub"
            )
            streamUrl = resp.data?.sources?.firstOrNull()?.url
        } catch (e: Exception) {
            error = "Failed to load stream"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(color = Color(0xFF7C4DFF))
            error != null -> Text(error!!, color = Color.White)
            streamUrl != null -> {
                val player = remember {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(streamUrl!!))
                        prepare()
                        playWhenReady = true
                        repeatMode = Player.REPEAT_MODE_OFF
                    }
                }
                DisposableEffect(Unit) {
                    onDispose { player.release() }
                }
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
