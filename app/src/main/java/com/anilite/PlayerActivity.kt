package com.anilite

import android.app.Activity
import android.content.ContentValues.TAG
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
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
import kotlin.math.abs

// ── Data ─────────────────────────────────────────────────────────────────────

data class StreamData(
    val m3u8Url: String,
    val referer: String,
    val introEnd: Long,
    val subtitleUrl: String?
)

// ── Activity ──────────────────────────────────────────────────────────────────

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val episodeId     = intent.getStringExtra("episodeId")     ?: ""
        val category      = intent.getStringExtra("category")      ?: "sub"
        val episodeTitle  = intent.getStringExtra("episodeTitle")  ?: ""
        val episodeNumber = intent.getIntExtra("episodeNumber", 0)

        setContent {
            AnidakuTheme {
                PlayerScreen(
                    episodeId     = episodeId,
                    category      = category,
                    episodeTitle  = episodeTitle,
                    episodeNumber = episodeNumber,
                    onBack        = { finish() }
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

// ── Stream fetch ──────────────────────────────────────────────────────────────

suspend fun fetchStreamData(episodeId: String, category: String): StreamData {
    val animeEpisodeId = episodeId.substringBefore("?ep=")
    val ep             = episodeId.substringAfterLast("ep=")

    for (server in listOf("hd-1", "hd-2")) {
        try {
            val url = "https://anidaku-api.vercel.app/api/v2/hianime/episode/sources" +
                "?animeEpisodeId=$animeEpisodeId&ep=$ep&server=$server&category=$category"
            val response = withContext(Dispatchers.IO) {
                URL(url).openConnection().apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    connectTimeout = 15000
                    readTimeout    = 15000
                }.getInputStream().bufferedReader().readText()
            }
            val json    = JSONObject(response)
            val m3u8    = json.getString("source")
            val referer = json.optString("refer", "https://megacloud.club/")
            val intro   = json.optJSONObject("skip")
                ?.optJSONObject("intro")?.optLong("end", 0L) ?: 0L
            val subtitleUrl = json.optJSONArray("tracks")?.let { tracks ->
                (0 until tracks.length()).map { tracks.getJSONObject(it) }
                    .firstOrNull {
                        it.optBoolean("default") && it.optString("kind") == "captions"
                    }?.optString("file")
            }
            return StreamData(m3u8, referer, intro * 1000L, subtitleUrl)
        } catch (e: Exception) {
            Log.w(TAG, "Server $server failed: ${e.message}")
        }
    }
    throw Exception("All servers failed")
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun formatTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

// ── Player screen ─────────────────────────────────────────────────────────────

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

    // ── State ─────────────────────────────────────────────────────────────
    var streamData      by remember { mutableStateOf<StreamData?>(null) }
    var isLoading       by remember { mutableStateOf(true) }
    var isBuffering     by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf("") }
    var showControls    by remember { mutableStateOf(true) }
    var showSkipIntro   by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration   by remember { mutableStateOf(1L) }
    var isPlaying       by remember { mutableStateOf(true) }
    var isLocked        by remember { mutableStateOf(false) }
    var showSpeedMenu   by remember { mutableStateOf(false) }
    var currentSpeed    by remember { mutableStateOf(1f) }
    var seekFeedback    by remember { mutableStateOf("") } // "+10s" / "-10s"
    var aspectRatioMode by remember { mutableStateOf(0) } // 0=Fit 1=Fill 2=Zoom
    var volumeLevel     by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var showVolumeHud   by remember { mutableStateOf(false) }
    var showBrightnessHud by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableStateOf(
        try { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f }
        catch (e: Exception) { 0.5f }
    )}

    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    val exoPlayer = remember { mutableStateOf<ExoPlayer?>(null) }
    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }

    val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    val aspectLabels = listOf("Fit", "Fill", "Zoom")
    val aspectModes  = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )

    // ── Fetch stream ──────────────────────────────────────────────────────
    LaunchedEffect(episodeId, category) {
        isLoading = true; errorMsg = ""
        try {
            streamData = fetchStreamData(episodeId, category)
        } catch (e: Exception) {
            errorMsg = "Failed to load stream.\nPlease go back and try again."
        } finally {
            isLoading = false
        }
    }

    // ── Auto-hide controls ────────────────────────────────────────────────
    LaunchedEffect(showControls, isLocked) {
        if (showControls && !isLocked) {
            delay(4000)
            showControls = false
        }
    }

    // ── Clear seek feedback ───────────────────────────────────────────────
    LaunchedEffect(seekFeedback) {
        if (seekFeedback.isNotEmpty()) {
            delay(800)
            seekFeedback = ""
        }
    }

    // ── Hide HUDs ─────────────────────────────────────────────────────────
    LaunchedEffect(showVolumeHud) {
        if (showVolumeHud) { delay(1500); showVolumeHud = false }
    }
    LaunchedEffect(showBrightnessHud) {
        if (showBrightnessHud) { delay(1500); showBrightnessHud = false }
    }

    // ── Build ExoPlayer ───────────────────────────────────────────────────
    LaunchedEffect(streamData) {
        val data = streamData ?: return@LaunchedEffect
        val factory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(mapOf(
                "Referer"    to data.referer,
                "Origin"     to data.referer.trimEnd('/'),
                "User-Agent" to "Mozilla/5.0"
            ))
        }
        val mediaSource = HlsMediaSource.Factory(factory).createMediaSource(
            MediaItem.Builder().setUri(data.m3u8Url).setMimeType(MimeTypes.APPLICATION_M3U8).build()
        )
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
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

    // ── Position + skip intro polling ─────────────────────────────────────
    LaunchedEffect(exoPlayer.value) {
        val player = exoPlayer.value ?: return@LaunchedEffect
        while (true) {
            delay(500)
            currentPosition = player.currentPosition
            totalDuration   = player.duration.coerceAtLeast(1L)
            val introEnd    = streamData?.introEnd ?: 0L
            showSkipIntro   = introEnd > 0 && currentPosition in 1 until introEnd
        }
    }

    // ── Dispose ───────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.value?.release()
            (context as? Activity)?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ── Seek helpers ──────────────────────────────────────────────────────
    fun seekBack() {
        exoPlayer.value?.let {
            it.seekTo((it.currentPosition - 10000).coerceAtLeast(0))
            seekFeedback = "⏪  -10s"
        }
    }
    fun seekForward() {
        exoPlayer.value?.let {
            it.seekTo(it.currentPosition + 10000)
            seekFeedback = "+10s  ⏩"
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI
    // ══════════════════════════════════════════════════════════════════════

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ── Loading ───────────────────────────────────────────────────────
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color(0xFF9B59F5), strokeWidth = 3.dp)
                Spacer(Modifier.height(14.dp))
                Text("Loading stream…", color = Color(0xFFCCCCCC), fontSize = 14.sp)
            }
            return@Box
        }

        // ── Error ─────────────────────────────────────────────────────────
        if (errorMsg.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("⚠️", fontSize = 40.sp)
                Text(
                    errorMsg, color = Color.White, fontSize = 14.sp,
                    textAlign = TextAlign.Center, lineHeight = 22.sp
                )
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59F5)),
                    shape  = RoundedCornerShape(10.dp)
                ) { Text("Go Back") }
            }
            return@Box
        }

        // ── Video surface ─────────────────────────────────────────────────
        if (streamData != null && exoPlayer.value != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode    = aspectModes[aspectRatioMode]
                        player        = exoPlayer.value
                        playerViewRef.value = this
                    }
                },
                update = { view ->
                    view.player     = exoPlayer.value
                    view.resizeMode = aspectModes[aspectRatioMode]
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── Gesture layer (double tap seek, swipe volume/brightness) ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked) {
                        if (!isLocked) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2) seekBack()
                                    else seekForward()
                                },
                                onTap = { showControls = !showControls }
                            )
                        } else {
                            detectTapGestures(onTap = { showControls = !showControls })
                        }
                    }
                    .pointerInput(isLocked) {
                        if (!isLocked) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                val x = change.position.x
                                if (x < size.width / 2) {
                                    // Left = brightness
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
                                    // Right = volume
                                    val delta = -dragAmount / size.height * maxVolume
                                    val newVol = (volumeLevel + delta).coerceIn(0f, maxVolume)
                                    volumeLevel = newVol
                                    showVolumeHud = true
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        newVol.toInt(),
                                        0
                                    )
                                }
                            }
                        }
                    }
            )

            // ── Buffering spinner ─────────────────────────────────────────
            AnimatedVisibility(
                visible = isBuffering,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF9B59F5),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            // ── Seek feedback (double tap) ────────────────────────────────
            AnimatedVisibility(
                visible = seekFeedback.isNotEmpty(),
                enter   = fadeIn() + scaleIn(),
                exit    = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xAA000000))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(seekFeedback, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Volume HUD ────────────────────────────────────────────────
            AnimatedVisibility(
                visible  = showVolumeHud,
                enter    = fadeIn(),
                exit     = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
            ) {
                SwipeHud(
                    icon  = Icons.Default.VolumeUp,
                    value = volumeLevel / maxVolume,
                    label = "Volume"
                )
            }

            // ── Brightness HUD ────────────────────────────────────────────
            AnimatedVisibility(
                visible  = showBrightnessHud,
                enter    = fadeIn(),
                exit     = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
            ) {
                SwipeHud(
                    icon  = Icons.Default.BrightnessHigh,
                    value = brightnessLevel,
                    label = "Brightness"
                )
            }

            // ── Lock screen: only show lock icon ──────────────────────────
            if (isLocked) {
                AnimatedVisibility(
                    visible  = showControls,
                    enter    = fadeIn(),
                    exit     = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                ) {
                    IconButton(
                        onClick = { isLocked = false; showControls = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xAA000000))
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = Color(0xFF9B59F5), modifier = Modifier.size(22.dp))
                    }
                }
                return@Box
            }

            // ── Full controls overlay ─────────────────────────────────────
            AnimatedVisibility(
                visible  = showControls,
                enter    = fadeIn(tween(200)),
                exit     = fadeOut(tween(300)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000))
                ) {
                    // ── Top bar ───────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                episodeTitle,
                                color = Color.White, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1
                            )
                            Text(
                                "Episode $episodeNumber • ${category.uppercase()}",
                                color = Color(0xFFAAAAAA), fontSize = 11.sp
                            )
                        }
                        // Aspect ratio
                        TextButton(
                            onClick = { aspectRatioMode = (aspectRatioMode + 1) % 3 },
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Text(aspectLabels[aspectRatioMode], color = Color.White, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(6.dp))
                        // Speed
                        Box {
                            TextButton(
                                onClick = { showSpeedMenu = true },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Text("${currentSpeed}x", color = Color.White, fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false },
                                modifier = Modifier.background(Color(0xFF1A1A2E))
                            ) {
                                speedOptions.forEach { speed ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${speed}x",
                                                color = if (speed == currentSpeed) Color(0xFF9B59F5) else Color.White,
                                                fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            currentSpeed = speed
                                            exoPlayer.value?.playbackParameters = PlaybackParameters(speed)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        // Lock
                        IconButton(onClick = { isLocked = true; showControls = false }) {
                            Icon(Icons.Default.LockOpen, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // ── Centre controls ───────────────────────────────────
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        PlayerIconButton(Icons.Default.Replay10, "-10s", size = 40.dp) { seekBack() }

                        // Play / Pause
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA9B59F5))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    if (isPlaying) exoPlayer.value?.pause()
                                    else exoPlayer.value?.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint     = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        PlayerIconButton(Icons.Default.Forward10, "+10s", size = 40.dp) { seekForward() }
                    }

                    // ── Bottom bar ────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Slider(
                            value         = currentPosition.toFloat() / totalDuration.toFloat(),
                            onValueChange = { fraction ->
                                exoPlayer.value?.seekTo((fraction * totalDuration).toLong())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = SliderDefaults.colors(
                                thumbColor         = Color(0xFF9B59F5),
                                activeTrackColor   = Color(0xFF9B59F5),
                                inactiveTrackColor = Color(0xFF555555)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(currentPosition), color = Color(0xFFCCCCCC), fontSize = 11.sp)
                            Text(formatTime(totalDuration),   color = Color(0xFFCCCCCC), fontSize = 11.sp)
                        }
                    }
                }
            }

            // ── Skip intro button ─────────────────────────────────────────
            AnimatedVisibility(
                visible  = showSkipIntro,
                enter    = slideInHorizontally { it } + fadeIn(),
                exit     = slideOutHorizontally { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 72.dp)
            ) {
                Button(
                    onClick = {
                        streamData?.introEnd?.let { exoPlayer.value?.seekTo(it) }
                        showSkipIntro = false
                    },
                    shape  = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59F5)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Skip Intro ⏭", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Reusable icon button ──────────────────────────────────────────────────────

@Composable
fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(size + 16.dp)) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(size))
    }
}

// ── Volume / Brightness HUD ───────────────────────────────────────────────────

@Composable
fun SwipeHud(icon: ImageVector, value: Float, label: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xBB000000))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF444444))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF9B59F5))
                    .align(Alignment.BottomCenter)
            )
        }
        Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 10.sp)
    }
}
