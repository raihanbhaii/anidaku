package com.anilite

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
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
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.anilite.ui.theme.AnidakuTheme
import com.anilite.ui.theme.Purple40
import kotlinx.coroutines.*

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        val episodeId = intent.getStringExtra("episodeId") ?: ""
        val episodeTitle = intent.getStringExtra("episodeTitle") ?: ""

        setContent {
            AnidakuTheme {
                PlayerScreen(
                    episodeId = episodeId,
                    episodeTitle = episodeTitle,
                    onBack = { finish() }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(episodeId: String, episodeTitle: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var category by remember { mutableStateOf("sub") }
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isExtracting by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }

    val embedUrl = "https://megaplay.buzz/stream/s-2/$episodeId/$category"

    // Hidden WebView to intercept m3u8
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val url = request.url.toString()
                        if (url.contains(".m3u8") && streamUrl == null) {
                            streamUrl = url
                            isExtracting = false
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                webChromeClient = WebChromeClient()
                visibility = android.view.View.GONE
                loadUrl(embedUrl)
            }
        },
        update = { webView ->
            webView.loadUrl(embedUrl)
        },
        modifier = Modifier.size(1.dp)
    )

    // Auto hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Reset when category changes
    LaunchedEffect(category) {
        streamUrl = null
        isExtracting = true
        error = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        when {
            isExtracting -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Purple40, strokeWidth = 3.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading stream...", color = Color.White, fontSize = 13.sp)
                    Text(
                        "Fetching from server",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
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
                        onClick = {
                            streamUrl = null
                            isExtracting = true
                            error = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple40)
                    ) {
                        Text("Retry")
                    }
                }
            }

            streamUrl != null -> {
                val player = remember(streamUrl, category) {
                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(
                            mapOf(
                                "Referer" to "https://megaplay.buzz/",
                                "Origin" to "https://megaplay.buzz",
                                "User-Agent" to "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36"
                            )
                        )
                    ExoPlayer.Builder(context).build().apply {
                        val source = HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(streamUrl!!))
                        setMediaSource(source)
                        prepare()
                        playWhenReady = true
                        addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(playing: Boolean) {
                                isPlaying = playing
                            }
                        })
                    }
                }

                DisposableEffect(streamUrl, category) {
                    onDispose { player.release() }
                }

                // ExoPlayer view
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Custom controls overlay
                if (showControls) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000))
                    ) {
                        // Top bar
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    episodeTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            // Sub/Dub toggle
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF1C1C28), RoundedCornerShape(20.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
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
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        // Center playback controls
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { player.seekBack() },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    Icons.Default.Replay10,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (player.isPlaying) player.pause() else player.play()
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Purple40, RoundedCornerShape(32.dp))
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(
                                onClick = { player.seekForward() },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    Icons.Default.Forward10,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Progress bar at bottom
                        var progress by remember { mutableStateOf(0f) }
                        var duration by remember { mutableStateOf(0L) }

                        LaunchedEffect(player) {
                            while (true) {
                                delay(500)
                                if (player.duration > 0) {
                                    progress = player.currentPosition.toFloat() / player.duration
                                    duration = player.duration
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Slider(
                                value = progress,
                                onValueChange = { newVal ->
                                    player.seekTo((newVal * player.duration).toLong())
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Purple40,
                                    activeTrackColor = Purple40,
                                    inactiveTrackColor = Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    formatTime((progress * duration).toLong()),
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                Text(
                                    formatTime(duration),
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
