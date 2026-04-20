package com.anilite

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
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
import kotlinx.coroutines.launch
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
    val kind: String,
    val language: String = "en"
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
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

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
            WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

// ==================== STREAM FETCHER ====================
@OptIn(UnstableApi::class)
suspend fun fetchStreamData(episodeId: String, category: String): StreamData {
    val cleanId = episodeId.substringBefore("?ep=").substringBefore("?")
    val epParam = if (episodeId.contains("?ep=")) episodeId.substringAfter("?ep=") else ""
    val servers = listOf("HD-1", "HD-2", "HD-3")
    val base = "https://byanime-iota.vercel.app/api/stream"

    for (server in servers) {
        try {
            val url = buildString {
                append("$base?id=${if (epParam.isNotEmpty()) cleanId else episodeId}")
                append("&server=$server&type=$category")
                if (epParam.isNotEmpty()) append("&ep=$epParam")
            }
            Log.d("PlayerActivity", "Trying: $url")

            val response = withContext(Dispatchers.IO) {
                URL(url).openConnection().apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }.getInputStream().bufferedReader().readText()
            }

            val root = JSONObject(response)
            val json = root.optJSONObject("results") ?: root
            val sources = json.optJSONArray("streamingLink") ?: json.optJSONArray("sources")

            if (sources != null && sources.length() > 0) {
                val first = sources.getJSONObject(0)
                val m3u8 = first.optString("link").ifBlank { first.optString("file") }
                if (m3u8.startsWith("http")) {
                    Log.d("PlayerActivity", "✅ Stream found: $server")

                    val subtitles = mutableListOf<SubtitleTrack>()
                    val tracksArr = json.optJSONArray("tracks") ?: json.optJSONArray("subtitles")
                    tracksArr?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val t = arr.getJSONObject(i)
                            val file = t.optString("file").ifBlank { t.optString("url") }
                            if (file.isNotBlank()) {
                                val label = t.optString("label").ifBlank {
                                    t.optString("lang", "English")
                                }
                                val language = t.optString("language").ifBlank {
                                    when {
                                        label.contains("english", true) -> "en"
                                        label.contains("spanish", true) -> "es"
                                        label.contains("french", true) -> "fr"
                                        label.contains("portuguese", true) -> "pt"
                                        else -> "en"
                                    }
                                }
                                subtitles += SubtitleTrack(
                                    url = file,
                                    label = label,
                                    kind = t.optString("kind", "subtitles"),
                                    language = language
                                )
                            }
                        }
                    }

                    val intro = json.optJSONObject("intro")?.let {
                        SkipSegment(it.optLong("start") * 1000L, it.optLong("end") * 1000L)
                    }
                    val outro = json.optJSONObject("outro")?.let {
                        SkipSegment(it.optLong("start") * 1000L, it.optLong("end") * 1000L)
                    }

                    return StreamData(m3u8, subtitles, intro, outro)
                }
            }
        } catch (e: Exception) {
            Log.w("PlayerActivity", "[$base][$server] failed: ${e.message}")
        }
    }
    throw Exception("All servers failed. Please try again later.")
}

// ==================== HELPERS ====================
fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

@OptIn(UnstableApi::class)
fun applySubtitleState(player: ExoPlayer, enabled: Boolean, preferredLanguage: String = "en") {
    val builder = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)

    if (enabled) {
        builder
            .setPreferredTextLanguage(preferredLanguage)
            .setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE)
    }

    player.trackSelectionParameters = builder.build()

    kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
        delay(100)
        player.prepare()
    }
}

