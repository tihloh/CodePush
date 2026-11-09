package com.qrdatasender.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AvailableUpdate(
    val version: String,
    val releaseUrl: String,
    val apkUrl: String?
)

/** Checks published stable GitHub releases. No APK is installed without Android's confirmation. */
object UpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun check(installedVersion: String): AvailableUpdate? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/tihloh/CodePush/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "CodePush-Android")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Could not check GitHub releases (HTTP ${response.code}).")
            }
            val body = response.body?.string() ?: throw IOException("Empty GitHub release response.")
            val release = JSONObject(body)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@withContext null
            val version = release.optString("tag_name").removePrefix("v")
            if (!isNewer(version, installedVersion)) return@withContext null
            val releaseUrl = release.optString("html_url")
            if (!releaseUrl.startsWith("https://github.com/tihloh/CodePush/releases/")) {
                throw IOException("Invalid release URL.")
            }
            val assets = release.optJSONArray("assets")
            var apk: String? = null
            if (assets != null) for (i in 0 until assets.length()) {
                val item = assets.optJSONObject(i) ?: continue
                val name = item.optString("name")
                val url = item.optString("browser_download_url")
                if (name.endsWith(".apk", ignoreCase = true) &&
                    !name.contains("debug", ignoreCase = true) &&
                    url.startsWith("https://github.com/tihloh/CodePush/releases/download/")) {
                    apk = url
                    break
                }
            }
            AvailableUpdate(version, releaseUrl, apk)
        }
    }

    internal fun isNewer(candidate: String, installed: String): Boolean {
        fun parts(version: String): List<Int>? {
            val core = version.removePrefix("v")
            if (!Regex("\\d+(\\.\\d+){1,3}").matches(core)) return null
            return core.split('.').map { it.toIntOrNull() ?: return null }
        }
        val a = parts(candidate) ?: return false
        val b = parts(installed) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrElse(i) { 0 }).compareTo(b.getOrElse(i) { 0 })
            if (comparison != 0) return comparison > 0
        }
        return false
    }
}
