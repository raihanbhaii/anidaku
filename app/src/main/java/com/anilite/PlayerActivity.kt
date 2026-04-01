package com.anilite

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import com.anilite.ui.theme.AnidakuTheme
import com.anilite.ui.theme.Purple40

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val playerUrl = intent.getStringExtra("playerUrl") ?: ""
        val episodeTitle = intent.getStringExtra("episodeTitle") ?: ""
        val episodeNumber = intent.getIntExtra("episodeNumber", 1)

        setContent {
            AnidakuTheme {
                PlayerScreen(
                    playerUrl = playerUrl,
                    episodeTitle = episodeTitle,
                    episodeNumber = episodeNumber,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun PlayerScreen(
    playerUrl: String,
    episodeTitle: String,
    episodeNumber: Int,
    onBack: () -> Unit
) {
    var category by remember { mutableStateOf("sub") }   // sub or dub
    var showControls by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(playerUrl) }

    // Rebuild URL when category changes
    LaunchedEffect(category) {
        if (playerUrl.contains("/s-2/")) {
            // Replace sub/dub at the end
            currentUrl = playerUrl.substringBeforeLast("/") + "/$category"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // MegaPlay Embed using WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36"
                    }

                    webViewClient = WebViewClient()
                    loadUrl(currentUrl)

                    // Make it fullscreen
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { webView ->
                webView.loadUrl(currentUrl)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Controls (visible when showControls = true)
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }

                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(
                            text = episodeTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "Episode $episodeNumber",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    // Sub / Dub Toggle
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1C1C28), RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("sub", "dub").forEach { cat ->
                            val isSelected = category == cat
                            Text(
                                text = cat.uppercase(),
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .background(
                                        if (isSelected) Purple40 else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { category = cat }
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Bottom hint
                Text(
                    text = "Tap screen to show/hide controls",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            }
        }
    }
}
