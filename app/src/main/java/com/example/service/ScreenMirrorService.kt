package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.MirrorConfig
import com.example.data.model.MirrorQualityPreset
import java.io.ByteArrayOutputStream

class ScreenMirrorService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private var lastFrameTimestampMs = 0L
    private var fpsCounterStartMs = 0L
    private var framesInWindow = 0
    private var currentMeasuredFps = 0

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopMirroringInternal(null)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMirroringInternal(null)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                val presetId = intent.getStringExtra(EXTRA_PRESET_ID)
                val fps = intent.getIntExtra(EXTRA_FPS, 30).coerceIn(15, 60)
                val quality = intent.getIntExtra(EXTRA_QUALITY, 74).coerceIn(45, 92)
                val config = MirrorConfig(
                    preset = MirrorQualityPreset.fromId(presetId),
                    targetFps = fps,
                    jpegQuality = quality
                )

                if (resultCode != Activity.RESULT_OK || resultData == null) {
                    ScreenMirrorStateHolder.setMirroringStopped("لم يتم منح إذن التقاط الشاشة")
                    stopSelf()
                    return START_NOT_STICKY
                }

                promoteToForeground(config)
                startProjection(resultCode, resultData, config)
            }
        }
        return START_NOT_STICKY
    }

    private fun promoteToForeground(config: MirrorConfig) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_mirror),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_mirror_content)
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ScreenMirrorService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_mirror_title))
            .setContentText("الجودة: ${config.preset.label} • ${config.targetFps} FPS")
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.mirror_stop), stopIntent)
            .build()

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
    }

    private fun startProjection(resultCode: Int, data: Intent, config: MirrorConfig) {
        stopMirroringInternal(null)

        try {
            val pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = pm.getMediaProjection(resultCode, data)
            if (projection == null) {
                ScreenMirrorStateHolder.setMirroringStopped("تعذر تهيئة خدمة بث الشاشة")
                stopSelf()
                return
            }
            mediaProjection = projection

            val thread = HandlerThread("dz_mirror_capture").apply { start() }
            captureThread = thread
            val handler = Handler(thread.looper)
            captureHandler = handler

            // Required on Android 14+ before createVirtualDisplay
            projection.registerCallback(projectionCallback, handler)

            val metrics = resources.displayMetrics
            val screenW = metrics.widthPixels.coerceAtLeast(360)
            val screenH = metrics.heightPixels.coerceAtLeast(360)
            val isPortrait = screenH >= screenW

            val targetLong = config.preset.maxLongSide
            val targetShort = config.preset.maxShortSide
            val aspect = screenW.toFloat() / screenH.toFloat()

            val captureW: Int
            val captureH: Int
            if (isPortrait) {
                captureH = minOf(screenH, targetLong)
                captureW = (captureH * aspect).toInt().coerceAtMost(targetShort)
            } else {
                captureW = minOf(screenW, targetLong)
                captureH = (captureW / aspect).toInt().coerceAtMost(targetShort)
            }

            // Ensure even dimensions for encoder/display compatibility
            val finalW = (captureW / 2) * 2
            val finalH = (captureH / 2) * 2
            val minFrameIntervalMs = (1000L / config.targetFps.coerceIn(15, 60)).coerceAtLeast(16L)

            val reader = ImageReader.newInstance(finalW, finalH, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            fpsCounterStartMs = System.currentTimeMillis()
            framesInWindow = 0

            val jpegBuffer = ByteArrayOutputStream(64 * 1024)

            reader.setOnImageAvailableListener({ r ->
                val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
                try {
                    val now = System.currentTimeMillis()
                    if (now - lastFrameTimestampMs < minFrameIntervalMs) {
                        return@setOnImageAvailableListener
                    }
                    lastFrameTimestampMs = now

                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * finalW

                    val paddedBitmap = Bitmap.createBitmap(
                        finalW + rowPadding / pixelStride,
                        finalH,
                        Bitmap.Config.ARGB_8888
                    )
                    paddedBitmap.copyPixelsFromBuffer(buffer)

                    val frameBitmap = if (rowPadding == 0) {
                        paddedBitmap
                    } else {
                        Bitmap.createBitmap(paddedBitmap, 0, 0, finalW, finalH).also {
                            paddedBitmap.recycle()
                        }
                    }

                    jpegBuffer.reset()
                    frameBitmap.compress(Bitmap.CompressFormat.JPEG, config.jpegQuality, jpegBuffer)
                    frameBitmap.recycle()

                    framesInWindow++
                    val elapsed = now - fpsCounterStartMs
                    if (elapsed >= 1000L) {
                        currentMeasuredFps = ((framesInWindow * 1000L) / elapsed).toInt()
                        framesInWindow = 0
                        fpsCounterStartMs = now
                    }

                    ScreenMirrorStateHolder.updateFrame(jpegBuffer.toByteArray(), currentMeasuredFps)
                } catch (_: Exception) {
                    // Ignore transient frame copy errors
                } finally {
                    image.close()
                }
            }, handler)

            virtualDisplay = projection.createVirtualDisplay(
                "DZCastMirror",
                finalW,
                finalH,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )

            ScreenMirrorStateHolder.setMirroringStarted(config)
        } catch (e: Exception) {
            ScreenMirrorStateHolder.setMirroringStopped(e.localizedMessage ?: "تعذر بدء بث الشاشة")
            stopMirroringInternal(e.localizedMessage)
            stopSelf()
        }
    }

    private fun stopMirroringInternal(error: String?) {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null

        runCatching { imageReader?.setOnImageAvailableListener(null, null) }
        runCatching { imageReader?.close() }
        imageReader = null

        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
        runCatching { mediaProjection?.stop() }
        mediaProjection = null

        runCatching { captureThread?.quitSafely() }
        captureThread = null
        captureHandler = null

        ScreenMirrorStateHolder.setMirroringStopped(error)
    }

    override fun onDestroy() {
        stopMirroringInternal(null)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "dz_cast_screen_mirror"
        private const val NOTIFICATION_ID = 4401

        const val ACTION_START = "com.example.action.START_MIRROR"
        const val ACTION_STOP = "com.example.action.STOP_MIRROR"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_PRESET_ID = "extra_preset_id"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_QUALITY = "extra_quality"

        fun createStartIntent(
            context: Context,
            resultCode: Int,
            data: Intent,
            config: MirrorConfig
        ): Intent = Intent(context, ScreenMirrorService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_RESULT_CODE, resultCode)
            putExtra(EXTRA_RESULT_DATA, data)
            putExtra(EXTRA_PRESET_ID, config.preset.id)
            putExtra(EXTRA_FPS, config.targetFps)
            putExtra(EXTRA_QUALITY, config.jpegQuality)
        }

        fun createStopIntent(context: Context): Intent =
            Intent(context, ScreenMirrorService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
