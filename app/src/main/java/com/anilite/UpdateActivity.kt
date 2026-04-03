package com.anilite

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.FileProvider
import com.anilite.ui.theme.AnidakuTheme
import com.anilite.ui.theme.Purple40
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apkUrl = intent.getStringExtra("apk_url") ?: UpdateChecker.APK_DOWNLOAD_URL
        val latestVersion = intent.getStringExtra("latest_version") ?: ""

        setContent {
            AnidakuTheme {
                UpdateScreen(
                    latestVersion = latestVersion,
                    onUpdate = { onProgress, onDone, onError ->
                        downloadApk(apkUrl, onProgress, onDone, onError)
                    }
                )
            }
        }
    }

    private fun downloadApk(
        apkUrl: String,
        onProgress: (Int) -> Unit,
        onDone: (File) -> Unit,
        onError: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(apkUrl).openConnection() as HttpURLConnection
                connection.connect()
                val fileLength = connection.contentLength
                val apkFile = File(
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "anidaku_update.apk"
                )
                val output = FileOutputStream(apkFile)
                val input = connection.inputStream
                val buffer = ByteArray(4096)
                var downloaded = 0L
                var count: Int

                while (input.read(buffer).also { count = it } != -1) {
                    downloaded += count
                    output.write(buffer, 0, count)
                    if (fileLength > 0) {
                        val progress = (downloaded * 100 / fileLength).toInt()
                        withContext(Dispatchers.Main) { onProgress(progress) }
                    }
                }
                output.flush(); output.close(); input.close()
                withContext(Dispatchers.Main) { onDone(apkFile) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError() }
            }
        }
    }

    fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

@Composable
fun UpdateScreen(
    latestVersion: String,
    onUpdate: (
        onProgress: (Int) -> Unit,
        onDone: (File) -> Unit,
        onError: () -> Unit
    ) -> Unit
) {
    val activity = androidx.compose.ui.platform.LocalContext.current as UpdateActivity

    var progress by remember { mutableStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    // Pulse animation for the icon
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val bgColor = Color(0xFF0D0D0D)
    val cardColor = Color(0xFF1A1A2E)
    val accentColor = Purple40
    val softPurple = Color(0xFF9C89B8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Icon circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(if (!isDownloading && !isDone) scale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⬇", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isDone) "All Set! 🎉" else "Update Available",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (latestVersion.isNotEmpty()) {
                Text(
                    text = "Version $latestVersion",
                    fontSize = 13.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = when {
                    isDone -> "Update complete! Tap below to install."
                    isDownloading -> "Downloading update, please wait..."
                    isError -> "Download failed. Check connection and retry."
                    else -> "A new version of Anidaku is ready.\nUpdate now to get the latest features."
                },
                fontSize = 14.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Progress card
            if (isDownloading || isDone) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isDone) "Complete" else "Downloading...",
                                fontSize = 13.sp,
                                color = softPurple
                            )
                            Text(
                                text = "$progress%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = accentColor,
                            trackColor = Color(0xFF2A2A3E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Main button
            Button(
                onClick = {
                    when {
                        isDone -> downloadedFile?.let { activity.installApk(it) }
                        isError -> {
                            isError = false
                            isDownloading = true
                            progress = 0
                            onUpdate(
                                { p -> progress = p },
                                { file -> isDone = true; isDownloading = false; downloadedFile = file },
                                { isError = true; isDownloading = false }
                            )
                        }
                        !isDownloading -> {
                            isDownloading = true
                            onUpdate(
                                { p -> progress = p },
                                { file -> isDone = true; isDownloading = false; downloadedFile = file },
                                { isError = true; isDownloading = false }
                            )
                        }
                    }
                },
                enabled = !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = accentColor.copy(alpha = 0.4f)
                )
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Downloading...", fontSize = 15.sp, color = Color.White)
                } else {
                    Text(
                        text = when {
                            isDone -> "Install Now"
                            isError -> "Retry"
                            else -> "Update Now"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Anidaku • Free & Open Source",
                fontSize = 11.sp,
                color = Color(0xFF555555)
            )
        }
    }
}
