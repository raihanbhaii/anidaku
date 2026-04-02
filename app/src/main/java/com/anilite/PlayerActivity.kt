package com.anilite

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView
import com.anilite.ui.theme.AnidakuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class StreamData(
    val m3u8Url: String,
    val referer: String,
    val introEnd: Long,
    val subtitleUrl: String?
)

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val episodeId = intent.getStringExtra("episodeId") ?: ""
        val category = intent.getStringExtra("category") ?: "sub"
        val episodeTitle = intent.getStringExtra("episodeTitle") ?: ""
        val episodeNumber = intent.getIntExtra("episodeNumber", 0)

        setContent {
            AnidakuTheme {
                PlayerScreen(
                    episodeId = episodeId,
                    category = category,
                    episodeTitle = episodeTitle,
                    episodeNumber = episodeNumber,
                    onBack = { finish() }
                )
            }
        }
    }
}

suspend fun fetchStreamData(episodeId: String, category: String): StreamData {
    val animeEpisodeId = episodeId.substringBefore("?ep=")
    val ep = episodeId.substringAfterLast("ep=")

    val url = "https://anidaku-api.vercel.app/api/v2/hianime/episode/sources" +
        "?animeEpisodeId=$animeEpisodeId&ep=$ep&server=hd-1&category=$category"

    val response = withContext(Dispatchers.IO) {
        URL(url).openConnection().apply {
            setRequestProperty("User-Agent", "Mozilla/5.0")
            connectTimeout = 15000
            readTimeout = 15000
        }.getInputStream().bufferedReader().readText()
    }

    val json = JSONObject(response)
    val m3u8 = json.getString("source")
    val referer = json.optString("refer", "https://megacloud.club/")

    val intro = json.optJSONObject("skip")
        ?.optJSONObject("intro")
        ?.optLong("end", 0L) ?: 0L

    val subtitleUrl = json.optJSONArray("tracks")?.let { tracks ->
        (0 until tracks.length()).map { tracks.getJSONObject(it) }
            .firstOrNull {
                it.optBoolean("default") &&
                it.optString("kind") == "captions"
            }?.optString("file")
    }

    return StreamData(
        m3u8Url = m3u8,
        referer = referer,
        introEnd = intro * 1000L,
        subtitleUrl = subtitleUrl
    )
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    episodeId: String,
    category: String,
    episodeTitle: String,
    episodeNumber: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var streamData by remember { mutableStateOf<StreamData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var showControls by remember { mutableStateOf(true) }
    var showSkipIntro by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }

    LaunchedEffect(episodeId, category) {
        isLoading = true
        errorMsg = ""
        try {
            streamData = fetchStreamData(episodeId, category)
        } catch (e: Exception) {
            try {
                val animeEpisodeId = episodeId.substringBefore("?ep=")
                val ep = episodeId.substringAfterLast("ep=")
                val url = "https://anidaku-api.vercel.app/api/v2/hianime/episode/sources" +
                    "?animeEpisodeId=$animeEpisodeId&ep=$ep&server=hd-2&category=$category"
                val response = withContext(Dispatchers.IO) {
                    URL(url).openConnection().apply {
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                        connectTimeout = 15000
                        readTimeout = 15000
                    }.getInputStream().bufferedReader().readText()
                }
                val json = JSONObject(response)
                streamData = StreamData(
                    m3u8Url = json.getString("source"),
                    referer = json.optString("refer", "https://megacloud.club/"),
                    introEnd = (json.optJSONObject("skip")
                        ?.optJSONObject("intro")
                        ?.optLong("end", 0L) ?: 0L) * 1000L,
                    subtitleUrl = null
                )
            } catch (e2: Exception) {
                errorMsg = "Failed to load stream: ${e2.message}"
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    val exoPlayer = remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(streamData) {
        val data = streamData ?: return@LaunchedEffect
        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(mapOf(
                "Referer" to data.referer,
                "Origin" to data.referer.trimEnd('/'),
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ))
        }

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(
                MediaItem.Builder()
                    .setUri(data.m3u8Url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            )

        val player = ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    errorMsg = "Playback error: ${error.message}"
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {}
            })
        }
        exoPlayer.value = player
    }

    LaunchedEffect(exoPlayer.value) {
        val player = exoPlayer.value ?: return@LaunchedEffect
        while (true) {
            delay(500)
            val pos = player.currentPosition
            currentPosition = pos
            val introEnd = streamData?.introEnd ?: 0L
            showSkipIntro = introEnd > 0 && pos < introEnd && pos > 0
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.value?.release()
            (context as? Activity)?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF9B59F5))
                    Spacer(Modifier.height(12.dp))
                    Text("Loading stream...", color = Color.White, fontSize = 14.sp)
                }
            }

            errorMsg.isNotEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(errorMsg, color = Color.Red, fontSize = 13.sp)
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59F5))
                    ) { Text("Go Back") }
                }
            }

            streamData != null && exoPlayer.value != null -> {
                // ExoPlayer View
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer.value
                            useController = false
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer.value
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )

                // FIX: Use Modifier.clickable correctly on a Box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showControls = !showControls }
                )

                // Controls overlay
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
                                .padding(12.dp)
                                .align(Alignment.TopStart),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    episodeTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Episode $episodeNumber • ${category.uppercase()}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Center play/pause + seek
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    exoPlayer.value?.seekTo(
                                        (exoPlayer.value!!.currentPosition - 10000).coerceAtLeast(0)
                                    )
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    Icons.Default.Replay10,
                                    contentDescription = "-10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            val isPlaying = exoPlayer.value?.isPlaying == true
                            IconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer.value?.pause()
                                    else exoPlayer.value?.play()
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                // FIX: Use Icons.Default.Pause and Icons.Default.PlayArrow directly
                                Icon(
                                    if (isPlaying) Icons.Default.Pause
                                    else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    exoPlayer.value?.seekTo(
                                        exoPlayer.value!!.currentPosition + 10000
                                    )
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    Icons.Default.Forward10,
                                    contentDescription = "+10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Bottom progress bar
                        val duration = exoPlayer.value?.duration?.takeIf { it > 0 } ?: 1L
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Slider(
                                value = currentPosition.toFloat() / duration.toFloat(),
                                onValueChange = { fraction ->
                                    exoPlayer.value?.seekTo((fraction * duration).toLong())
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF9B59F5),
                                    activeTrackColor = Color(0xFF9B59F5),
                                    inactiveTrackColor = Color(0xFF444444)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    formatTime(currentPosition),
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

                // Skip Intro button
                if (showSkipIntro) {
                    Button(
                        onClick = {
                            streamData?.introEnd?.let { exoPlayer.value?.seekTo(it) }
                            showSkipIntro = false
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 80.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9B59F5)
                        )
                    ) {
                        Text("Skip Intro ⏭", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
