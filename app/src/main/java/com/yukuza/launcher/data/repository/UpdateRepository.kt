package com.yukuza.launcher.data.repository

import com.yukuza.launcher.BuildConfig
import com.yukuza.launcher.data.remote.GithubReleasesApi
import com.yukuza.launcher.domain.model.UpdateInfo
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val api: GithubReleasesApi,
    private val currentVersion: String = BuildConfig.VERSION_NAME,
) {
    suspend fun checkForUpdate(): UpdateInfo? = try {
        withTimeout(10_000) {
            val release = api.getLatestRelease()
            val remoteVersion = release.tagName.removePrefix("v")

            if (!isNewerVersion(remoteVersion, currentVersion)) return@withTimeout null

            val apkAsset = release.assets
                .filter { it.name.endsWith(".apk") }
                .let { apks -> apks.firstOrNull { it.name.contains("release") } ?: apks.firstOrNull() }
                ?: return@withTimeout null

            UpdateInfo(
                latestVersion = remoteVersion,
                downloadUrl = apkAsset.browserDownloadUrl,
                releaseNotes = release.body,
            )
        }
    } catch (e: Exception) {
        null
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: return false }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: return false }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
