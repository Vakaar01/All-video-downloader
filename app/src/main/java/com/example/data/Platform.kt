package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.TextPrimary

enum class PlatformType(
    val displayName: String,
    val description: String,
    val color: Color,
    val defaultPrefix: String,
    val domainPatterns: List<String>
) {
    INSTAGRAM(
        displayName = "Instagram",
        description = "Reels, Stories & IGTV Streams",
        color = CyberMagenta,
        defaultPrefix = "https://www.instagram.com/reel/",
        domainPatterns = listOf("instagram.com", "instagr.am")
    ),
    YOUTUBE(
        displayName = "YouTube",
        description = "Shorts, 1080p, 4K & Audio",
        color = CyberRed,
        defaultPrefix = "https://www.youtube.com/watch?v=",
        domainPatterns = listOf("youtube.com", "youtu.be")
    ),
    FACEBOOK(
        displayName = "Facebook",
        description = "Watch, Public Reels & Clips",
        color = CyberCyan,
        defaultPrefix = "https://www.facebook.com/watch/?v=",
        domainPatterns = listOf("facebook.com", "fb.watch", "fb.com")
    ),
    TIKTOK(
        displayName = "TikTok",
        description = "Watermark-Free HD Streams",
        color = CyberCyan,
        defaultPrefix = "https://www.tiktok.com/@user/video/",
        domainPatterns = listOf("tiktok.com", "vm.tiktok.com")
    ),
    TWITTER_X(
        displayName = "Twitter / X",
        description = "Direct High-Bitrate Video",
        color = TextPrimary,
        defaultPrefix = "https://x.com/user/status/",
        domainPatterns = listOf("twitter.com", "x.com")
    ),
    REDDIT(
        displayName = "Reddit",
        description = "DASH Video & Audio Streams",
        color = CyberYellow,
        defaultPrefix = "https://www.reddit.com/r/videos/comments/",
        domainPatterns = listOf("reddit.com", "v.redd.it")
    ),
    PINTEREST(
        displayName = "Pinterest",
        description = "Video Pins & Media",
        color = CyberRed,
        defaultPrefix = "https://pinterest.com/pin/",
        domainPatterns = listOf("pinterest.com", "pin.it")
    ),
    TWITCH(
        displayName = "Twitch",
        description = "High-FPS Gaming Clips & VODs",
        color = CyberMagenta,
        defaultPrefix = "https://clips.twitch.tv/",
        domainPatterns = listOf("twitch.tv")
    ),
    THREADS(
        displayName = "Threads",
        description = "Meta Threads Video Media",
        color = TextPrimary,
        defaultPrefix = "https://www.threads.net/@user/post/",
        domainPatterns = listOf("threads.net")
    ),
    VIMEO(
        displayName = "Vimeo",
        description = "Pro Cinema 1080p / 4K Streams",
        color = CyberCyan,
        defaultPrefix = "https://vimeo.com/",
        domainPatterns = listOf("vimeo.com")
    ),
    OTHER(
        displayName = "Other Web Stream",
        description = "Direct MP4, M3U8 & HTTP Streams",
        color = CyberGreen,
        defaultPrefix = "https://",
        domainPatterns = emptyList()
    );

    companion object {
        fun detectFromUrl(url: String): PlatformType {
            val lower = url.lowercase()
            for (p in entries) {
                if (p.domainPatterns.any { lower.contains(it) }) {
                    return p
                }
            }
            return OTHER
        }
    }
}

enum class MediaFormat(
    val title: String,
    val subtitle: String,
    val extension: String,
    val mimeType: String,
    val isAudio: Boolean
) {
    ORIGINAL_MP4(
        title = "ORIGINAL VIDEO (MP4)",
        subtitle = "Highest available resolution & adaptive bitrate",
        extension = "mp4",
        mimeType = "video/mp4",
        isAudio = false
    ),
    HD_MP4(
        title = "HD 1080P VIDEO (MP4)",
        subtitle = "Crisp 1080p stream with stereo sound",
        extension = "mp4",
        mimeType = "video/mp4",
        isAudio = false
    ),
    AUDIO_MP3(
        title = "HQ AUDIO STREAM (MP3)",
        subtitle = "Extracted 320 kbps clear studio audio",
        extension = "mp3",
        mimeType = "audio/mpeg",
        isAudio = true
    )
}
