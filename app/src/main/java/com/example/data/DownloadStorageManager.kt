package com.example.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
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
import java.io.InputStream
import java.io.OutputStream
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

        // Primary public folder requested by user: /sdcard/Download/vakaar/
        val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicVakaarDir = File(publicDownloadsDir, "vakaar")
        if (!publicVakaarDir.exists()) {
            publicVakaarDir.mkdirs()
        }
        val targetPublicFile = File(publicVakaarDir, fileName)

        // Internal backup cache folder in case of Android 11+ direct write restriction
        val appVakaarDir = File(context.getExternalFilesDir(null), "vakaar")
        if (!appVakaarDir.exists()) {
            appVakaarDir.mkdirs()
        }
        val appLocalFile = File(appVakaarDir, fileName)

        Log.d(tag, "Initiating download: $fileName from: $streamUrl")

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

        // Determine destination stream: try direct public file first, fallback to app local
        var chosenFile = targetPublicFile
        var outputStream: OutputStream? = null

        try {
            outputStream = FileOutputStream(targetPublicFile)
            chosenFile = targetPublicFile
        } catch (e: Exception) {
            Log.w(tag, "Direct public file write restricted, writing to app local first: ${e.message}")
            outputStream = FileOutputStream(appLocalFile)
            chosenFile = appLocalFile
        }

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        404 -> "URL NOT FOUND / BROKEN LINK: Video server par nahi mila ya delete ho chuka hai (Error 404)."
                        403, 401 -> "PRIVATE / ACCESS RESTRICTED: Ye video private account ka hai ya login ke bina download nahi ho sakta (Error 403)."
                        410 -> "URL EXPIRED: Video stream link expire ho chuka hai. Dobara try karein."
                        429 -> "SERVER BUSY: Too many requests. Kripya 1 minute baad try karein."
                        in 500..599 -> "SERVER DOWN: Video host server abhi down hai (Error ${response.code})."
                        else -> "HTTP ERROR ${response.code}: Video server se download nahi ho paya."
                    }
                    throw IOException(errorMsg)
                }

                val body = response.body ?: throw IOException("SERVER ERROR: Empty response from video server")
                totalBytesExpected = body.contentLength()

                val inputStream: InputStream = body.byteStream()
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdateTime > 120 || totalBytesRead == totalBytesExpected) {
                        val elapsedSec = (now - startTime).coerceAtLeast(1) / 1000.0
                        val currentSpeedMbps = (totalBytesRead / (elapsedSec * 1024.0 * 1024.0))

                        val percent = if (totalBytesExpected > 0) {
                            ((totalBytesRead * 100) / totalBytesExpected).toInt().coerceIn(0, 99)
                        } else {
                            50
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
            try { outputStream?.close() } catch (_: Exception) {}
            if (chosenFile.exists() && chosenFile.length() == 0L) {
                chosenFile.delete()
            }
            Log.e(tag, "Stream fetch failed: ${e.message}", e)
            val friendlyMsg = when (e) {
                is java.net.UnknownHostException -> "NO INTERNET: Internet connection nahi mil raha hai. Wi-Fi ya Mobile Data check karein."
                is java.net.SocketTimeoutException -> "CONNECTION TIMEOUT: Video server ne response nahi diya. Internet speed check karein."
                is java.net.ConnectException -> "SERVER UNREACHABLE: Video server se connection fail ho gaya."
                else -> e.message ?: "Download connection interrupted"
            }
            throw IOException(friendlyMsg)
        }

        val downloadedFileSize = chosenFile.length()
        if (downloadedFileSize == 0L) {
            throw IOException("URL NOT FOUND / EMPTY FILE: Server se koi data nahi mila. Video link check karein.")
        }

        // Final 100% progress notification
        val totalElapsedSec = (System.currentTimeMillis() - startTime).coerceAtLeast(1) / 1000.0
        val avgSpeedMbps = (downloadedFileSize / (totalElapsedSec * 1024.0 * 1024.0))
        onProgress(100, downloadedFileSize, downloadedFileSize, avgSpeedMbps)

        // Make sure file is placed in public Downloads/vakaar folder and visible to user
        var publicFinalPath = targetPublicFile.absolutePath

        // 1. If we downloaded to appLocalFile, copy it to targetPublicFile if possible
        if (chosenFile == appLocalFile) {
            try {
                if (!publicVakaarDir.exists()) publicVakaarDir.mkdirs()
                appLocalFile.copyTo(targetPublicFile, overwrite = true)
                publicFinalPath = targetPublicFile.absolutePath
            } catch (e: Exception) {
                Log.w(tag, "Direct copy to public folder failed: ${e.message}")
            }
        }

        // 2. Also register in MediaStore.Downloads with RELATIVE_PATH = "Download/vakaar" (Scoped Storage compliant for Android 10+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/vakaar")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                // MediaStore.Downloads handles Download/ relative directory without throwing IllegalArgumentException
                val contentUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri: Uri? = context.contentResolver.insert(contentUri, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        chosenFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                    publicFinalPath = targetPublicFile.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "MediaStore.Downloads indexing error: ${e.message}")
        }

        // 3. Scan file so system media scanner immediately indexes it
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(publicFinalPath, targetPublicFile.absolutePath),
                arrayOf(format.mimeType),
                null
            )
        } catch (e: Exception) {
            Log.w(tag, "MediaScanner scan error: ${e.message}")
        }

        // Keep a copy in app local storage as well for guaranteed FileProvider access
        if (chosenFile == targetPublicFile && !appLocalFile.exists()) {
            try {
                targetPublicFile.copyTo(appLocalFile, overwrite = true)
            } catch (_: Exception) {}
        }

        val formattedSize = formatFileSize(downloadedFileSize)
        val displayTitle = if (!mediaTitle.isNullOrBlank()) {
            mediaTitle.take(45)
        } else {
            "${platform.displayName} Media"
        }

        DownloadEntity(
            title = displayTitle,
            originalUrl = inputUrl,
            platformName = platform.displayName,
            formatName = format.title,
            fileSizeBytes = downloadedFileSize,
            formattedSize = formattedSize,
            filePath = publicFinalPath,
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
