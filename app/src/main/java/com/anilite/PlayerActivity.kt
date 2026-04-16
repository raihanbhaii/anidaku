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
import com.anilite.ui.theme.AnidakuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// Data classes
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

// Activity
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
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

// Stream fetch with new API
suspend fun fetchStreamData(episodeId: String, category: String): StreamData {
    val cleanId = episodeId.substringAfterLast("ep=").takeIf { it.isNotBlank() } ?: episodeId
    
    val servers = listOf("HD-1", "HD-2", "HD-3")
    
    for (server in servers) {
        try {
            val url = "https://byanime-iota.vercel.app/api/stream?id=$cleanId&server=$server&type=$category"
            
            val response = withContext(Dispatchers.IO) {
                URL(url).openConnection().apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    connectTimeout = 15000
                    readTimeout = 15000
                }.getInputStream().bufferedReader().readText()
            }
            
            val json = JSONObject(response)
            val streamingLink = json.optJSONArray("streamingLink")
            
            if (streamingLink != null && streamingLink.length() > 0) {
                val firstLink = streamingLink.getJSONObject(0)
                val m3u8 = firstLink.optString("link")
                
                if (m3u8.isNotBlank()) {
                    val tracksArray = json.optJSONArray("tracks")
                    val subtitles = mutableListOf<SubtitleTrack>()
                    
                    if (tracksArray != null) {
                        for (i in 0 until tracksArray.length()) {
                            val track = tracksArray.getJSONObject(i)
                            subtitles.add(
                                SubtitleTrack(
                                    url = track.optString("file"),
                                    label = track.optString("label", "Unknown"),
                                    kind = track.optString("kind", "subtitles")
                                )
                            )
                        }
                    }
                    
                    val introJson = json.optJSONObject("intro")
                    val intro = if (introJson != null) {
                        SkipSegment(
                            start = introJson.optLong("start", 0L) * 1000L,
                            end = introJson.optLong("end", 0L) * 1000L
                        )
                    } else null
                    
                    val outroJson = json.optJSONObject("outro")
                    val outro = if (outroJson != null) {
                        SkipSegment(
                            start = outroJson.optLong("start", 0L) * 1000L,
                            end = outroJson.optLong("end", 0L) * 1000L
                        )
                    } else null
                    
                    return StreamData(m3u8, subtitles, intro, outro)
                }
            }
        } catch (e: Exception) {
            Log.w("PlayerActivity", "Server $server failed: ${e.message}")
        }
    }
    
    throw Exception("All servers failed")
}

// Helpers
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

