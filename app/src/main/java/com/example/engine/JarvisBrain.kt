package com.example.engine

import com.example.data.api.GeminiApi
import com.example.data.model.DeviceTelemetry

data class JarvisExecutionResult(
    val replyText: String,
    val actionTag: String? = null,
    val executedSuccessfully: Boolean = true
)

class JarvisBrain(
    private val systemController: SystemController,
    private val geminiApi: GeminiApi
) {

    suspend fun processCommand(
        input: String,
        telemetry: DeviceTelemetry,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): JarvisExecutionResult {
        val query = input.trim().lowercase()

        // 1. Hardware: Flashlight / Torch
        if (query.contains("flashlight") || query.contains("torch") || query.contains("light")) {
            if (query.contains("on") || query.contains("enable") || query.contains("activate") || query.contains("chalu")) {
                val ok = systemController.setTorch(true)
                return if (ok) {
                    JarvisExecutionResult(
                        replyText = "Illuminating your perimeter, sir. Flashlight activated.",
                        actionTag = "⚡ FLASH_ON"
                    )
                } else {
                    JarvisExecutionResult(
                        replyText = "I encountered an error accessing the camera flash module, sir.",
                        actionTag = "⚠️ FLASH_ERROR",
                        executedSuccessfully = false
                    )
                }
            } else if (query.contains("off") || query.contains("disable") || query.contains("deactivate") || query.contains("band")) {
                val ok = systemController.setTorch(false)
                return if (ok) {
                    JarvisExecutionResult(
                        replyText = "Flashlight deactivated, sir.",
                        actionTag = "⚡ FLASH_OFF"
                    )
                } else {
                    JarvisExecutionResult(
                        replyText = "Unable to turn off flash module, sir.",
                        actionTag = "⚠️ FLASH_ERROR",
                        executedSuccessfully = false
                    )
                }
            } else if (query.contains("toggle")) {
                val newState = systemController.toggleTorch()
                return JarvisExecutionResult(
                    replyText = if (newState) "Flashlight turned on, sir." else "Flashlight turned off, sir.",
                    actionTag = "⚡ FLASH_TOGGLE"
                )
            }
        }

        // 2. Hardware: Volume Control
        if (query.contains("volume") || query.contains("sound") || query.contains("awaz")) {
            if (query.contains("up") || query.contains("raise") || query.contains("increase") || query.contains("badhao")) {
                val vol = systemController.adjustVolume(true)
                return JarvisExecutionResult(
                    replyText = "Raising audio output. Current volume is at $vol%, sir.",
                    actionTag = "🔊 VOL_UP"
                )
            } else if (query.contains("down") || query.contains("lower") || query.contains("decrease") || query.contains("kam")) {
                val vol = systemController.adjustVolume(false)
                return JarvisExecutionResult(
                    replyText = "Lowering audio levels. Current volume is at $vol%, sir.",
                    actionTag = "🔉 VOL_DOWN"
                )
            } else if (query.contains("mute") || query.contains("silent") || query.contains("zero")) {
                val vol = systemController.setVolume(0)
                return JarvisExecutionResult(
                    replyText = "Audio output muted, sir.",
                    actionTag = "🔇 VOL_MUTE"
                )
            } else if (query.contains("max") || query.contains("full") || query.contains("100")) {
                val vol = systemController.setVolume(100)
                return JarvisExecutionResult(
                    replyText = "Volume boosted to maximum 100%, sir.",
                    actionTag = "🔊 VOL_MAX"
                )
            }
            // Parse specific percentage (e.g., "volume 80")
            val regex = Regex("""\b(\d{1,3})\s*%?""")
            val match = regex.find(query)
            if (match != null) {
                val pct = match.groupValues[1].toIntOrNull()
                if (pct != null && pct in 0..100) {
                    val actual = systemController.setVolume(pct)
                    return JarvisExecutionResult(
                        replyText = "Adjusting master volume to $actual%, sir.",
                        actionTag = "🔊 VOL_SET"
                    )
                }
            }
        }

        // 3. System Telemetry & Diagnostics
        if (query.contains("battery") || query.contains("power") || query.contains("charging")) {
            val chargingText = if (telemetry.isCharging) "plugged in and charging" else "running on internal cell"
            return JarvisExecutionResult(
                replyText = "Power levels are at ${telemetry.batteryPercent}%, sir. The unit is currently $chargingText.",
                actionTag = "🔋 BATTERY_REPORT"
            )
        }

        if (query.contains("ram") || query.contains("memory") || query.contains("storage") || query.contains("specs")) {
            return JarvisExecutionResult(
                replyText = "Memory diagnostic: ${telemetry.freeRamMb}MB RAM free out of ${telemetry.totalRamMb}MB (${telemetry.ramUsedPercent}% utilized). Internal storage has ${telemetry.freeStorageGb}GB available.",
                actionTag = "📊 SYSTEM_TELEMETRY"
            )
        }

        if (query.contains("diagnostics") || query.contains("system status") || query.contains("system report")) {
            val status = "Diagnostics complete, sir. Battery: ${telemetry.batteryPercent}%, RAM: ${telemetry.freeRamMb}MB free, Network: ${telemetry.networkStatus}, Storage: ${telemetry.freeStorageGb}GB free. All core protocols nominal."
            return JarvisExecutionResult(
                replyText = status,
                actionTag = "🛡️ FULL_DIAGNOSTIC"
            )
        }

        // 4. App Launching ("open youtube", "launch whatsapp", etc.)
        if (query.startsWith("open ") || query.startsWith("launch ") || query.startsWith("start ") || query.startsWith("kholo ")) {
            val target = query.removePrefix("open ")
                .removePrefix("launch ")
                .removePrefix("start ")
                .removePrefix("kholo ")
                .trim()

            if (target.isNotBlank()) {
                val success = systemController.launchAppByName(target)
                return if (success) {
                    JarvisExecutionResult(
                        replyText = "Launching $target immediately, sir.",
                        actionTag = "🚀 APP_LAUNCH"
                    )
                } else {
                    JarvisExecutionResult(
                        replyText = "I couldn't locate an application named '$target' on this device, sir. Attempting web search instead.",
                        actionTag = "⚠️ APP_NOT_FOUND"
                    ).also {
                        systemController.openWebSearch(target)
                    }
                }
            }
        }

        // 5. Web Search ("search for...", "google...")
        if (query.startsWith("search ") || query.startsWith("google ") || query.startsWith("find ")) {
            val searchTarget = query.removePrefix("search for ")
                .removePrefix("search ")
                .removePrefix("google ")
                .removePrefix("find ")
                .trim()
            if (searchTarget.isNotBlank()) {
                systemController.openWebSearch(searchTarget)
                return JarvisExecutionResult(
                    replyText = "Initiating web lookup for '$searchTarget', sir.",
                    actionTag = "🌐 WEB_SEARCH"
                )
            }
        }

        // 6. Identity & Protocol Information
        if (query.contains("who are you") || query.contains("your name") || query.contains("what are you")) {
            return JarvisExecutionResult(
                replyText = "I am JARVIS — Just A Rather Very Intelligent System. Running Mark LIII protocol on your Android device. At your service, sir.",
                actionTag = "🤖 IDENTITY"
            )
        }

        // 7. Conversational Query / High Intelligence -> Gemini API
        val telemetrySummary = "Battery: ${telemetry.batteryPercent}%, Charging: ${telemetry.isCharging}, Free RAM: ${telemetry.freeRamMb}MB, Network: ${telemetry.networkStatus}"
        val geminiResult = geminiApi.queryJarvis(
            prompt = input,
            telemetrySummary = telemetrySummary,
            conversationHistory = conversationHistory
        )

        return if (geminiResult.isSuccess) {
            JarvisExecutionResult(
                replyText = geminiResult.getOrThrow(),
                actionTag = "✨ GEMINI_AI"
            )
        } else {
            val ex = geminiResult.exceptionOrNull()
            // Provide intelligent fallback answer
            val fallbackResponse = generateLocalFallback(input, telemetry)
            JarvisExecutionResult(
                replyText = "$fallbackResponse\n(Note: ${ex?.message ?: "AI offline mode"})",
                actionTag = "💡 LOCAL_HEURISTIC"
            )
        }
    }

    private fun generateLocalFallback(input: String, telemetry: DeviceTelemetry): String {
        val q = input.lowercase()
        return when {
            q.contains("hello") || q.contains("hey") || q.contains("hi") || q.contains("namaste") ->
                "Greetings, sir. How may I be of assistance today?"
            q.contains("time") ->
                "The current system time is ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}, sir."
            q.contains("date") || q.contains("day") ->
                "Today is ${java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}, sir."
            q.contains("help") ->
                "I can toggle your flashlight, adjust volume, launch any app, check real-time battery & RAM telemetry, search the web, and engage via Gemini AI."
            else ->
                "Understood, sir. Processing your request. You can also connect your Gemini API Key in Settings for deep conversational reasoning."
        }
    }
}
