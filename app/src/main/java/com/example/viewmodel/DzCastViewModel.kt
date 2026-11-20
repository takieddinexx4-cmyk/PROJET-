package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cast.AdBlockAndSniffer
import com.example.cast.DlnaCastController
import com.example.cast.LocalCastServer
import com.example.cast.NetworkUtils
import com.example.cast.SsdpDiscoveryManager
import com.example.data.local.BookmarkEntity
import com.example.data.local.CastHistoryEntity
import com.example.data.local.DzCastDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.SavedStreamEntity
import com.example.data.local.UserSettings
import com.example.data.model.ActiveCastSession
import com.example.data.model.AppTab
import com.example.data.model.CastDevice
import com.example.data.model.CastPlaybackState
import com.example.data.model.DetectedMedia
import com.example.data.model.MirrorQualityPreset
import com.example.data.repository.CastLibraryRepository
import com.example.service.ScreenMirrorLiveState
import com.example.service.ScreenMirrorStateHolder
import com.example.ui.theme.GlassThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowserUiState(
    val currentUrl: String = "",
    val pageTitle: String = "الصفحة الرئيسية",
    val isLoading: Boolean = false,
    val loadingProgress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val adsBlockedCount: Int = 0,
    val detectedMedia: List<DetectedMedia> = emptyList(),
    val showHomeSpeedDial: Boolean = true
)

data class DevicesUiState(
    val isScanning: Boolean = false,
    val isWifiConnected: Boolean = true,
    val localIp: String = "127.0.0.1",
    val receiverUrl: String = "http://127.0.0.1:8899",
    val devices: List<CastDevice> = emptyList(),
    val selectedDevice: CastDevice? = null,
    val activeSession: ActiveCastSession? = null,
    val statusBanner: String? = null
)

class DzCastViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val preferencesRepo = PreferencesRepository(appContext)
    private val database = DzCastDatabase.getInstance(appContext)
    private val libraryRepo = CastLibraryRepository(database.dao())
    private val localServer = LocalCastServer(appContext)
    private val ssdpDiscovery = SsdpDiscoveryManager(appContext)
    private val dlnaController = DlnaCastController()

    private val _selectedTab = MutableStateFlow(AppTab.MIRROR)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    val settings: StateFlow<UserSettings> = preferencesRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    val mirrorLiveState: StateFlow<ScreenMirrorLiveState> = ScreenMirrorStateHolder.state

    val bookmarks: StateFlow<List<BookmarkEntity>> = libraryRepo.bookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val castHistory: StateFlow<List<CastHistoryEntity>> = libraryRepo.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedStreams: StateFlow<List<SavedStreamEntity>> = libraryRepo.savedStreams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _browserState = MutableStateFlow(BrowserUiState())
    val browserState: StateFlow<BrowserUiState> = _browserState.asStateFlow()

    private val _devicesState = MutableStateFlow(DevicesUiState())
    val devicesState: StateFlow<DevicesUiState> = _devicesState.asStateFlow()

    private val _pendingBrowserUrl = MutableStateFlow<String?>(null)
    val pendingBrowserUrl: StateFlow<String?> = _pendingBrowserUrl.asStateFlow()

    init {
        startLocalServerAndNetwork()
        scanForCastDevices()
    }

    private fun startLocalServerAndNetwork() {
        viewModelScope.launch(Dispatchers.IO) {
            val port = localServer.start(settings.value.serverPort)
            val ip = NetworkUtils.getLocalIpAddress(appContext)
            val baseUrl = "http://$ip:$port"
            val wifiConnected = NetworkUtils.isWifiConnected(appContext)
            val webReceiver = buildWebReceiverDevice(ip, port)

            _devicesState.update { current ->
                val updatedList = listOf(webReceiver) + current.devices.filterNot { it.isWebReceiver }
                current.copy(
                    isWifiConnected = wifiConnected,
                    localIp = ip,
                    receiverUrl = baseUrl,
                    devices = updatedList,
                    selectedDevice = current.selectedDevice ?: webReceiver
                )
            }
        }
    }

    private fun buildWebReceiverDevice(ip: String, port: Int): CastDevice = CastDevice(
        id = "dz_web_receiver",
        name = "مستقبل DZ Cast (أي تلفاز أو متصفح)",
        manufacturer = "DZ Cast Local Server",
        modelName = "Web Receiver • $ip:$port",
        ipAddress = ip,
        isWebReceiver = true
    )

    fun refreshNetworkInfo() {
        val ip = NetworkUtils.getLocalIpAddress(appContext)
        val port = localServer.activePort
        val baseUrl = "http://$ip:$port"
        val wifiConnected = NetworkUtils.isWifiConnected(appContext)
        val webReceiver = buildWebReceiverDevice(ip, port)

        _devicesState.update { current ->
            val updatedDevices = listOf(webReceiver) + current.devices.filterNot { it.isWebReceiver }
            val selected = if (current.selectedDevice?.isWebReceiver != false) webReceiver else current.selectedDevice
            current.copy(
                isWifiConnected = wifiConnected,
                localIp = ip,
                receiverUrl = baseUrl,
                devices = updatedDevices,
                selectedDevice = selected
            )
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun consumePendingBrowserUrl() {
        _pendingBrowserUrl.value = null
    }

    fun openUrlInBrowser(rawUrl: String) {
        val formatted = normalizeInputUrl(rawUrl)
        _browserState.update { it.copy(showHomeSpeedDial = false, currentUrl = formatted) }
        _pendingBrowserUrl.value = formatted
        _selectedTab.value = AppTab.BROWSER
    }

    fun showBrowserHome() {
        _browserState.update {
            it.copy(
                showHomeSpeedDial = true,
                pageTitle = "الصفحة الرئيسية",
                isLoading = false
            )
        }
    }

    fun updateBrowserNavigation(
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        _browserState.update {
            it.copy(
                currentUrl = url,
                pageTitle = title.ifBlank { url },
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                showHomeSpeedDial = false
            )
        }
    }

    fun updateBrowserProgress(progress: Int) {
        _browserState.update {
            it.copy(
                loadingProgress = progress,
                isLoading = progress in 1..99
            )
        }
    }

    fun recordBlockedAd() {
        _browserState.update { it.copy(adsBlockedCount = it.adsBlockedCount + 1) }
    }

    fun onMediaDetected(media: DetectedMedia) {
        _browserState.update { current ->
            if (current.detectedMedia.any { it.url == media.url }) {
                current
            } else {
                current.copy(detectedMedia = (listOf(media) + current.detectedMedia).take(40))
            }
        }
    }

    fun clearDetectedMedia() {
        _browserState.update { it.copy(detectedMedia = emptyList()) }
    }

    fun scanForCastDevices() {
        viewModelScope.launch {
            refreshNetworkInfo()
            _devicesState.update { it.copy(isScanning = true, statusBanner = null) }
            val discovered = ssdpDiscovery.discoverDevices()
            val currentIp = NetworkUtils.getLocalIpAddress(appContext)
            val webReceiver = buildWebReceiverDevice(currentIp, localServer.activePort)
            val combined = listOf(webReceiver) + discovered
            _devicesState.update { current ->
                val selected = current.selectedDevice?.let { prev ->
                    combined.firstOrNull { it.id == prev.id }
                } ?: discovered.firstOrNull() ?: webReceiver
                current.copy(
                    isScanning = false,
                    devices = combined,
                    selectedDevice = selected,
                    statusBanner = if (discovered.isEmpty()) {
                        "افتح رابط المستقبل على متصفح التلفاز، أو تأكد من اتصال التلفاز بنفس الواي فاي"
                    } else {
                        "تم العثور على ${discovered.size} جهاز تلفاز ذكي على شبكتك"
                    }
                )
            }
        }
    }

    fun selectCastDevice(device: CastDevice) {
        _devicesState.update {
            it.copy(
                selectedDevice = device,
                statusBanner = "الجهاز المحدد للبث: ${device.name}"
            )
        }
    }

    fun castDetectedMedia(media: DetectedMedia) {
        val target = _devicesState.value.selectedDevice
            ?: buildWebReceiverDevice(NetworkUtils.getLocalIpAddress(appContext), localServer.activePort)

        viewModelScope.launch {
            localServer.setReceiverVideo(media.title, media.url, media.formatTag)

            if (target.isWebReceiver) {
                _devicesState.update {
                    it.copy(
                        activeSession = ActiveCastSession(
                            mediaTitle = media.title,
                            mediaUrl = media.url,
                            formatTag = media.formatTag,
                            targetDevice = target,
                            isMirrorMode = false,
                            playbackState = CastPlaybackState.PLAYING
                        ),
                        statusBanner = "جاهز على مستقبل الويب: ${localServer.getBaseUrl()}"
                    )
                }
            } else {
                _devicesState.update {
                    it.copy(
                        activeSession = ActiveCastSession(
                            mediaTitle = media.title,
                            mediaUrl = media.url,
                            formatTag = media.formatTag,
                            targetDevice = target,
                            isMirrorMode = false,
                            playbackState = CastPlaybackState.LOADING
                        )
                    )
                }
                val result = dlnaController.castMedia(target, media.url, media.title, media.mimeType)
                _devicesState.update { state ->
                    if (result.isSuccess) {
                        state.copy(
                            activeSession = state.activeSession?.copy(playbackState = CastPlaybackState.PLAYING),
                            statusBanner = "تم إرسال الفيديو بنجاح إلى ${target.name}"
                        )
                    } else {
                        state.copy(
                            activeSession = state.activeSession?.copy(playbackState = CastPlaybackState.ERROR),
                            statusBanner = "تعذر البث عبر DLNA (${result.exceptionOrNull()?.localizedMessage ?: "خطأ اتصال"}) — الرابط جاهز أيضاً على مستقبل الويب"
                        )
                    }
                }
            }

            libraryRepo.recordCastHistory(
                title = media.title,
                mediaUrl = media.url,
                formatTag = media.formatTag,
                targetDeviceName = target.name
            )
        }
    }

    fun castLocalMediaUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val served = localServer.registerLocalMedia(uri)
            val lanUrl = localServer.getMediaUrl(served.id)
            val format = served.mimeType.substringAfter("/").uppercase().take(6).ifBlank { "LOCAL" }
            val media = DetectedMedia(
                url = lanUrl,
                title = served.title,
                mimeType = served.mimeType,
                formatTag = format,
                isLocal = true,
                localUri = uri
            )
            castDetectedMedia(media)
        }
    }

    fun castDirectUrl(rawUrl: String, customTitle: String = "") {
        val cleaned = rawUrl.trim()
        if (cleaned.isBlank()) return
        val detected = AdBlockAndSniffer.detectMediaFromUrl(cleaned, pageTitle = customTitle)
            ?: DetectedMedia(
                url = cleaned,
                title = customTitle.ifBlank { "بث مباشر" },
                mimeType = if (cleaned.contains(".m3u8", true)) "application/vnd.apple.mpegurl" else "video/mp4",
                formatTag = if (cleaned.contains(".m3u8", true)) "HLS" else "STREAM"
            )
        castDetectedMedia(detected)
    }

    fun notifyMirrorStarted() {
        val cfg = settings.value.mirrorConfig
        localServer.setReceiverMirrorMode(cfg.preset.label)
        refreshNetworkInfo()
        val target = _devicesState.value.selectedDevice
            ?: buildWebReceiverDevice(NetworkUtils.getLocalIpAddress(appContext), localServer.activePort)

        _devicesState.update {
            it.copy(
                activeSession = ActiveCastSession(
                    mediaTitle = "بث شاشة الهاتف (${cfg.preset.label} • ${cfg.targetFps} FPS)",
                    mediaUrl = localServer.getMirrorStreamUrl(),
                    formatTag = cfg.preset.label,
                    targetDevice = target,
                    isMirrorMode = true,
                    playbackState = CastPlaybackState.PLAYING
                ),
                statusBanner = "بث الشاشة نشط الآن على ${localServer.getBaseUrl()}"
            )
        }
    }

    fun pushLiveMirrorToSelectedDlnaTv() {
        val target = _devicesState.value.selectedDevice ?: return
        if (target.isWebReceiver) {
            _devicesState.update {
                it.copy(statusBanner = "افتح ${localServer.getBaseUrl()} من متصفح التلفاز لمشاهدة الشاشة فوراً")
            }
            return
        }
        viewModelScope.launch {
            val streamUrl = localServer.getMirrorStreamUrl()
            val result = dlnaController.castMedia(
                device = target,
                mediaUrl = streamUrl,
                title = "بث شاشة DZ Cast",
                mimeType = "video/x-motion-jpeg"
            )
            _devicesState.update {
                it.copy(
                    statusBanner = if (result.isSuccess) {
                        "تم إرسال بث الشاشة إلى ${target.name}"
                    } else {
                        "بعض أجهزة التلفاز لا تدعم MJPEG عبر DLNA؛ افتح ${localServer.getBaseUrl()} في متصفح التلفاز للبث السلس"
                    }
                )
            }
        }
    }

    fun notifyMirrorStopped() {
        localServer.setReceiverIdle()
        _devicesState.update { state ->
            if (state.activeSession?.isMirrorMode == true) {
                state.copy(activeSession = null, statusBanner = "تم إيقاف بث الشاشة")
            } else {
                state
            }
        }
    }

    fun togglePlayPauseCast() {
        val session = _devicesState.value.activeSession ?: return
        if (session.isMirrorMode || session.targetDevice.isWebReceiver) return
        viewModelScope.launch {
            if (session.playbackState == CastPlaybackState.PLAYING) {
                dlnaController.pause(session.targetDevice)
                _devicesState.update {
                    it.copy(activeSession = session.copy(playbackState = CastPlaybackState.PAUSED))
                }
            } else {
                dlnaController.play(session.targetDevice)
                _devicesState.update {
                    it.copy(activeSession = session.copy(playbackState = CastPlaybackState.PLAYING))
                }
            }
        }
    }

    fun stopActiveCast() {
        val session = _devicesState.value.activeSession ?: return
        viewModelScope.launch {
            if (!session.targetDevice.isWebReceiver) {
                dlnaController.stop(session.targetDevice)
            }
            localServer.setReceiverIdle()
            _devicesState.update { it.copy(activeSession = null, statusBanner = "تم إيقاف البث") }
        }
    }

    fun updateCastVolume(volumePercent: Int) {
        val session = _devicesState.value.activeSession ?: return
        val clamped = volumePercent.coerceIn(0, 100)
        _devicesState.update {
            it.copy(activeSession = session.copy(volume = clamped))
        }
        if (!session.targetDevice.isWebReceiver) {
            viewModelScope.launch {
                dlnaController.setVolume(session.targetDevice, clamped)
            }
        }
    }

    // Room Library Actions
    fun toggleBookmarkForCurrentPage() {
        val url = _browserState.value.currentUrl
        val title = _browserState.value.pageTitle
        if (url.isBlank()) return
        viewModelScope.launch {
            val existing = bookmarks.value.any { it.url == url }
            if (existing) {
                libraryRepo.removeBookmarkByUrl(url)
            } else {
                libraryRepo.saveBookmark(title, url)
            }
        }
    }

    fun addBookmark(title: String, url: String) {
        if (url.isBlank()) return
        viewModelScope.launch { libraryRepo.saveBookmark(title, normalizeInputUrl(url)) }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { libraryRepo.removeBookmark(id) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { libraryRepo.deleteHistory(id) }
    }

    fun clearCastHistory() {
        viewModelScope.launch { libraryRepo.clearHistory() }
    }

    fun saveStreamToLibrary(title: String, url: String, category: String = "IPTV", formatTag: String = "HLS") {
        if (url.isBlank()) return
        viewModelScope.launch {
            libraryRepo.saveStream(title, url.trim(), category, formatTag)
            _devicesState.update { it.copy(statusBanner = "تم حفظ الرابط في المكتبة") }
        }
    }

    fun importM3uText(rawText: String): Int {
        val channels = AdBlockAndSniffer.parseM3uPlaylist(rawText)
        if (channels.isNotEmpty()) {
            viewModelScope.launch {
                libraryRepo.saveStreams(
                    channels.map {
                        SavedStreamEntity(
                            title = it.title,
                            streamUrl = it.url,
                            category = it.group,
                            formatTag = "M3U"
                        )
                    }
                )
            }
            _devicesState.update { it.copy(statusBanner = "تم استيراد ${channels.size} قناة إلى المكتبة") }
        }
        return channels.size
    }

    fun deleteSavedStream(id: Long) {
        viewModelScope.launch { libraryRepo.deleteSavedStream(id) }
    }

    // Preferences Actions
    fun setThemeMode(mode: GlassThemeMode) {
        viewModelScope.launch { preferencesRepo.setThemeMode(mode) }
    }

    fun setFrostIntensity(value: Float) {
        viewModelScope.launch { preferencesRepo.setFrostIntensity(value) }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.setAdBlockEnabled(enabled) }
    }

    fun setDesktopUserAgent(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.setDesktopUserAgent(enabled) }
    }

    fun setMirrorPreset(preset: MirrorQualityPreset) {
        viewModelScope.launch { preferencesRepo.setMirrorPreset(preset) }
    }

    fun setMirrorFps(fps: Int) {
        viewModelScope.launch { preferencesRepo.setMirrorFps(fps) }
    }

    fun setMirrorJpegQuality(quality: Int) {
        viewModelScope.launch { preferencesRepo.setMirrorJpegQuality(quality) }
    }

    fun setKeepScreenOn(keepOn: Boolean) {
        viewModelScope.launch { preferencesRepo.setKeepScreenOn(keepOn) }
    }

    fun clearStatusBanner() {
        _devicesState.update { it.copy(statusBanner = null) }
    }

    private fun normalizeInputUrl(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://www.google.com/search?q=${Uri.encode(trimmed)}"
        }
    }

    override fun onCleared() {
        localServer.stop()
        super.onCleared()
    }
}
