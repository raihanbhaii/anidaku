package com.anilite

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.anilite.data.AniApiService
import com.anilite.ui.theme.AnidakuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// ==================== DATA CLASSES ====================
data class StreamData(
    val m3u8Url: String,
    val subtitles: List<SubtitleTrack>,
    val intro: SkipSegment?,
    val outro: SkipSegment?
)

data class SubtitleTrack(
    val url: String,
    val label: String,
    val kind: String
)

data class SkipSegment(
    val start: Long,
    val end: Long
)

// ==================== ACTIVITY ====================
class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

// ==================== UPDATED FETCH FUNCTION (BYANIME ONLY) ====================
@OptIn(UnstableApi::class)
suspend fun fetchStreamData(episodeId: String, category: String): StreamData {
    val cleanId = when {
        episodeId.contains("?ep=") -> episodeId.substringBefore("?ep=")
        episodeId.contains("?") -> episodeId.substringBefore("?")
        else -> episodeId
    }
    val epParam = if (episodeId.contains("?ep=")) episodeId.substringAfter("?ep=") else ""

    // Try primary byanime-iota API with multiple servers
    val primaryServers = listOf("HD-1", "HD-2", "HD-3")
    for (server in primaryServers) {
        try {
            val baseUrl = "https://byanime-iota.vercel.app/api/stream"
            val url = if (epParam.isNotEmpty()) {
                "$baseUrl?id=$cleanId&server=$server&type=$category&ep=$epParam"
            } else {
                "$baseUrl?id=$episodeId&server=$server&type=$category"
            }
            
            Log.d("PlayerActivity", "Trying Primary API: $url")

            val response = withContext(Dispatchers.IO) {
                URL(url).openConnection().apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    connectTimeout = 15000
                    readTimeout = 15000
                }.getInputStream().bufferedReader().readText()
            }

            val json = JSONObject(response)
            val streamingArray = json.optJSONArray("streamingLink") ?: json.optJSONArray("sources")

            if (streamingArray != null && streamingArray.length() > 0) {
                val firstLink = streamingArray.getJSONObject(0)
                val m3u8 = firstLink.optString("link") ?: firstLink.optString("file", "")

                if (m3u8.isNotBlank() && m3u8.startsWith("http")) {
                    Log.d("PlayerActivity", "✅ SUCCESS with server $server")

                    val subtitles = mutableListOf<SubtitleTrack>()
                    json.optJSONArray("tracks")?.let { array ->
                        for (i in 0 until array.length()) {
                            val track = array.getJSONObject(i)
                            val file = track.optString("file")
                            if (file.isNotBlank()) {
                                subtitles.add(
                                    SubtitleTrack(
                                        url = file,
                                        label = track.optString("label", "English"),
                                        kind = track.optString("kind", "subtitles")
                                    )
                                )
                            }
                        }
                    }

                    val intro = json.optJSONObject("intro")?.let {
                        SkipSegment(it.optLong("start", 0) * 1000L, it.optLong("end", 0) * 1000L)
                    }
                    val outro = json.optJSONObject("outro")?.let {
                        SkipSegment(it.optLong("start", 0) * 1000L, it.optLong("end", 0) * 1000L)
                    }

                    return StreamData(m3u8, subtitles, intro, outro)
                }
            }
        } catch (e: Exception) {
            Log.w("PlayerActivity", "Primary server $server failed: ${e.message}")
        }
    }

    // Try Fallback byanime-iota API
    for (server in primaryServers) {
        try {
            val baseUrl = "https://byanime-iota.vercel.app/api/stream/fallback"
            val url = if (epParam.isNotEmpty()) {
                "$baseUrl?id=$cleanId&server=$server&type=$category&ep=$epParam"
            } else {
                "$baseUrl?id=$episodeId&server=$server&type=$category"
            }
            Log.d("PlayerActivity", "Trying Fallback API: $url")

            val response = withContext(Dispatchers.IO) {
                URL(url).openConnection().apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    connectTimeout = 10000
                    readTimeout = 10000
                }.getInputStream().bufferedReader().readText()
            }

            val json = JSONObject(response)
            val streamingArray = json.optJSONArray("streamingLink") ?: json.optJSONArray("sources")

            if (streamingArray != null && streamingArray.length() > 0) {
                val m3u8 = streamingArray.getJSONObject(0).optString("link")
                if (m3u8.isNotBlank() && m3u8.startsWith("http")) {
                    Log.d("PlayerActivity", "✅ SUCCESS with fallback server $server")
                    return StreamData(m3u8, emptyList(), null, null)
                }
            }
        } catch (e: Exception) {
            Log.w("PlayerActivity", "Fallback server $server failed: ${e.message}")
        }
    }

    throw Exception("ByAnime servers failed to load stream. Please try another episode.")
}

