package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cast.AdBlockAndSniffer
import com.example.data.local.BookmarkEntity
import com.example.data.model.DetectedMedia
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.StatusLiveEmerald
import com.example.viewmodel.BrowserUiState
import java.io.ByteArrayInputStream

private data class QuickPortal(val name: String, val url: String, val tag: String)

private val quickPortals = listOf(
    QuickPortal("البحث في الويب", "https://duckduckgo.com", "بحث"),
    QuickPortal("أرشيف الفيديو", "https://archive.org/details/movies", "أفلام"),
    QuickPortal("Vimeo", "https://vimeo.com/watch", "فيديو"),
    QuickPortal("Dailymotion", "https://www.dailymotion.com", "بث")
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    browserState: BrowserUiState,
    adBlockEnabled: Boolean,
    desktopUserAgent: Boolean,
    bookmarks: List<BookmarkEntity>,
    pendingUrl: String?,
    onConsumePendingUrl: () -> Unit,
    onNavigateToUrl: (String) -> Unit,
    onShowHome: () -> Unit,
    onNavigationChanged: (url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onAdBlocked: () -> Unit,
    onMediaDetected: (DetectedMedia) -> Unit,
    onClearDetectedMedia: () -> Unit,
    onCastMedia: (DetectedMedia) -> Unit,
    onSaveStream: (DetectedMedia) -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleDesktopUa: (Boolean) -> Unit,
    onJumpToMirror: () -> Unit,
    onJumpToLocalMedia: () -> Unit,
    contentBottomPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalGlassPalette.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var urlInput by remember(browserState.currentUrl) {
        mutableStateOf(browserState.currentUrl)
    }
    var showDetectedSheet by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }
    }

    LaunchedEffect(desktopUserAgent) {
        webView.settings.userAgentString = if (desktopUserAgent) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        } else {
            WebSettings.getDefaultUserAgent(context)
        }
    }

    LaunchedEffect(adBlockEnabled) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString().orEmpty()
                if (reqUrl.isBlank()) return super.shouldInterceptRequest(view, request)

                if (adBlockEnabled && AdBlockAndSniffer.shouldBlockUrl(reqUrl)) {
                    view?.post { onAdBlocked() }
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }

                val accept = request?.requestHeaders?.get("Accept")
                val detected = AdBlockAndSniffer.detectMediaFromUrl(
                    rawUrl = reqUrl,
                    mimeHeader = accept,
                    pageTitle = browserState.pageTitle,
                    sourcePage = browserState.currentUrl
                )
                if (detected != null) {
                    view?.post { onMediaDetected(detected) }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (!url.isNullOrBlank()) {
                    onNavigationChanged(
                        url,
                        view?.title.orEmpty(),
                        view?.canGoBack() == true,
                        view?.canGoForward() == true
                    )
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!url.isNullOrBlank()) {
                    onNavigationChanged(
                        url,
                        view?.title.orEmpty(),
                        view?.canGoBack() == true,
                        view?.canGoForward() == true
                    )
                    // Scan DOM video/audio tags automatically
                    view?.evaluateJavascript(AdBlockAndSniffer.VIDEO_EXTRACTOR_JS) { raw ->
                        val cleaned = raw?.trim('"')?.replace("\\n", "\n").orEmpty()
                        cleaned.lines().filter { it.startsWith("http") }.forEach { foundUrl ->
                            val media = AdBlockAndSniffer.detectMediaFromUrl(
                                rawUrl = foundUrl,
                                pageTitle = view.title.orEmpty(),
                                sourcePage = url
                            ) ?: DetectedMedia(
                                url = foundUrl,
                                title = view.title?.take(60).orEmpty().ifBlank { "فيديو صفحة الويب" },
                                formatTag = "VIDEO",
                                sourcePage = url
                            )
                            onMediaDetected(media)
                        }
                    }
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged(newProgress)
            }
        }
    }

    LaunchedEffect(pendingUrl) {
        if (!pendingUrl.isNullOrBlank()) {
            webView.loadUrl(pendingUrl)
            onConsumePendingUrl()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    BackHandler(enabled = !browserState.showHomeSpeedDial) {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            onShowHome()
        }
    }

    val isBookmarked = bookmarks.any { it.url == browserState.currentUrl && it.url.isNotBlank() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = contentBottomPadding.calculateBottomPadding())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Liquid Glass Browser Bar
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onShowHome,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("browser_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = "الصفحة الرئيسية",
                                tint = GlassTextPrimary
                            )
                        }

                        // Address Input Pill
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.36f))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = GlassTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                cursorBrush = SolidColor(palette.primaryAccent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        keyboardController?.hide()
                                        if (urlInput.isNotBlank()) {
                                            onNavigateToUrl(urlInput)
                                        }
                                    }
                                ),
                                decorationBox = { inner ->
                                    if (urlInput.isEmpty()) {
                                        Text(
                                            text = "أدخل رابط موقع أو ابحث عن فيديو…",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GlassTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    inner()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("browser_url_input")
                            )
                            if (urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { urlInput = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "مسح الرابط",
                                        tint = GlassTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (!browserState.showHomeSpeedDial) {
                            IconButton(
                                onClick = { webView.reload() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("browser_reload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "تحديث الصفحة",
                                    tint = GlassTextPrimary
                                )
                            }
                        }
                    }

                    // Compact Controls + Ad-Block Shield Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!browserState.showHomeSpeedDial) {
                                IconButton(
                                    onClick = { if (webView.canGoBack()) webView.goBack() },
                                    enabled = browserState.canGoBack,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = if (browserState.canGoBack) Color.White else GlassTextMuted,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { if (webView.canGoForward()) webView.goForward() },
                                    enabled = browserState.canGoForward,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = "تقدم",
                                        tint = if (browserState.canGoForward) Color.White else GlassTextMuted,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onToggleBookmark,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("browser_bookmark_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = "حفظ الموقع",
                                        tint = if (isBookmarked) palette.primaryAccent else GlassTextSecondary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // Ad-Blocker Pill
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (adBlockEnabled) StatusLiveEmerald.copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (adBlockEnabled) StatusLiveEmerald.copy(alpha = 0.42f)
                                        else Color.White.copy(alpha = 0.14f),
                                        CircleShape
                                    )
                                    .testTag("browser_adblock_pill")
                                    .clickable { onToggleAdBlock(!adBlockEnabled) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = "مانع الإعلانات",
                                    tint = if (adBlockEnabled) StatusLiveEmerald else GlassTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (adBlockEnabled) "حماية (${browserState.adsBlockedCount})" else "معطّل",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlassTextPrimary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Desktop Mode Pill
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (desktopUserAgent) palette.secondaryAccent.copy(alpha = 0.28f)
                                        else Color.White.copy(alpha = 0.07f)
                                    )
                                    .clickable { onToggleDesktopUa(!desktopUserAgent) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DesktopWindows,
                                    contentDescription = "وضع سطح المكتب",
                                    tint = if (desktopUserAgent) Color.White else GlassTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "كمبيوتر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (desktopUserAgent) Color.White else GlassTextSecondary
                                )
                            }

                            if (!browserState.showHomeSpeedDial) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(palette.primaryAccent.copy(alpha = 0.25f))
                                        .border(1.dp, palette.primaryAccent.copy(alpha = 0.55f), CircleShape)
                                        .testTag("browser_scan_videos_pill")
                                        .clickable {
                                            webView.evaluateJavascript(AdBlockAndSniffer.VIDEO_EXTRACTOR_JS) { raw ->
                                                val cleaned = raw?.trim('"')?.replace("\\n", "\n").orEmpty()
                                                cleaned.lines().filter { it.startsWith("http") }.forEach { u ->
                                                    val m = AdBlockAndSniffer.detectMediaFromUrl(
                                                        rawUrl = u,
                                                        pageTitle = browserState.pageTitle,
                                                        sourcePage = browserState.currentUrl
                                                    ) ?: DetectedMedia(url = u, title = browserState.pageTitle, formatTag = "VIDEO")
                                                    onMediaDetected(m)
                                                }
                                                showDetectedSheet = true
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Radar,
                                        contentDescription = "فحص الفيديوهات",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "التقاط (${browserState.detectedMedia.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (browserState.isLoading) {
                        LinearProgressIndicator(
                            progress = { browserState.loadingProgress / 100f },
                            color = palette.primaryAccent,
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // Content: Speed Dial Home OR Live WebView
            if (browserState.showHomeSpeedDial) {
                BrowserHomeSpeedDial(
                    bookmarks = bookmarks,
                    detectedCount = browserState.detectedMedia.size,
                    onOpenUrl = onNavigateToUrl,
                    onJumpToMirror = onJumpToMirror,
                    onJumpToLocalMedia = onJumpToLocalMedia,
                    onOpenDetectedSheet = { showDetectedSheet = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                ) {
                    AndroidView(
                        factory = { webView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Floating Detected Video Streams Pill
        if (browserState.detectedMedia.isNotEmpty() && !browserState.showHomeSpeedDial) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 14.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(listOf(palette.primaryAccent, palette.secondaryAccent))
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    .testTag("floating_detected_videos_fab")
                    .clickable { showDetectedSheet = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cast,
                    contentDescription = "عرض الفيديوهات المكتشفة",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${browserState.detectedMedia.size} فيديو جاهز للبث",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }

    // Detected Videos Liquid Glass Bottom Sheet
    if (showDetectedSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDetectedSheet = false },
            sheetState = sheetState,
            containerColor = ObsidianElevated,
            contentColor = GlassTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الفيديوهات المكتشفة (${browserState.detectedMedia.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = GlassTextPrimary
                    )
                    if (browserState.detectedMedia.isNotEmpty()) {
                        Text(
                            text = "مسح القائمة",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent,
                            modifier = Modifier
                                .clickable { onClearDetectedMedia() }
                                .padding(8.dp)
                        )
                    }
                }

                if (browserState.detectedMedia.isEmpty()) {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = null,
                                tint = GlassTextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "لم يتم التقاط روابط فيديو بعد",
                                style = MaterialTheme.typography.titleMedium,
                                color = GlassTextPrimary
                            )
                            Text(
                                text = "شغّل الفيديو داخل صفحة المتصفح لثانية واحدة وسيلتقطه DZ Cast تلقائياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(340.dp)
                    ) {
                        items(browserState.detectedMedia, key = { it.url }) { media ->
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LiquidGlassBadge(
                                            text = media.formatTag,
                                            accentColor = palette.secondaryAccent
                                        )
                                        Text(
                                            text = media.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = GlassTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Text(
                                        text = media.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlassTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LiquidGlassActionButton(
                                            text = "بث للتلفاز",
                                            icon = Icons.Rounded.Cast,
                                            onClick = {
                                                onCastMedia(media)
                                                showDetectedSheet = false
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onSaveStream(media) },
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.BookmarkBorder,
                                                contentDescription = "حفظ في المكتبة",
                                                tint = Color.White
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("Video URL", media.url))
                                            },
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = "نسخ الرابط",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserHomeSpeedDial(
    bookmarks: List<BookmarkEntity>,
    detectedCount: Int,
    onOpenUrl: (String) -> Unit,
    onJumpToMirror: () -> Unit,
    onJumpToLocalMedia: () -> Unit,
    onOpenDetectedSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                highlightAccent = true
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "متصفح البث الذكي الخالي من الإعلانات",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "تصفح أي موقع فيديو وسيقوم DZ Cast تلقائياً بحجب النوافذ المزعجة والتقاط روابط HLS و MP4 للبث الفوري إلى تلفازك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassActionButton(
                            text = "بث الشاشة",
                            icon = Icons.Rounded.ScreenShare,
                            onClick = onJumpToMirror,
                            modifier = Modifier.weight(1f)
                        )
                        LiquidGlassActionButton(
                            text = "فيديو من الهاتف",
                            icon = Icons.Rounded.VideoLibrary,
                            isPrimary = false,
                            onClick = onJumpToLocalMedia,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (detectedCount > 0) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenDetectedSheet
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "عرض الفيديوهات الملتقطة ($detectedCount)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        LiquidGlassBadge(text = "جاهز", accentColor = StatusLiveEmerald)
                    }
                }
            }
        }

        item {
            Text(
                text = "وصول سريع",
                style = MaterialTheme.typography.titleMedium,
                color = GlassTextPrimary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quickPortals.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { portal ->
                            LiquidGlassCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 20.dp,
                                contentPadding = PaddingValues(14.dp),
                                onClick = { onOpenUrl(portal.url) }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LiquidGlassBadge(
                                        text = portal.tag,
                                        accentColor = palette.secondaryAccent
                                    )
                                    Text(
                                        text = portal.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GlassTextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = portal.url.removePrefix("https://"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GlassTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bookmarks.isNotEmpty()) {
            item {
                Text(
                    text = "مواقعك المحفوظة",
                    style = MaterialTheme.typography.titleMedium,
                    color = GlassTextPrimary
                )
            }
            items(bookmarks.take(6), key = { it.id }) { bm ->
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp),
                    onClick = { onOpenUrl(bm.url) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = null,
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bm.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = GlassTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = bm.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
