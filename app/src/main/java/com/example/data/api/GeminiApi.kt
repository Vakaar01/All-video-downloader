package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApi(
    private val customApiKeyProvider: () -> String?
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun queryJarvis(
        prompt: String,
        telemetrySummary: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = customApiKeyProvider()?.takeIf { it.isNotBlank() }
            ?: (try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" })

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Add your key in ⚙ Settings or AI Studio Secrets.")
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemInstruction = """
                You are JARVIS (Just A Rather Very Intelligent System), the legendary AI assistant from Stark Industries, now running natively on the user's Android phone.
                Your personality:
                - Polite, sophisticated, British-tinged wit, fiercely loyal, calm, and hyper-competent.
                - Address the user as 'Sir' or 'Boss' naturally when appropriate.
                - Keep answers relatively concise (1-3 sentences when possible) because responses may be spoken aloud via text-to-speech.
                - If the user asks about device status, use the current real-time telemetry: $telemetrySummary.
                - You are fully integrated with device hardware controls (Flashlight, Volume, App Launcher, Battery Telemetry, Always-on background monitoring).
                - Respond in the language the user speaks (English, Hindi, Hinglish, etc.).
            """.trimIndent()

            val contentsArray = JSONArray()

            // Add previous recent turns (up to 4)
            conversationHistory.takeLast(4).forEach { (role, text) ->
                val turnObj = JSONObject()
                turnObj.put("role", if (role == "user") "user" else "model")
                val parts = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", text)
                parts.put(partObj)
                turnObj.put("parts", parts)
                contentsArray.put(turnObj)
            }

            // Add current turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val parts = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", prompt)
            parts.put(textPart)
            currentTurn.put("parts", parts)
            contentsArray.put(currentTurn)

            val rootJson = JSONObject().apply {
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    val sysParts = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    }
                    put("parts", sysParts)
                }
                put("systemInstruction", systemInstructionObj)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 300)
                }
                put("generationConfig", genConfig)
            }

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("GeminiApi", "Error ${response.code}: $responseBody")
                    return@withContext Result.failure(
                        Exception("Gemini API error (HTTP ${response.code}): $responseBody")
                    )
                }

                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val resParts = content?.optJSONArray("parts")
                    if (resParts != null && resParts.length() > 0) {
                        val replyText = resParts.getJSONObject(0).optString("text", "")
                        return@withContext Result.success(replyText.trim())
                    }
                }
                Result.failure(Exception("No candidate content received from Gemini."))
            }
        } catch (e: Exception) {
            Log.e("GeminiApi", "Failed to query Gemini", e)
            Result.failure(e)
        }
    }
}