// Settings Panel
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
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
    )

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
                Text("Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = divider, thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))

            // Subtitles
            Text("SUBTITLES", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, 
                 letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(8.dp))

            if (hasSubtitles) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ClosedCaption, null, 
                             tint = if (subtitlesEnabled) accent else Color(0xFF666666), 
                             modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Subtitles", color = Color.White, fontSize = 14.sp)
                            Text(if (subtitlesEnabled) "On" else "Off", 
                                 color = if (subtitlesEnabled) accent else Color(0xFF888888), 
                                 fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = subtitlesEnabled,
                        onCheckedChange = onSubtitlesToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent,
                            uncheckedThumbColor = Color(0xFFAAAAAA),
                            uncheckedTrackColor = Color(0xFF444444)
                        )
                    )
                }
            } else {
                Text("No subtitles available", color = Color(0xFF555555), fontSize = 13.sp, 
                     modifier = Modifier.padding(horizontal = 18.dp))
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = divider, thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))

            // Playback Speed
            Text("PLAYBACK SPEED", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                 letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(8.dp))

            speedOptions.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    row.forEach { speed ->
                        val active = speed == currentSpeed
                        Box(
                            modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(7.dp))
                                .background(if (active) accent else chipBg).clickable { onSpeedChange(speed) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (speed == 1f) "Normal" else "${speed}x",
                                color = if (active) Color.White else Color(0xFFBBBBBB),
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = divider, thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))

            // Aspect Ratio
            Text("ASPECT RATIO", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                 letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                aspectLabels.forEachIndexed { index, label ->
                    val active = index == aspectRatioMode
                    Column(
                        modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (active) accent else chipBg).clickable { onAspectChange(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(aspectIcons[index], label, 
                             tint = if (active) Color.White else Color(0xFFAAAAAA), 
                             modifier = Modifier.size(16.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(label, color = if (active) Color.White else Color(0xFFBBBBBB), 
                             fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

// Main Player Screen
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
    var volumeLevel by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var showVolumeHud by remember { mutableStateOf(false) }
    var showBrightnessHud by remember { mutableStateOf(false) }
    var brightnessLevel by remember {
        mutableStateOf(
            try { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f }
            catch (e: Exception) { 0.5f }
        )
    }

    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    val exoPlayer = remember { mutableStateOf<ExoPlayer?>(null) }
    val tracksReady = remember { mutableStateOf(false) }

    val aspectModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )

    // Fetch stream
    LaunchedEffect(episodeId, category) {
        isLoading = true
        errorMsg = ""
        try {
            streamData = fetchStreamData(episodeId, category)
        } catch (e: Exception) {
            errorMsg = "Failed to load stream. Please try again."
        } finally {
            isLoading = false
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isLocked, showSettings) {
        if (showControls && !isLocked && !showSettings) {
            delay(3000)
            showControls = false
        }
    }

    // Clear seek feedback
    LaunchedEffect(seekFeedback) {
        if (seekFeedback.isNotEmpty()) {
            delay(800)
            seekFeedback = ""
        }
    }

    // Hide HUDs
    LaunchedEffect(showVolumeHud) {
        if (showVolumeHud) { delay(1500); showVolumeHud = false }
    }
    LaunchedEffect(showBrightnessHud) {
        if (showBrightnessHud) { delay(1500); showBrightnessHud = false }
    }

    // Build ExoPlayer
    LaunchedEffect(streamData) {
        val data = streamData ?: return@LaunchedEffect
        tracksReady.value = false

        val factory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(mapOf(
                "User-Agent" to "Mozilla/5.0",
                "Origin" to "https://byanime-iota.vercel.app"
            ))
        }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(data.m3u8Url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)

        data.subtitles.firstOrNull()?.let { subtitle ->
            val subtitleConfig = SubtitleConfiguration.Builder(android.net.Uri.parse(subtitle.url))
                .setMimeType(MimeTypes.TEXT_VTT)
                .setLanguage("en")
                .setLabel(subtitle.label)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
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
                    errorMsg = "Playback error: ${error.message}"
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

    // Apply subtitle toggle
    LaunchedEffect(subtitlesEnabled, tracksReady.value) {
        if (tracksReady.value) {
            exoPlayer.value?.let { applySubtitleState(it, subtitlesEnabled) }
        }
    }

    // Position polling
    LaunchedEffect(exoPlayer.value) {
        val player = exoPlayer.value ?: return@LaunchedEffect
        while (true) {
            delay(200)
            currentPosition = player.currentPosition
            totalDuration = player.duration.coerceAtLeast(1L)
            
            streamData?.intro?.let {
                showSkipIntro = currentPosition in it.start until it.end
            }
            streamData?.outro?.let {
                showSkipOutro = currentPosition in it.start until it.end
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.value?.release()
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Seek helpers
    fun seekBack() {
        exoPlayer.value?.let {
            it.seekTo((it.currentPosition - 10000).coerceAtLeast(0))
            seekFeedback = "-10s"
        }
    }

    fun seekForward() {
        exoPlayer.value?.let {
            it.seekTo(it.currentPosition + 10000)
            seekFeedback = "+10s"
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        // Loading
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color(0xFF9B59F5), strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text("Loading stream...", color = Color(0xFFCCCCCC), fontSize = 14.sp)
            }
            return@Box
        }

        // Error
        if (errorMsg.isNotEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Error", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(errorMsg, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59F5)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Go Back") }
            }
            return@Box
        }

        // Video surface
        if (streamData != null && exoPlayer.value != null) {
            
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = aspectModes[aspectRatioMode]
                        player = exoPlayer.value
                    }
                },
                update = { view ->
                    view.player = exoPlayer.value
                    view.resizeMode = aspectModes[aspectRatioMode]
                },
                modifier = Modifier.fillMaxSize()
            )

            // Gesture layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked, showSettings) {
                        if (!isLocked && !showSettings) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2) seekBack() else seekForward()
                                },
                                onTap = { showControls = !showControls }
                            )
                        } else if (!showSettings) {
                            detectTapGestures(onTap = { showControls = !showControls })
                        }
                    }
                    .pointerInput(isLocked, showSettings) {
                        if (!isLocked && !showSettings) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                val x = change.position.x
                                if (x < size.width / 2) {
                                    val delta = -dragAmount / size.height
                                    val newBrightness = (brightnessLevel + delta).coerceIn(0f, 1f)
                                    brightnessLevel = newBrightness
                                    showBrightnessHud = true
                                    try {
                                        val lp = (context as Activity).window.attributes
                                        lp.screenBrightness = newBrightness
                                        context.window.attributes = lp
                                    } catch (_: Exception) {}
                                } else {
                                    val delta = -dragAmount / size.height * maxVolume
                                    val newVol = (volumeLevel + delta).coerceIn(0f, maxVolume)
                                    volumeLevel = newVol
                                    showVolumeHud = true
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol.toInt(), 0)
                                }
                            }
                        }
                    }
            )

            // Buffering indicator
            AnimatedVisibility(visible = isBuffering, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0x55000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF9B59F5), strokeWidth = 2.dp, modifier = Modifier.size(40.dp))
                }
            }

            // Seek feedback
            AnimatedVisibility(
                visible = seekFeedback.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(seekFeedback, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Volume HUD
            AnimatedVisibility(
                visible = showVolumeHud,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
            ) {
                SwipeHud(Icons.Default.VolumeUp, volumeLevel / maxVolume)
            }

            // Brightness HUD
            AnimatedVisibility(
                visible = showBrightnessHud,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
            ) {
                SwipeHud(Icons.Default.BrightnessHigh, brightnessLevel)
            }

            // Lock screen
            if (isLocked) {
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                ) {
                    IconButton(
                        onClick = { isLocked = false; showControls = true },
                        modifier = Modifier.clip(CircleShape).background(Color(0xAA000000)).size(44.dp)
                    ) {
                        Icon(Icons.Default.Lock, "Unlock", tint = Color(0xFF9B59F5), modifier = Modifier.size(20.dp))
                    }
                }
                return@Box
            }

            // Controls overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x80000000))) {
                    
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(episodeTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text("Episode $episodeNumber", color = Color(0xFFAAAAAA), fontSize = 11.sp)
                        }
                        
                        IconButton(onClick = { isLocked = true; showControls = false }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.LockOpen, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        
                        IconButton(
                            onClick = { showSettings = true; showControls = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Settings, "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Center controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerIconButton(Icons.Default.Replay10, size = 44.dp) { seekBack() }
                        
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF9B59F5))
                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                    if (isPlaying) exoPlayer.value?.pause() else exoPlayer.value?.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        PlayerIconButton(Icons.Default.Forward10, size = 44.dp) { seekForward() }
                    }

                    // Bottom bar - Simple Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Standard Slider (no experimental APIs)
                        Slider(
                            value = currentPosition.toFloat() / totalDuration.toFloat(),
                            onValueChange = { fraction ->
                                exoPlayer.value?.seekTo((fraction * totalDuration).toLong())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF9B59F5),
                                activeTrackColor = Color(0xFF9B59F5),
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )
                        
                        // Time labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(currentPosition), color = Color(0xFFEEEEEE), fontSize = 12.sp)
                            Text(formatTime(totalDuration), color = Color(0xFFEEEEEE), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Skip intro button
            AnimatedVisibility(
                visible = showSkipIntro && !showSettings,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 80.dp)
            ) {
                Button(
                    onClick = {
                        streamData?.intro?.end?.let { exoPlayer.value?.seekTo(it) }
                        showSkipIntro = false
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59F5)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Skip Intro", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }

            // Skip outro button
            AnimatedVisibility(
                visible = showSkipOutro && !showSettings,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 80.dp)
            ) {
                Button(
                    onClick = {
                        streamData?.outro?.end?.let { exoPlayer.value?.seekTo(it) }
                        showSkipOutro = false
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59F5)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Skip Outro", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }

            // Settings panel
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInHorizontally { it } + fadeIn(tween(180)),
                exit = slideOutHorizontally { it } + fadeOut(tween(180)),
                modifier = Modifier.fillMaxSize()
            ) {
                SettingsPanel(
                    hasSubtitles = streamData?.subtitles?.isNotEmpty() == true,
                    subtitlesEnabled = subtitlesEnabled,
                    onSubtitlesToggle = { subtitlesEnabled = it },
                    currentSpeed = currentSpeed,
                    onSpeedChange = { speed ->
                        currentSpeed = speed
                        exoPlayer.value?.playbackParameters = PlaybackParameters(speed)
                    },
                    aspectRatioMode = aspectRatioMode,
                    onAspectChange = { aspectRatioMode = it },
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}

@Composable
fun PlayerIconButton(icon: ImageVector, size: androidx.compose.ui.unit.Dp = 40.dp, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size - 16.dp))
    }
}

@Composable
fun SwipeHud(icon: ImageVector, value: Float) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xCC000000))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF444444))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF9B59F5))
                    .align(Alignment.BottomCenter)
            )
        }
        Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 9.sp)
    }
}
