package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppTab
import com.example.service.ScreenMirrorService
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidGlassFloatingDock
import com.example.ui.components.MiniCastFloatingBar
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.CastHubScreen
import com.example.ui.screens.LibrarySettingsScreen
import com.example.ui.screens.ScreenCastScreen
import com.example.ui.theme.DzCastTheme
import com.example.viewmodel.DzCastViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DzCastViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        handleSharedUrlIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val mirrorLive by viewModel.mirrorLiveState.collectAsStateWithLifecycle()

            // Keep screen awake when mirroring & enabled in settings
            LaunchedEffect(mirrorLive.isMirroring, settings.mirrorConfig.keepScreenOn) {
                if (mirrorLive.isMirroring && settings.mirrorConfig.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DzCastTheme(
                    themeMode = settings.themeMode,
                    frostIntensity = settings.frostIntensity
                ) {
                    DzCastAppRoot(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedUrlIntent(intent)
    }

    private fun handleSharedUrlIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            val urlRegex = Regex("""https?://\S+""")
            val extractedUrl = urlRegex.find(sharedText)?.value
            if (!extractedUrl.isNullOrBlank()) {
                viewModel.openUrlInBrowser(extractedUrl)
            }
        }
    }
}

@Composable
private fun DzCastAppRoot(viewModel: DzCastViewModel) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val browserState by viewModel.browserState.collectAsStateWithLifecycle()
    val devicesState by viewModel.devicesState.collectAsStateWithLifecycle()
    val mirrorLive by viewModel.mirrorLiveState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val castHistory by viewModel.castHistory.collectAsStateWithLifecycle()
    val savedStreams by viewModel.savedStreams.collectAsStateWithLifecycle()
    val pendingBrowserUrl by viewModel.pendingBrowserUrl.collectAsStateWithLifecycle()

    // Handle hardware Back from non-home tabs cleanly
    BackHandler(enabled = selectedTab != AppTab.MIRROR) {
        viewModel.selectTab(AppTab.MIRROR)
    }

    // MediaProjection launcher for live phone screen casting
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = ScreenMirrorService.createStartIntent(
                context = context,
                resultCode = result.resultCode,
                data = result.data!!,
                config = settings.mirrorConfig
            )
            ContextCompat.startForegroundService(context, serviceIntent)
            viewModel.notifyMirrorStarted()
        }
    }

    val launchMediaProjectionConsent: () -> Unit = {
        val pm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(pm.createScreenCaptureIntent())
    }

    // Android 13+ notification permission launcher (used for MediaProjection foreground notification)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Proceed with MediaProjection consent regardless (notification is still posted by FGS)
        launchMediaProjectionConsent()
    }

    val startScreenMirrorFlow: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            launchMediaProjectionConsent()
        }
    }

    val stopScreenMirrorFlow: () -> Unit = {
        context.startService(ScreenMirrorService.createStopIntent(context))
        viewModel.notifyMirrorStopped()
    }

    // Zero-Permission Android Photo Picker for Local Videos
    val galleryVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.castLocalMediaUri(uri)
        }
    }

    // Storage Access Framework (SAF) picker for any video/audio file
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.castLocalMediaUri(uri)
        }
    }

    val hasMiniBar = devicesState.activeSession != null
    val bottomOverlayPadding = PaddingValues(bottom = if (hasMiniBar) 174.dp else 102.dp)

    LiquidGlassBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            when (selectedTab) {
                AppTab.BROWSER -> BrowserScreen(
                    browserState = browserState,
                    adBlockEnabled = settings.adBlockEnabled,
                    desktopUserAgent = settings.desktopUserAgent,
                    bookmarks = bookmarks,
                    pendingUrl = pendingBrowserUrl,
                    onConsumePendingUrl = viewModel::consumePendingBrowserUrl,
                    onNavigateToUrl = viewModel::openUrlInBrowser,
                    onShowHome = viewModel::showBrowserHome,
                    onNavigationChanged = viewModel::updateBrowserNavigation,
                    onProgressChanged = viewModel::updateBrowserProgress,
                    onAdBlocked = viewModel::recordBlockedAd,
                    onMediaDetected = viewModel::onMediaDetected,
                    onClearDetectedMedia = viewModel::clearDetectedMedia,
                    onCastMedia = viewModel::castDetectedMedia,
                    onSaveStream = { media ->
                        viewModel.saveStreamToLibrary(
                            title = media.title,
                            url = media.url,
                            category = "متصفح",
                            formatTag = media.formatTag
                        )
                    },
                    onToggleBookmark = viewModel::toggleBookmarkForCurrentPage,
                    onToggleAdBlock = viewModel::setAdBlockEnabled,
                    onToggleDesktopUa = viewModel::setDesktopUserAgent,
                    onJumpToMirror = { viewModel.selectTab(AppTab.MIRROR) },
                    onJumpToLocalMedia = { viewModel.selectTab(AppTab.DEVICES) },
                    contentBottomPadding = bottomOverlayPadding
                )

                AppTab.MIRROR -> ScreenCastScreen(
                    mirrorConfig = settings.mirrorConfig,
                    liveState = mirrorLive,
                    receiverUrl = devicesState.receiverUrl,
                    isWifiConnected = devicesState.isWifiConnected,
                    selectedDeviceName = devicesState.selectedDevice?.name ?: "مستقبل الويب",
                    isDlnaSelected = devicesState.selectedDevice?.isWebReceiver == false,
                    onSelectPreset = viewModel::setMirrorPreset,
                    onSelectFps = viewModel::setMirrorFps,
                    onChangeJpegQuality = viewModel::setMirrorJpegQuality,
                    onToggleKeepScreenOn = viewModel::setKeepScreenOn,
                    onStartMirrorRequest = startScreenMirrorFlow,
                    onStopMirrorRequest = stopScreenMirrorFlow,
                    onPushToDlnaTv = viewModel::pushLiveMirrorToSelectedDlnaTv,
                    onOpenDevicesTab = { viewModel.selectTab(AppTab.DEVICES) },
                    contentBottomPadding = bottomOverlayPadding
                )

                AppTab.DEVICES -> CastHubScreen(
                    devicesState = devicesState,
                    onScanDevices = viewModel::scanForCastDevices,
                    onSelectDevice = viewModel::selectCastDevice,
                    onPickGalleryVideo = {
                        galleryVideoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onPickLocalFile = {
                        openDocumentLauncher.launch(arrayOf("video/*", "audio/*"))
                    },
                    onCastDirectUrl = viewModel::castDirectUrl,
                    onSaveStream = { title, url ->
                        viewModel.saveStreamToLibrary(title = title, url = url, category = "مباشر")
                    },
                    onImportM3u = viewModel::importM3uText,
                    onTogglePlayPause = viewModel::togglePlayPauseCast,
                    onStopCast = {
                        if (devicesState.activeSession?.isMirrorMode == true) {
                            stopScreenMirrorFlow()
                        } else {
                            viewModel.stopActiveCast()
                        }
                    },
                    onVolumeChange = viewModel::updateCastVolume,
                    contentBottomPadding = bottomOverlayPadding
                )

                AppTab.LIBRARY -> LibrarySettingsScreen(
                    settings = settings,
                    bookmarks = bookmarks,
                    history = castHistory,
                    savedStreams = savedStreams,
                    onOpenBookmark = viewModel::openUrlInBrowser,
                    onDeleteBookmark = viewModel::deleteBookmark,
                    onCastUrl = viewModel::castDirectUrl,
                    onDeleteHistory = viewModel::deleteHistoryItem,
                    onClearHistory = viewModel::clearCastHistory,
                    onDeleteSavedStream = viewModel::deleteSavedStream,
                    onSelectThemeMode = viewModel::setThemeMode,
                    onChangeFrostIntensity = viewModel::setFrostIntensity,
                    onToggleAdBlock = viewModel::setAdBlockEnabled,
                    onToggleDesktopUa = viewModel::setDesktopUserAgent,
                    contentBottomPadding = bottomOverlayPadding
                )
            }

            // Floating Mini Cast Controller + iOS 27 Liquid Glass Dock
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = devicesState.activeSession != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    devicesState.activeSession?.let { session ->
                        Column {
                            MiniCastFloatingBar(
                                session = session,
                                onTogglePlayPause = viewModel::togglePlayPauseCast,
                                onStop = {
                                    if (session.isMirrorMode) stopScreenMirrorFlow()
                                    else viewModel.stopActiveCast()
                                },
                                onOpenControls = {
                                    viewModel.selectTab(if (session.isMirrorMode) AppTab.MIRROR else AppTab.DEVICES)
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                LiquidGlassFloatingDock(
                    selectedTab = selectedTab,
                    detectedVideoCount = browserState.detectedMedia.size,
                    isMirrorLive = mirrorLive.isMirroring,
                    onSelectTab = viewModel::selectTab
                )
            }
        }
    }
}