fun getSubtitleMimeType(url: String): String {
    return when {
        url.endsWith(".vtt", true) || url.contains("vtt", true) -> MimeTypes.TEXT_VTT
        url.endsWith(".srt", true) || url.contains("srt", true) -> MimeTypes.APPLICATION_SUBRIP
        url.endsWith(".ttml", true) || url.endsWith(".xml", true) -> MimeTypes.APPLICATION_TTML
        url.endsWith(".dfxp", true) -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.TEXT_VTT
    }
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
    val panelBg = Color(0xF0111122)
    val divider = Color(0x1FFFFFFF)
    val chipBg = Color(0x18FFFFFF)

    val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    val aspectLabels = listOf("Fit", "Fill", "Zoom")
    val aspectIcons = listOf(
        Icons.Default.FitScreen,
        Icons.Default.AspectRatio,
        Icons.Default.ZoomOutMap
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(panelBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFFAAAAAA))
                    }
                }
                HorizontalDivider(color = divider, thickness = 0.5.dp)
                Spacer(Modifier.height(20.dp))

                // Subtitles
                SectionLabel("SUBTITLES", accent)
                Spacer(Modifier.height(10.dp))
                if (hasSubtitles) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple()
                            ) { onSubtitlesToggle(!subtitlesEnabled) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ClosedCaption, null,
                                tint = if (subtitlesEnabled) accent else Color(0xFF666666),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("English Subtitles", color = Color.White, fontSize = 15.sp)
                        }
                        Switch(
                            checked = subtitlesEnabled,
                            onCheckedChange = onSubtitlesToggle,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = accent,
                                uncheckedTrackColor = Color(0xFF333344),
                                checkedThumbColor = Color.White,
                                uncheckedThumbColor = Color(0xFFAAAAAA)
                            ),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    Text(
                        "No subtitles available",
                        color = Color(0xFF666666),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = divider, thickness = 0.5.dp)
                Spacer(Modifier.height(20.dp))

                // Speed
                SectionLabel("PLAYBACK SPEED", accent)
                Spacer(Modifier.height(12.dp))
                speedOptions.chunked(4).forEach { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { speed ->
                            val active = speed == currentSpeed
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) accent else chipBg)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple()
                                    ) { onSpeedChange(speed) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (speed == 1f) "1x" else "${speed}x",
                                    color = if (active) Color.White else Color(0xFFAAAAAA),
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = divider, thickness = 0.5.dp)
                Spacer(Modifier.height(20.dp))

                // Aspect Ratio
                SectionLabel("ASPECT RATIO", accent)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aspectLabels.forEachIndexed { index, label ->
                        val active = index == aspectRatioMode
                        Column(
                            Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) accent else chipBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple()
                                ) { onAspectChange(index) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                aspectIcons[index], null,
                                tint = if (active) Color.White else Color(0xFF888888),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                color = if (active) Color.White else Color(0xFF888888),
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

// ==================== PLAYER SCREEN ====================
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
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // State
    var streamData by remember { mutableStateOf<StreamData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showControls by remember { mutableStateOf(true) }
    var showSkipIntro by remember { mutableStateOf(false) }
    var showSkipOutro by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(1L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1f) }
    var seekFeedback by remember { mutableStateOf("") }
    var aspectRatioMode by remember { mutableStateOf(0) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekDragPosition by remember { mutableStateOf(0L) }
    var tracksReady by remember { mutableStateOf(false) }
    var availableSubtitleLanguages by remember { mutableStateOf(setOf<String>()) }
    var selectedSubtitleLang by remember { mutableStateOf("en") }
    var lastInteractionTime by remember { mutableStateOf(0L) }
    var sliderWidthPx by remember { mutableStateOf(0f) }

    val playerState = remember { mutableStateOf<ExoPlayer?>(null) }

    val aspectModes = remember {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        )
    }

    // Fetch stream
    LaunchedEffect(episodeId, category) {
        isLoading = true
        errorMsg = ""
        streamData = null
        try {
            streamData = fetchStreamData(episodeId, category)
        } catch (e: Exception) {
            errorMsg = e.message ?: "Unknown error occurred"
            Log.e("PlayerActivity", "Stream fetch failed", e)
        } finally {
            isLoading = false
        }
    }

    // Build ExoPlayer
    LaunchedEffect(streamData) {
        val data = streamData ?: return@LaunchedEffect
        playerState.value?.release()
        playerState.value = null
        tracksReady.value = false

        val factory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
        }

        val itemBuilder = MediaItem.Builder()
            .setUri(data.m3u8Url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)

        if (data.subtitles.isNotEmpty()) {
            val configs = data.subtitles.mapNotNull { sub ->
                val mimeType = getSubtitleMimeType(sub.url)
                try {
                    SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                        .setMimeType(mimeType)
                        .setLanguage(sub.language)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                        .setLabel(sub.label)
                        .build()
                } catch (e: Exception) {
                    Log.w("PlayerActivity", "Failed to add subtitle: ${sub.url}", e)
                    null
                }
            }
            if (configs.isNotEmpty()) {
                itemBuilder.setSubtitleConfigurations(configs)
                availableSubtitleLanguages = data.subtitles.map { it.language }.toSet()
            }
        }

        val source = HlsMediaSource.Factory(factory).createMediaSource(itemBuilder.build())

        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()
            .apply {
                setMediaSource(source)
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onTracksChanged(tracks: Tracks) {
                        tracksReady.value = true
                        val langs = mutableSetOf<String>()
                        for (group in tracks.groups) {
                            if (group.type == C.TRACK_TYPE_TEXT && group.isSupported) {
                                for (i in 0 until group.mediaTrackGroup.length) {
                                    val format = group.mediaTrackGroup.getFormat(i)
                                    format.language?.let { langs.add(it) }
                                }
                            }
                        }
                        if (langs.isNotEmpty()) availableSubtitleLanguages = langs
                        applySubtitleState(this@apply, subtitlesEnabled, selectedSubtitleLang)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        errorMsg = "Playback error: ${error.message ?: "Unknown"}"
                        Log.e("PlayerActivity", "Player error", error)
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) errorMsg = ""
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
            }
        playerState.value = player
    }

    // Subtitle handling
    LaunchedEffect(subtitlesEnabled, selectedSubtitleLang) {
        if (tracksReady.value) {
            playerState.value?.let { player ->
                applySubtitleState(player, subtitlesEnabled, selectedSubtitleLang)
            }
        }
    }

    // Position polling
    LaunchedEffect(playerState.value) {
        while (true) {
            delay(250)
            playerState.value?.let { p ->
                currentPosition = p.currentPosition
                totalDuration = p.duration.coerceAtLeast(1L)
                streamData?.intro?.let {
                    showSkipIntro = currentPosition in it.start until it.end && !isLocked
                }
                streamData?.outro?.let {
                    showSkipOutro = currentPosition in it.start until it.end && !isLocked
                }
            }
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isLocked, showSettings, isPlaying) {
        if (showControls && !isLocked && !showSettings && isPlaying) {
            val startTime = System.currentTimeMillis()
            lastInteractionTime = startTime
            delay(4000)
            if (lastInteractionTime == startTime && !showSettings) {
                showControls = false
            }
        }
    }

    fun resetControlsTimer() {
        lastInteractionTime = System.currentTimeMillis()
        if (!showControls) showControls = true
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            playerState.value?.release()
            playerState.value = null
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun seekBy(ms: Long) {
        playerState.value?.let { p ->
            val newPos = (p.currentPosition + ms).coerceIn(0L, totalDuration)
            p.seekTo(newPos)
            seekFeedback = if (ms > 0) "+${ms / 1000}s" else "${ms / 1000}s"
            resetControlsTimer()
        }
    }

    // UI
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { if (!isLocked) resetControlsTimer() }
            }
    ) {
        // Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    resizeMode = aspectModes[aspectRatioMode]
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.player = playerState.value
                view.resizeMode = aspectModes[aspectRatioMode]
            }
        )

        // Loading
        if (isLoading) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = Color(0xFF9B59F5), strokeWidth = 3.dp)
                    Text("Loading stream…", color = Color(0xFFCCCCCC), fontSize = 14.sp)
                }
            }
        }

        // Error
        if (errorMsg.isNotEmpty() && !isLoading) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFFF4444), modifier = Modifier.size(56.dp))
                    Text("Stream Unavailable", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(errorMsg, color = Color(0xFF888888), fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                isLoading = true
                                errorMsg = ""
                                scope.launch {
                                    try {
                                        streamData = fetchStreamData(episodeId, category)
                                    } catch (e: Exception) {
                                        errorMsg = e.message ?: "Retry failed"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59F5)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                        OutlinedButton(
                            onClick = { (context as? Activity)?.finish() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }

        // Buffering
        if (isBuffering && !isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = Color(0xFF9B59F5),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Double-tap seek zones
        if (!isLocked) {
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { seekBy(-10_000L); resetControlsTimer() },
                                onTap = { resetControlsTimer() }
                            )
                        }
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { seekBy(10_000L); resetControlsTimer() },
                                onTap = { resetControlsTimer() }
                            )
                        }
                )
            }
        }

        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .pointerInput(Unit) {
                        detectTapGestures { resetControlsTimer() }
                    }
            ) {
                // Top bar
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent), endY = 140f))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(
                            episodeTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text("Episode $episodeNumber", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    }
                    if (!isLocked) {
                        IconButton(
                            onClick = {
                                showSettings = !showSettings
                                resetControlsTimer()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Settings, null, tint = Color.White)
                        }
                    }
                    IconButton(
                        onClick = {
                            isLocked = !isLocked
                            resetControlsTimer()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            null,
                            tint = if (isLocked) Color(0xFF9B59F5) else Color.White
                        )
                    }
                }

                // Center controls
                if (!isLocked) {
                    Row(
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { seekBy(-10_000L); resetControlsTimer() },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            Text("-10s", color = Color(0xFFCCCCCC), fontSize = 10.sp)
                        }

                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF9B59F5))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple()
                                ) {
                                    playerState.value?.let { it.playWhenReady = !isPlaying }
                                    resetControlsTimer()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { seekBy(10_000L); resetControlsTimer() },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            Text("+10s", color = Color(0xFFCCCCCC), fontSize = 10.sp)
                        }
                    }
                }

                // Bottom bar + progress
                if (!isLocked) {
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000)), startY = 60f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                    ) {
                        val progress = if (isSeeking)
                            (seekDragPosition.toFloat() / totalDuration).coerceIn(0f, 1f)
                        else
                            (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f)

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .onGloballyPositioned { sliderWidthPx = it.size.width.toFloat() }
                                .pointerInput(totalDuration, sliderWidthPx) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { offset ->
                                            if (sliderWidthPx > 0) {
                                                isSeeking = true
                                                seekDragPosition = ((offset.x / sliderWidthPx) * totalDuration).toLong()
                                            }
                                        },
                                        onDragEnd = {
                                            playerState.value?.seekTo(seekDragPosition)
                                            isSeeking = false
                                            resetControlsTimer()
                                        },
                                        onDragCancel = {
                                            isSeeking = false
                                            resetControlsTimer()
                                        },
                                        onHorizontalDrag = { _, dragAmount ->
                                            if (sliderWidthPx > 0) {
                                                val delta = (dragAmount / sliderWidthPx * totalDuration).toLong()
                                                seekDragPosition = (seekDragPosition + delta).coerceIn(0L, totalDuration)
                                            }
                                        }
                                    )
                                }
                                .pointerInput(totalDuration, sliderWidthPx) {
                                    detectTapGestures { offset ->
                                        if (sliderWidthPx > 0) {
                                            val p = (offset.x / sliderWidthPx).coerceIn(0f, 1f)
                                            playerState.value?.seekTo((p * totalDuration).toLong())
                                            resetControlsTimer()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Track background
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x44FFFFFF))
                            )
                            // Track fill
                            Box(
                                Modifier
                                    .fillMaxWidth(progress)
                                    .height(4.dp)
                                    .align(Alignment.CenterStart)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF9B59F5))
                            )
                            // Thumb
                            Box(
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .offset {
                                        val thumbOffset = (progress * sliderWidthPx) - with(density) { 6.dp.toPx() }
                                        IntOffset(thumbOffset.toInt(), 0)
                                    }
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .shadow(2.dp, CircleShape)
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                formatTime(if (isSeeking) seekDragPosition else currentPosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                formatTime(totalDuration),
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Skip buttons
        val skipButtonPadding = 24.dp
        val skipButtonBottom = 80.dp + if (showControls && !isLocked) 60.dp else 0.dp

        val gradientBrush = Brush.linearGradient(listOf(Color(0xFF9B59F5), Color(0xFF7B3FD5)))

        if (showSkipIntro && !showSkipOutro) {
            OutlinedButton(
                onClick = {
                    streamData?.intro?.let { playerState.value?.seekTo(it.end) }
                    resetControlsTimer()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = skipButtonPadding, bottom = skipButtonBottom),
                border = BorderStroke(width = 2.dp, brush = gradientBrush),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color(0x80000000)
                )
            ) {
                Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Skip Intro", fontWeight = FontWeight.SemiBold)
            }
        }

        if (showSkipOutro) {
            OutlinedButton(
                onClick = {
                    streamData?.outro?.let { playerState.value?.seekTo(it.end) }
                    resetControlsTimer()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = skipButtonPadding, bottom = skipButtonBottom),
                border = BorderStroke(width = 2.dp, brush = gradientBrush),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color(0x80000000)
                )
            ) {
                Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Skip Outro", fontWeight = FontWeight.SemiBold)
            }
        }

        // Seek feedback
        AnimatedVisibility(
            visible = seekFeedback.isNotEmpty(),
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            LaunchedEffect(seekFeedback) {
                delay(600)
                seekFeedback = ""
            }
            Box(
                Modifier
                    .background(Color(0xBB000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(seekFeedback, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Settings panel
        if (showSettings) {
            SettingsPanel(
                hasSubtitles = availableSubtitleLanguages.isNotEmpty() || (streamData?.subtitles?.isNotEmpty() == true),
                subtitlesEnabled = subtitlesEnabled,
                onSubtitlesToggle = { enabled ->
                    subtitlesEnabled = enabled
                    if (enabled && availableSubtitleLanguages.isNotEmpty()) {
                        selectedSubtitleLang = availableSubtitleLanguages.firstOrNull { it.startsWith("en") }
                            ?: availableSubtitleLanguages.first()
                    }
                },
                currentSpeed = currentSpeed,
                onSpeedChange = { speed ->
                    currentSpeed = speed
                    playerState.value?.setPlaybackParameters(PlaybackParameters(speed))
                },
                aspectRatioMode = aspectRatioMode,
                onAspectChange = { aspectRatioMode = it },
                onDismiss = { showSettings = false }
            )
        }
    }
}
