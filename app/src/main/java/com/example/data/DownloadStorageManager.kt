package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DownloadStorageManager(private val context: Context) {

    private val tag = "VakaarStorage"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun saveMediaFile(
        platform: PlatformType,
        format: MediaFormat,
        inputUrl: String,
        directMediaUrl: String?,
        mediaTitle: String? = null,
        onProgress: (Int, Long, Long, Double) -> Unit // percent, bytesRead, totalBytes, speedMbps
    ): DownloadEntity = withContext(Dispatchers.IO) {
        val streamUrl = directMediaUrl?.trim() ?: inputUrl.trim()
        if (streamUrl.isBlank() || (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://"))) {
            throw IOException("No valid media stream URL was found. Please verify the URL.")
        }

        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
        val cleanPlatform = platform.displayName.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
        val fileName = "vakaar_${cleanPlatform}_${dateStr}.${format.extension}"

        // Dedicated app folder in storage
        val vakaarDir = File(context.getExternalFilesDir(null), "vakaar")
        if (!vakaarDir.exists()) {
            vakaarDir.mkdirs()
        }
        val targetLocalFile = File(vakaarDir, fileName)

        Log.d(tag, "Starting real stream download from: $streamUrl into: ${targetLocalFile.absolutePath}")

        val request = Request.Builder()
            .url(streamUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .addHeader("Accept", "*/*")
            .addHeader("Referer", inputUrl)
            .build()

        var totalBytesRead: Long = 0
        var totalBytesExpected: Long = -1

        val startTime = System.currentTimeMillis()
        var lastProgressUpdateTime = startTime

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP error ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw IOException("Empty response body from media server")
                totalBytesExpected = body.contentLength()

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(targetLocalFile)
                val buffer = ByteArray(64 * 1024) // 64KB buffer for fast transfer
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val now = System.currentTimeMillis()
                    // Throttle UI progress updates to every 120ms to avoid UI stutter
                    if (now - lastProgressUpdateTime > 120 || totalBytesRead == totalBytesExpected) {
                        val elapsedSec = (now - startTime).coerceAtLeast(1) / 1000.0
                        val currentSpeedMbps = (totalBytesRead / (elapsedSec * 1024.0 * 1024.0))

                        val percent = if (totalBytesExpected > 0) {
                            ((totalBytesRead * 100) / totalBytesExpected).toInt().coerceIn(0, 99)
                        } else {
                            50 // Indeterminate stream
                        }

                        onProgress(percent, totalBytesRead, totalBytesExpected, currentSpeedMbps)
                        lastProgressUpdateTime = now
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
            }
        } catch (e: Exception) {
            // Delete corrupt incomplete file if download failed
            if (targetLocalFile.exists() && targetLocalFile.length() == 0L) {
                targetLocalFile.delete()
            }
            Log.e(tag, "Download failed: ${e.message}", e)
            throw IOException("Download failed: ${e.message ?: "Connection error"}")
        }

        val downloadedFileSize = targetLocalFile.length()
        if (downloadedFileSize == 0L) {
            throw IOException("Download failed: received 0 bytes from server")
        }

        // Final 100% progress notification
        val totalElapsedSec = (System.currentTimeMillis() - startTime).coerceAtLeast(1) / 1000.0
        val avgSpeedMbps = (downloadedFileSize / (totalElapsedSec * 1024.0 * 1024.0))
        onProgress(100, downloadedFileSize, downloadedFileSize, avgSpeedMbps)

        // Mirror to Android public Downloads/vakaar directory via MediaStore
        var publicPath = targetLocalFile.absolutePath
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/vakaar")
                }
                val contentUri = if (format.isAudio) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }
                val uri: Uri? = context.contentResolver.insert(contentUri, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        targetLocalFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    publicPath = "/sdcard/Download/vakaar/$fileName"
                }
            } else {
                @Suppress("DEPRECATION")
                val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "vakaar")
                if (!publicDir.exists()) publicDir.mkdirs()
                val destFile = File(publicDir, fileName)
                targetLocalFile.copyTo(destFile, overwrite = true)
                publicPath = destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(tag, "Could not mirror to public MediaStore: ${e.message}")
        }

        val formattedSize = formatFileSize(downloadedFileSize)
        val displayTitle = if (!mediaTitle.isNullOrBlank()) {
            mediaTitle.take(45)
        } else {
            "${platform.displayName} Video (${format.title})"
        }

        DownloadEntity(
            title = displayTitle,
            originalUrl = inputUrl,
            platformName = platform.displayName,
            formatName = format.title,
            fileSizeBytes = downloadedFileSize,
            formattedSize = formattedSize,
            filePath = publicPath,
            timestamp = timestamp,
            isAudio = format.isAudio
        )
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f GB", bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / 1024.0)
            else -> "$bytes B"
        }
    }
}
