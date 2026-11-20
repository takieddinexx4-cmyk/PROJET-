package com.example.data.model

import android.net.Uri

enum class AppTab(val route: String) {
    BROWSER("browser"),
    MIRROR("mirror"),
    DEVICES("devices"),
    LIBRARY("library")
}

enum class MirrorQualityPreset(
    val id: String,
    val label: String,
    val subtitleAr: String,
    val maxLongSide: Int,
    val maxShortSide: Int,
    val defaultQuality: Int,
    val baseMbps: Float
) {
    SD_480P(
        id = "480p",
        label = "480p SD",
        subtitleAr = "استجابة فائقة • واي فاي ضعيف",
        maxLongSide = 854,
        maxShortSide = 480,
        defaultQuality = 62,
        baseMbps = 2.4f
    ),
    HD_720P(
        id = "720p",
        label = "720p HD",
        subtitleAr = "متوازن • مثالي لمعظم الشاشات",
        maxLongSide = 1280,
        maxShortSide = 720,
        defaultQuality = 74,
        baseMbps = 5.2f
    ),
    FHD_1080P(
        id = "1080p",
        label = "1080p FHD",
        subtitleAr = "نقاء عالٍ • للشاشات الكبيرة",
        maxLongSide = 1920,
        maxShortSide = 1080,
        defaultQuality = 82,
        baseMbps = 9.8f
    );

    companion object {
        fun fromId(id: String?): MirrorQualityPreset =
            entries.firstOrNull { it.id == id } ?: HD_720P
    }
}

data class MirrorConfig(
    val preset: MirrorQualityPreset = MirrorQualityPreset.HD_720P,
    val targetFps: Int = 30,
    val jpegQuality: Int = 74,
    val keepScreenOn: Boolean = true
) {
    fun estimatedMbps(): Float {
        val fpsScale = targetFps / 30f
        val qualityScale = (jpegQuality / 74f).coerceIn(0.55f, 1.35f)
        return (preset.baseMbps * fpsScale * qualityScale).coerceIn(0.8f, 18.0f)
    }
}

data class DetectedMedia(
    val url: String,
    val title: String,
    val mimeType: String = "video/mp4",
    val formatTag: String = "MP4",
    val sourcePage: String = "",
    val detectedAt: Long = System.currentTimeMillis(),
    val isLocal: Boolean = false,
    val localUri: Uri? = null
)

data class CastDevice(
    val id: String,
    val name: String,
    val manufacturer: String,
    val modelName: String,
    val ipAddress: String,
    val avTransportControlUrl: String = "",
    val renderingControlUrl: String = "",
    val locationUrl: String = "",
    val isWebReceiver: Boolean = false
)

enum class CastPlaybackState {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

data class ActiveCastSession(
    val mediaTitle: String,
    val mediaUrl: String,
    val formatTag: String,
    val targetDevice: CastDevice,
    val isMirrorMode: Boolean = false,
    val playbackState: CastPlaybackState = CastPlaybackState.PLAYING,
    val volume: Int = 65
)

data class M3uChannel(
    val title: String,
    val url: String,
    val group: String = "عام"
)
