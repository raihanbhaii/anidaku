package com.anilite

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(episodeId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var streamResponse by remember { mutableStateOf<StreamResponse?>(null) }
    var isLoading      by remember { mutableStateOf(true) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }
    var isFullscreen   by remember { mutableStateOf(false) }
    var isPlaying      by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlayerError(error: PlaybackException) {
                    errorMessage = "Playback error: ${error.message}"
                    isLoading = false
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) isLoading = false
                }
            })
        }
    }

    // Update current position periodically for skip intro button
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            kotlinx.coroutines.delay(500) // Update every 500ms
        }
    }

    LaunchedEffect(episodeId) {
        isLoading = true
        errorMessage = null
        try {
            streamResponse = ApiClient.getStream(episodeId)
            val url = streamResponse?.streams?.firstOrNull()?.url
            if (url != null) {
                val mediaItem = MediaItem.fromUri(url)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            } else {
                errorMessage = "No stream available"
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load stream: ${e.message}"
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            // restore portrait
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(isFullscreen) {
        (context as? Activity)?.requestedOrientation =
            if (isFullscreen) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Video player ───────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Loading overlay ────────────────────────────────────────────────────
        if (isLoading) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BrandPurple, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Loading stream...", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // ── Error overlay ──────────────────────────────────────────────────────
        if (errorMessage != null) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("⚠", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Playback Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMessage ?: "", 
                        color = Color.Gray, 
                        fontSize = 13.sp, 
                        textAlign = TextAlign.Center // Fixed: Using proper TextAlign import
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Go Back", color = Color.White)
                    }
                }
            }
        }

        // ── Top controls ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(0.6f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Stream quality indicator
            streamResponse?.streams?.firstOrNull()?.quality?.let { quality ->
                Box(
                    Modifier
                        .background(BrandPurple.copy(0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(quality, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(onClick = { isFullscreen = !isFullscreen }) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // ── Skip intro button ─────────────────────────────────────────────────
        streamResponse?.intro?.let { intro ->
            val currentPosSeconds = currentPosition / 1000L // Fixed: Convert to seconds as Long
            if (currentPosSeconds >= intro.start && currentPosSeconds <= intro.end) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 72.dp, end = 16.dp)
                        .clip(RoundedCornerShape(6.dp)) // Fixed: Using proper clip import
                        .background(BrandPurple)
                        .clickable { exoPlayer.seekTo(intro.end.toLong() * 1000L) } // Fixed: Use Long multiplication
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Skip Intro ›", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