// ==================== HELPERS ====================
fun formatTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

@OptIn(UnstableApi::class)
fun applySubtitleState(player: ExoPlayer, enabled: Boolean) {
    player.trackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
        .build()
}

// ==================== SETTINGS PANEL ====================
@Composable
fun SettingsPanel(
    hasSubtitles: Boolean,
    subtitlesEnabled: Boolean,
    onSubtitlesToggle: (Boolean) -> Unit,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    aspectRatioMode: Int,
    onAspectChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = Color(0xFF9B59F5)
    val panelBg = Color(0xF01A1A2E)
    val divider = Color(0x22FFFFFF)
    val chipBg = Color(0x22FFFFFF)

    val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    val aspectLabels = listOf("Fit", "Fill", "Zoom")
    val aspectIcons = listOf(Icons.Default.FitScreen, Icons.Default.Fullscreen, Icons.Default.ZoomOutMap)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                onDismiss()
            }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Column(
                modifier = Modifier
                    .width(290.dp)
                    .fillMaxHeight()
                    .background(panelBg)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
                HorizontalDivider(color = divider)

                Spacer(Modifier.height(16.dp))

                Text("SUBTITLES", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp))
                Spacer(Modifier.height(8.dp))
                if (hasSubtitles) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ClosedCaption, null, tint = if (subtitlesEnabled) accent else Color.Gray, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("English Subtitles", color = Color.White, fontSize = 16.sp)
                        }
                        Switch(checked = subtitlesEnabled, onCheckedChange = onSubtitlesToggle, colors = SwitchDefaults.colors(checkedTrackColor = accent))
                    }
                } else {
                    Text("No subtitles available", color = Color.Gray, modifier = Modifier.padding(horizontal = 18.dp))
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = divider)

                Text("PLAYBACK SPEED", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
                speedOptions.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { speed ->
                            val isActive = speed == currentSpeed
                            Box(
                                modifier = Modifier.weight(1f).height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) accent else chipBg)
                                    .clickable { onSpeedChange(speed) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (speed == 1f) "Normal" else "${speed}x",
                                    color = if (isActive) Color.White else Color.LightGray,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = divider)

                Text("ASPECT RATIO", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aspectLabels.forEachIndexed { index, label ->
                        val active = index == aspectRatioMode
                        Column(
                            modifier = Modifier.weight(1f).height(60.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) accent else chipBg)
                                .clickable { onAspectChange(index) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(aspectIcons[index], null, tint = if (active) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                            Text(label, color = if (active) Color.White else Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==================== MAIN PLAYER SCREEN ====================
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
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var streamData by remember { mutableStateOf<StreamData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showControls by remember { mutableStateOf(true) }
    var showSkipIntro by remember { mutableStateOf(false) }
    var showSkipOutro by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(1L) }
    var isPlaying by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1f) }
    var seekFeedback by remember { mutableStateOf("") }
    var aspectRatioMode by remember { mutableStateOf(0) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }

    val exoPlayer = remember { mutableStateOf<ExoPlayer?>(null) }
    val tracksReady = remember { mutableStateOf(false) }

    val aspectModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )

    LaunchedEffect(episodeId, category) {
        isLoading = true
        errorMsg = ""
        try {
            streamData = fetchStreamData(episodeId, category)
        } catch (e: Exception) {
            errorMsg = e.message ?: "Unknown error"
            Log.e("PlayerActivity", "Stream fetch failed", e)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(showControls, isLocked, showSettings) {
        if (showControls && !isLocked && !showSettings) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(streamData) {
        val data = streamData ?: return@LaunchedEffect
        val factory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(mapOf("User-Agent" to "Mozilla/5.0"))
        }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(data.m3u8Url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)

        data.subtitles.firstOrNull()?.let { sub ->
            val subtitleConfig = SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                .setMimeType(MimeTypes.TEXT_VTT)
                .setLanguage("en")
                .setLabel(sub.label)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        }

        val mediaSource = HlsMediaSource.Factory(factory).createMediaSource(mediaItemBuilder.build())

        val player = ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    tracksReady.value = true
                    applySubtitleState(this@apply, subtitlesEnabled)
                }
                override fun onPlayerError(error: PlaybackException) {
                    errorMsg = "Playback Error: ${error.message}"
                }
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
        exoPlayer.value = player
    }

    LaunchedEffect(subtitlesEnabled, tracksReady.value) {
        if (tracksReady.value) exoPlayer.value?.let { applySubtitleState(it, subtitlesEnabled) }
    }

    LaunchedEffect(exoPlayer.value) {
        while (true) {
            delay(200)
            exoPlayer.value?.let { player ->
                currentPosition = player.currentPosition
                totalDuration = player.duration.coerceAtLeast(1L)
                streamData?.intro?.let { showSkipIntro = currentPosition in it.start until it.end }
                streamData?.outro?.let { showSkipOutro = currentPosition in it.start until it.end }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.value?.release()
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun seekBack() {
        exoPlayer.value?.seekTo((exoPlayer.value!!.currentPosition - 10000).coerceAtLeast(0))
        seekFeedback = "-10s"
    }

    fun seekForward() {
        exoPlayer.value?.seekTo(exoPlayer.value!!.currentPosition + 10000)
        seekFeedback = "+10s"
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF9B59F5))
                    Spacer(Modifier.height(16.dp))
                    Text("Loading stream...", color = Color.White)
                }
            }
            return@Box
        }

        if (errorMsg.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(60.dp))
                    Text("Failed to Load", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                    Text(errorMsg, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                }
            }
            return@Box
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer.value
                    useController = false
                    resizeMode = aspectModes[aspectRatioMode]
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view -> view.resizeMode = aspectModes[aspectRatioMode] }
        )

        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x44000000))
                    .pointerInput(Unit) { detectTapGestures { showControls = !showControls } }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0x00000000)), endY = 120f)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(episodeTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Episode $episodeNumber", color = Color.Gray, fontSize = 12.sp)
                    }
                    IconButton(onClick = { showSettings = !showSettings }) { Icon(Icons.Default.Settings, null, tint = Color.White) }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { seekBack() }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.FastRewind, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = { exoPlayer.value?.playWhenReady = !isPlaying }, modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF9B59F5))) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = { seekForward() }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0xFF000000)), startY = 80f)).padding(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0x44FFFFFF), RoundedCornerShape(2.dp)).pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val progress = (offset.x / size.width).coerceIn(0f, 1f)
                            exoPlayer.value?.seekTo((progress * totalDuration).toLong())
                        }
                    }) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f)).background(Color(0xFF9B59F5), RoundedCornerShape(2.dp)))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp)
                        Text(formatTime(totalDuration), color = Color.Gray, fontSize = 12.sp)
                    }
                }

                IconButton(onClick = { isLocked = !isLocked }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, null, tint = Color.White)
                }
            }
        }

        if (showSkipIntro) {
            Button(onClick = { streamData?.intro?.let { exoPlayer.value?.seekTo(it.end) } }, modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59F5))) { Text("Skip Intro") }
        }
        if (showSkipOutro) {
            Button(onClick = { streamData?.outro?.let { exoPlayer.value?.seekTo(it.end) } }, modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59F5))) { Text("Skip Outro") }
        }

        if (showSettings) {
            SettingsPanel(
                hasSubtitles = streamData?.subtitles?.isNotEmpty() ?: false,
                subtitlesEnabled = subtitlesEnabled,
                onSubtitlesToggle = { subtitlesEnabled = it },
                currentSpeed = currentSpeed,
                onSpeedChange = { speed -> currentSpeed = speed; exoPlayer.value?.setPlaybackParameters(PlaybackParameters(speed)) },
                aspectRatioMode = aspectRatioMode,
                onAspectChange = { aspectRatioMode = it },
                onDismiss = { showSettings = false }
            )
        }

        if (seekFeedback.isNotEmpty()) {
            LaunchedEffect(seekFeedback) { delay(500); seekFeedback = "" }
            Box(modifier = Modifier.align(Alignment.Center).background(Color(0x88000000), RoundedCornerShape(8.dp)).padding(16.dp)) {
                Text(seekFeedback, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isBuffering) {
            Box(modifier = Modifier.align(Alignment.Center).size(50.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF9B59F5), modifier = Modifier.size(40.dp))
            }
        }
    }
}
