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
import java.util.concurrent.TimeUnit

sealed class CobaltResult {
    data class Success(
        val streamUrl: String,
        val title: String?,
        val filename: String?,
        val instanceUsed: String
    ) : CobaltResult()

    data class Error(
        val message: String,
        val details: String? = null
    ) : CobaltResult()
}

class CobaltApiService {

    private val tag = "CobaltApi"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    // Multi-instance list for resilience & failover
    private val cobaltInstances = listOf(
        "https://api.cobalt.tools",
        "https://cobalt-api.kwiatekm.tokyo",
        "https://api.wuk.sh",
        "https://cobalt.api.scaven.me"
    )

    suspend fun extractMediaStream(
        targetUrl: String,
        format: MediaFormat
    ): CobaltResult = withContext(Dispatchers.IO) {
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        val downloadMode = if (format == MediaFormat.AUDIO_MP3) "audio" else "auto"
        val videoQuality = when (format) {
            MediaFormat.HD_MP4 -> "1080"
            MediaFormat.ORIGINAL_MP4 -> "max"
            MediaFormat.AUDIO_MP3 -> "720"
        }

        val requestPayload = JSONObject().apply {
            put("url", targetUrl.trim())
            put("videoQuality", videoQuality)
            put("audioFormat", "mp3")
            put("downloadMode", downloadMode)
            put("filenameStyle", "classic")
            put("youtubeVideoCodec", "h264")
        }.toString()

        var lastErrorMessage = "Failed to reach Cobalt endpoints"

        for (instance in cobaltInstances) {
            try {
                Log.d(tag, "Attempting extraction via Cobalt instance: $instance")
                val request = Request.Builder()
                    .url(instance)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "VakaarCyberEngine/4.0")
                    .post(requestPayload.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    val code = response.code

                    if (code in 200..299 && responseBody.isNotBlank()) {
                        val json = JSONObject(responseBody)
                        val status = json.optString("status", "")

                        when {
                            // Direct stream, tunnel, or redirect URL
                            status == "tunnel" || status == "redirect" || status == "stream" -> {
                                val streamUrl = json.optString("url")
                                val filename = json.optString("filename", null)
                                if (streamUrl.isNotBlank()) {
                                    return@withContext CobaltResult.Success(
                                        streamUrl = streamUrl,
                                        title = filename,
                                        filename = filename,
                                        instanceUsed = instance
                                    )
                                }
                            }
                            // Multi-media picker (e.g., carousel or multiple resolutions)
                            status == "picker" -> {
                                val pickerArray: JSONArray? = json.optJSONArray("picker")
                                if (pickerArray != null && pickerArray.length() > 0) {
                                    val firstItem = pickerArray.getJSONObject(0)
                                    val streamUrl = firstItem.optString("url")
                                    if (streamUrl.isNotBlank()) {
                                        return@withContext CobaltResult.Success(
                                            streamUrl = streamUrl,
                                            title = "Extracted media item 1",
                                            filename = null,
                                            instanceUsed = instance
                                        )
                                    }
                                }
                            }
                            status == "error" -> {
                                val errObj = json.optJSONObject("error")
                                val errText = json.optString("text", "")
                                val errCode = errObj?.optString("code", "") ?: ""
                                lastErrorMessage = if (errText.isNotBlank()) errText else "Cobalt error: $errCode"
                                Log.w(tag, "Instance $instance returned status=error: $lastErrorMessage")
                            }
                            else -> {
                                // Sometimes raw url field is present without status
                                val directUrl = json.optString("url", "")
                                if (directUrl.isNotBlank()) {
                                    return@withContext CobaltResult.Success(
                                        streamUrl = directUrl,
                                        title = null,
                                        filename = null,
                                        instanceUsed = instance
                                    )
                                }
                            }
                        }
                    } else {
                        Log.w(tag, "Instance $instance failed with HTTP $code: $responseBody")
                        lastErrorMessage = "Instance $instance returned HTTP $code"
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Exception querying $instance: ${e.message}")
                lastErrorMessage = e.message ?: "Connection error"
            }
        }

        // If all instances failed
        CobaltResult.Error(
            message = "Cobalt engine: $lastErrorMessage. Please check internet connection or target link."
        )
    }
}
