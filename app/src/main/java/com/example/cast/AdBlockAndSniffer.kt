package com.example.cast

import android.net.Uri
import com.example.data.model.DetectedMedia
import com.example.data.model.M3uChannel
import java.util.Locale

object AdBlockAndSniffer {

    private val adHostKeywords = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google",
        "pagead2.googlesyndication.com",
        "ads.pubmatic.com",
        "adnxs.com",
        "taboola.com",
        "outbrain.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "exoclick.com",
        "juicyads.com",
        "trafficjunky.net",
        "mgid.com",
        "revcontent.com",
        "criteo.com",
        "amazon-adsystem.com",
        "moatads.com",
        "scorecardresearch.com",
        "rubiconproject.com",
        "openx.net",
        "adcolony.com",
        "unityads.unity3d.com",
        "applovin.com",
        "adsterra.com",
        "onclickalgo.com",
        "hilltopads.net",
        "monetag.com",
        "adskeeper.com"
    )

    private val adPathHints = listOf(
        "/pagead/",
        "/adserver/",
        "/popunder",
        "/banner_ad",
        "/ads.js",
        "/adsbygoogle"
    )

    fun shouldBlockUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        val host = runCatching { Uri.parse(lower).host.orEmpty() }.getOrDefault("")
        if (host.isNotEmpty()) {
            for (adHost in adHostKeywords) {
                if (host == adHost || host.endsWith(".$adHost")) {
                    return true
                }
            }
        }
        return adPathHints.any { lower.contains(it) }
    }

    fun detectMediaFromUrl(
        rawUrl: String,
        mimeHeader: String? = null,
        pageTitle: String = "",
        sourcePage: String = ""
    ): DetectedMedia? {
        val url = rawUrl.trim()
        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return null
        }

        val lower = url.lowercase(Locale.ROOT)
        val path = runCatching { Uri.parse(lower).path.orEmpty() }.getOrDefault(lower)
        val mime = mimeHeader?.lowercase(Locale.ROOT).orEmpty()

        // Ignore tiny HLS segments (.ts / .m4s) if they look like individual chunks rather than playlists
        if (path.endsWith(".m4s") || (path.endsWith(".ts") && lower.contains("seg"))) {
            return null
        }

        val (formatTag, resolvedMime) = when {
            path.endsWith(".m3u8") || lower.contains(".m3u8?") || mime.contains("mpegurl") ->
                "HLS" to "application/vnd.apple.mpegurl"
            path.endsWith(".mpd") || lower.contains(".mpd?") || mime.contains("dash+xml") ->
                "DASH" to "application/dash+xml"
            path.endsWith(".mp4") || lower.contains(".mp4?") || mime.contains("video/mp4") ->
                "MP4" to "video/mp4"
            path.endsWith(".webm") || lower.contains(".webm?") || mime.contains("video/webm") ->
                "WEBM" to "video/webm"
            path.endsWith(".mkv") || lower.contains(".mkv?") || mime.contains("x-matroska") ->
                "MKV" to "video/x-matroska"
            path.endsWith(".mov") || lower.contains(".mov?") || mime.contains("quicktime") ->
                "MOV" to "video/quicktime"
            path.endsWith(".m4v") || lower.contains(".m4v?") ->
                "M4V" to "video/mp4"
            path.endsWith(".mp3") || lower.contains(".mp3?") || mime.contains("audio/mpeg") ->
                "MP3" to "audio/mpeg"
            path.endsWith(".aac") || lower.contains(".aac?") || mime.contains("audio/aac") ->
                "AAC" to "audio/aac"
            mime.startsWith("video/") ->
                "VIDEO" to mime
            else -> return null
        }

        val filename = runCatching {
            Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() }
        }.getOrNull()

        val title = when {
            pageTitle.isNotBlank() -> pageTitle.take(64)
            filename != null -> filename.take(48)
            else -> "فيديو $formatTag"
        }

        return DetectedMedia(
            url = url,
            title = title,
            mimeType = resolvedMime,
            formatTag = formatTag,
            sourcePage = sourcePage
        )
    }

    fun parseM3uPlaylist(rawText: String): List<M3uChannel> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val results = mutableListOf<M3uChannel>()
        var pendingTitle = ""
        var pendingGroup = "IPTV"

        for (line in lines) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val commaIdx = line.lastIndexOf(',')
                    pendingTitle = if (commaIdx >= 0 && commaIdx + 1 < line.length) {
                        line.substring(commaIdx + 1).trim()
                    } else {
                        "قناة بث مباشر"
                    }
                    val groupMatch = Regex("""group-title="([^"]+)"""").find(line)
                    pendingGroup = groupMatch?.groupValues?.getOrNull(1)?.ifBlank { "IPTV" } ?: "IPTV"
                }
                line.startsWith("http://") || line.startsWith("https://") -> {
                    results += M3uChannel(
                        title = pendingTitle.ifBlank { "قناة ${results.size + 1}" },
                        url = line,
                        group = pendingGroup
                    )
                    pendingTitle = ""
                }
            }
        }
        return results
    }

    const val VIDEO_EXTRACTOR_JS: String = """
        (function() {
            var found = [];
            try {
                var videos = document.querySelectorAll('video');
                for (var i = 0; i < videos.length; i++) {
                    var v = videos[i];
                    if (v.currentSrc && v.currentSrc.indexOf('http') === 0) found.push(v.currentSrc);
                    if (v.src && v.src.indexOf('http') === 0) found.push(v.src);
                    var sources = v.querySelectorAll('source');
                    for (var j = 0; j < sources.length; j++) {
                        if (sources[j].src && sources[j].src.indexOf('http') === 0) found.push(sources[j].src);
                    }
                }
                var audios = document.querySelectorAll('audio');
                for (var k = 0; k < audios.length; k++) {
                    if (audios[k].currentSrc && audios[k].currentSrc.indexOf('http') === 0) found.push(audios[k].currentSrc);
                }
            } catch (e) {}
            return Array.from(new Set(found)).join('\n');
        })();
    """
}
