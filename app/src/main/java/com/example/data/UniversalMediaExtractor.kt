package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedMediaInfo(
    val title: String,
    val directVideoUrl: String,
    val directAudioUrl: String? = null,
    val durationSeconds: Int = 0,
    val approxSizeBytes: Long = 0,
    val provider: String,
    val quality: String = "1080p / High"
)

sealed class ExtractionResult {
    data class Success(val media: ExtractedMediaInfo) : ExtractionResult()
    data class Error(val message: String) : ExtractionResult()
}

class UniversalMediaExtractor {

    private val tag = "VakaarExtractor"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Public Invidious instances for YouTube
    private val invidiousInstances = listOf(
        "https://inv.tux.pizza",
        "https://invidious.nerdvpn.de",
        "https://invidious.protokolla.fi",
        "https://iv.ggtyler.dev"
    )

    // Public Cobalt instances as secondary fallback
    private val cobaltInstances = listOf(
        "https://cobalt-api.kwiatekm.tokyo",
        "https://api.wuk.sh",
        "https://api.cobalt.tools"
    )

    suspend fun extract(
        url: String,
        platform: PlatformType,
        format: MediaFormat
    ): ExtractionResult = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return@withContext ExtractionResult.Error("URL cannot be empty")
        }

        // 1. Check if already a direct media file (e.g. .mp4, .mp3, or direct CDN stream)
        val directCheck = checkDirectMediaLink(trimmed)
        if (directCheck != null) {
            return@withContext ExtractionResult.Success(directCheck)
        }

        // 2. Dispatch based on detected or selected platform
        try {
            when {
                // YouTube
                trimmed.contains("youtube.com") || trimmed.contains("youtu.be") -> {
                    return@withContext extractYouTube(trimmed)
                }

                // TikTok
                trimmed.contains("tiktok.com") -> {
                    return@withContext extractTikTok(trimmed)
                }

                // Twitter / X
                trimmed.contains("twitter.com") || trimmed.contains("x.com") -> {
                    return@withContext extractTwitter(trimmed)
                }

                // Instagram
                trimmed.contains("instagram.com") || trimmed.contains("instagr.am") -> {
                    return@withContext extractInstagram(trimmed)
                }

                // All other platforms: Try Cobalt cluster
                else -> {
                    val cobaltRes = extractViaCobalt(trimmed, format)
                    if (cobaltRes is ExtractionResult.Success) {
                        return@withContext cobaltRes
                    }
                    return@withContext ExtractionResult.Error(
                        "Unable to decode stream from this URL. Please verify the link is public."
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Extraction exception: ${e.message}", e)
            return@withContext ExtractionResult.Error(
                e.message ?: "Network or extraction error. Please check your internet connection."
            )
        }
    }

    private fun checkDirectMediaLink(url: String): ExtractedMediaInfo? {
        val lower = url.lowercase()
        val isDirectExtension = lower.endsWith(".mp4") || lower.endsWith(".mp3") ||
                lower.endsWith(".m4a") || lower.endsWith(".mov") ||
                lower.endsWith(".webm") || lower.endsWith(".mkv")

        if (isDirectExtension) {
            val fileName = url.substringAfterLast("/").substringBefore("?")
            return ExtractedMediaInfo(
                title = if (fileName.isNotBlank()) fileName else "Direct_Stream",
                directVideoUrl = url,
                directAudioUrl = if (lower.endsWith(".mp3") || lower.endsWith(".m4a")) url else null,
                provider = "Direct CDN Stream",
                quality = "Source Quality"
            )
        }
        return null
    }

    private fun extractYouTube(url: String): ExtractionResult {
        // Extract video ID from youtube.com/watch?v=ID or youtu.be/ID or youtube.com/shorts/ID
        val pattern = Pattern.compile("(?:v=|youtu\\.be/|shorts/|embed/)([a-zA-Z0-9_-]{11})")
        val matcher = pattern.matcher(url)
        val videoId = if (matcher.find()) matcher.group(1) else null

        if (videoId == null) {
            return ExtractionResult.Error("Invalid YouTube URL. Could not parse video ID.")
        }

        // Query Invidious API
        for (instance in invidiousInstances) {
            try {
                val apiUrl = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder()
                    .url(apiUrl)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .get()
                    .build()

                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val title = json.optString("title", "YouTube_Video_$videoId")
                        val lengthSeconds = json.optInt("lengthSeconds", 0)

                        // Format streams has combined video+audio
                        val formatStreams: JSONArray? = json.optJSONArray("formatStreams")
                        var bestVideoUrl: String? = null
                        var bestAudioUrl: String? = null
                        var approxSize: Long = 0

                        if (formatStreams != null && formatStreams.length() > 0) {
                            for (i in 0 until formatStreams.length()) {
                                val item = formatStreams.getJSONObject(i)
                                val itemUrl = item.optString("url")
                                val size = item.optLong("size", 0L)
                                val clen = item.optLong("contentLength", size)
                                if (itemUrl.isNotBlank()) {
                                    bestVideoUrl = itemUrl
                                    approxSize = clen
                                    break
                                }
                            }
                        }

                        // Adaptive formats for audio
                        val adaptiveFormats: JSONArray? = json.optJSONArray("adaptiveFormats")
                        if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                            for (i in 0 until adaptiveFormats.length()) {
                                val item = adaptiveFormats.getJSONObject(i)
                                val mimeType = item.optString("type", "")
                                if (mimeType.contains("audio/")) {
                                    bestAudioUrl = item.optString("url")
                                    if (bestVideoUrl == null) {
                                        approxSize = item.optLong("clen", 0L)
                                    }
                                    break
                                }
                            }
                        }

                        if (!bestVideoUrl.isNullOrBlank()) {
                            return ExtractionResult.Success(
                                ExtractedMediaInfo(
                                    title = title,
                                    directVideoUrl = bestVideoUrl,
                                    directAudioUrl = bestAudioUrl,
                                    durationSeconds = lengthSeconds,
                                    approxSizeBytes = approxSize,
                                    provider = "YouTube HD Stream",
                                    quality = "720p / 1080p MP4"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Invidious instance $instance failed: ${e.message}")
            }
        }

        // Fallback to Cobalt
        return extractViaCobalt(url, MediaFormat.ORIGINAL_MP4)
    }

    private fun extractTikTok(url: String): ExtractionResult {
        try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val apiUrl = "https://www.tikwm.com/api/?url=$encoded"

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    val code = json.optInt("code", -1)
                    if (code == 0) {
                        val data = json.getJSONObject("data")
                        val title = data.optString("title", "TikTok_Video")
                        val playUrl = data.optString("play", "")
                        val musicUrl = data.optString("music", "")
                        val duration = data.optInt("duration", 0)
                        val size = data.optLong("size", 0L)

                        if (playUrl.isNotBlank()) {
                            // Ensure url has scheme
                            val fullVideoUrl = if (playUrl.startsWith("http")) playUrl else "https://www.tikwm.com$playUrl"
                            val fullAudioUrl = if (musicUrl.isNotBlank() && musicUrl.startsWith("http")) musicUrl else if (musicUrl.isNotBlank()) "https://www.tikwm.com$musicUrl" else null

                            return ExtractionResult.Success(
                                ExtractedMediaInfo(
                                    title = title,
                                    directVideoUrl = fullVideoUrl,
                                    directAudioUrl = fullAudioUrl,
                                    durationSeconds = duration,
                                    approxSizeBytes = size,
                                    provider = "TikTok Clean Stream (No Watermark)",
                                    quality = "HD Original"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "TikWM error: ${e.message}")
        }

        return extractViaCobalt(url, MediaFormat.ORIGINAL_MP4)
    }

    private fun extractTwitter(url: String): ExtractionResult {
        try {
            // Extract tweet/status ID
            val pattern = Pattern.compile("status/([0-9]+)")
            val matcher = pattern.matcher(url)
            val tweetId = if (matcher.find()) matcher.group(1) else null

            if (tweetId != null) {
                val apiUrl = "https://api.vxtwitter.com/Twitter/status/$tweetId"
                val request = Request.Builder()
                    .url(apiUrl)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .get()
                    .build()

                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val mediaArr = json.optJSONArray("media_extended")
                        if (mediaArr != null && mediaArr.length() > 0) {
                            for (i in 0 until mediaArr.length()) {
                                val item = mediaArr.getJSONObject(i)
                                val type = item.optString("type")
                                if (type == "video" || type == "gif") {
                                    val videoUrl = item.optString("url")
                                    val text = json.optString("text", "Twitter_Video_$tweetId")
                                    if (videoUrl.isNotBlank()) {
                                        return ExtractionResult.Success(
                                            ExtractedMediaInfo(
                                                title = text.take(60),
                                                directVideoUrl = videoUrl,
                                                directAudioUrl = null,
                                                provider = "Twitter / X CDN",
                                                quality = "High Bitrate MP4"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "vxtwitter error: ${e.message}")
        }

        return extractViaCobalt(url, MediaFormat.ORIGINAL_MP4)
    }

    private fun extractInstagram(url: String): ExtractionResult {
        // Extract shortcode
        val pattern = Pattern.compile("(?:reel|p|tv)/([A-Za-z0-9_-]+)")
        val matcher = pattern.matcher(url)
        val shortcode = if (matcher.find()) matcher.group(1) else null

        if (shortcode != null) {
            try {
                // DDInstagram exposes the unauthenticated OpenGraph direct video link
                val ddUrl = "https://www.ddinstagram.com/reel/$shortcode/"
                val request = Request.Builder()
                    .url(ddUrl)
                    .addHeader("User-Agent", "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)")
                    .get()
                    .build()

                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val html = resp.body?.string() ?: ""
                        // Look for og:video or og:video:secure_url
                        val videoPattern = Pattern.compile("<meta\\s+(?:property|name)=[\"']og:video[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
                        val videoMatcher = videoPattern.matcher(html)
                        if (videoMatcher.find()) {
                            val directVideoUrl = videoMatcher.group(1)?.replace("&amp;", "&")
                            if (!directVideoUrl.isNullOrBlank() && directVideoUrl.startsWith("http")) {
                                return ExtractionResult.Success(
                                    ExtractedMediaInfo(
                                        title = "Instagram_Reel_$shortcode",
                                        directVideoUrl = directVideoUrl,
                                        directAudioUrl = null,
                                        provider = "Instagram Meta CDN",
                                        quality = "Original MP4"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "ddinstagram error: ${e.message}")
            }
        }

        return extractViaCobalt(url, MediaFormat.ORIGINAL_MP4)
    }

    private fun extractViaCobalt(targetUrl: String, format: MediaFormat): ExtractionResult {
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val downloadMode = if (format == MediaFormat.AUDIO_MP3) "audio" else "auto"
        val requestPayload = JSONObject().apply {
            put("url", targetUrl)
            put("videoQuality", "1080")
            put("audioFormat", "mp3")
            put("downloadMode", downloadMode)
            put("filenameStyle", "classic")
        }.toString()

        for (instance in cobaltInstances) {
            try {
                val request = Request.Builder()
                    .url(instance)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .post(requestPayload.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val status = json.optString("status", "")
                        val streamUrl = json.optString("url", "")

                        if (streamUrl.isNotBlank() && (status == "tunnel" || status == "redirect" || status == "stream")) {
                            return ExtractionResult.Success(
                                ExtractedMediaInfo(
                                    title = json.optString("filename", "Extracted_Media"),
                                    directVideoUrl = streamUrl,
                                    directAudioUrl = if (format == MediaFormat.AUDIO_MP3) streamUrl else null,
                                    provider = "Cobalt Decryption Pipeline",
                                    quality = "1080p Stream"
                                )
                            )
                        }

                        // Picker
                        val picker = json.optJSONArray("picker")
                        if (picker != null && picker.length() > 0) {
                            val first = picker.getJSONObject(0)
                            val pickUrl = first.optString("url")
                            if (pickUrl.isNotBlank()) {
                                return ExtractionResult.Success(
                                    ExtractedMediaInfo(
                                        title = "Extracted_Media",
                                        directVideoUrl = pickUrl,
                                        provider = "Cobalt Decryption Pipeline",
                                        quality = "High"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Cobalt instance $instance failed: ${e.message}")
            }
        }

        return ExtractionResult.Error(
            "Could not decode stream for this video. Please make sure the post is public and URL is correct."
        )
    }
}
