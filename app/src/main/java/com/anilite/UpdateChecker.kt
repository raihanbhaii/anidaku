package com.anilite

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val GITHUB_API =
        "https://api.github.com/repos/raihanbhaii/anidaku-apk/releases/latest"
    const val APK_DOWNLOAD_URL =
        "https://github.com/raihanbhaii/anidaku-apk/releases/latest/download/app-release.apk"

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String = "",
        val apkUrl: String = APK_DOWNLOAD_URL
    )

    fun check(context: Context): UpdateInfo {
        return try {
            val connection = URL(GITHUB_API).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 6000
            connection.readTimeout = 6000

            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            val latestVersion = json.getString("tag_name").trimStart('v')

            val currentVersion = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName
                ?.trimStart('v') ?: "0"

            UpdateInfo(
                hasUpdate = latestVersion != currentVersion,
                latestVersion = latestVersion,
                apkUrl = APK_DOWNLOAD_URL
            )
        } catch (e: Exception) {
            UpdateInfo(hasUpdate = false)
        }
    }
}
