package com.language.finebulary.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.language.finebulary.BuildConfig
import com.language.finebulary.data.api.GitHubApiService
import com.language.finebulary.data.api.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    private val gitHubApiService: GitHubApiService
) {
    suspend fun checkForUpdate(): GitHubRelease? {
        return try {
            val release = withContext(Dispatchers.IO) {
                gitHubApiService.getLatestRelease()
            }
            val remoteVersion = release.tagName.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

            if (isNewerVersion(remoteVersion, currentVersion)) {
                release
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        return try {
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrNull(i) ?: 0
                val c = currentParts.getOrNull(i) ?: 0
                if (r > c) return true
                if (r < c) return false
            }
            false
        } catch (_: Exception) {
            remote != current
        }
    }

    fun downloadAndInstallApk(context: Context, apkUrl: String, releaseName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Finebulary Update ($releaseName)")
                .setDescription("Downloading latest version...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Finebulary-$releaseName.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started. Check notifications when complete.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to start download: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
