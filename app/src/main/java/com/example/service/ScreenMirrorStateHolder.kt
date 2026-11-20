package com.example.service

import com.example.data.model.MirrorConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicReference

data class ScreenMirrorLiveState(
    val isMirroring: Boolean = false,
    val activeConfig: MirrorConfig = MirrorConfig(),
    val measuredFps: Int = 0,
    val lastFrameKb: Int = 0,
    val activeViewers: Int = 0,
    val errorMessage: String? = null
)

object ScreenMirrorStateHolder {
    private val _state = MutableStateFlow(ScreenMirrorLiveState())
    val state: StateFlow<ScreenMirrorLiveState> = _state.asStateFlow()

    private val latestFrameRef = AtomicReference<ByteArray?>(null)
    @Volatile
    private var frameSequence: Long = 0L

    fun getLatestFrame(): Pair<Long, ByteArray?> = frameSequence to latestFrameRef.get()

    fun updateFrame(jpegBytes: ByteArray, measuredFps: Int) {
        latestFrameRef.set(jpegBytes)
        frameSequence++
        _state.update {
            it.copy(
                measuredFps = measuredFps,
                lastFrameKb = (jpegBytes.size / 1024).coerceAtLeast(1)
            )
        }
    }

    fun setMirroringStarted(config: MirrorConfig) {
        _state.update {
            it.copy(
                isMirroring = true,
                activeConfig = config,
                errorMessage = null
            )
        }
    }

    fun setMirroringStopped(error: String? = null) {
        latestFrameRef.set(null)
        _state.update {
            it.copy(
                isMirroring = false,
                measuredFps = 0,
                lastFrameKb = 0,
                errorMessage = error
            )
        }
    }

    fun setActiveViewers(count: Int) {
        _state.update { it.copy(activeViewers = count.coerceAtLeast(0)) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
