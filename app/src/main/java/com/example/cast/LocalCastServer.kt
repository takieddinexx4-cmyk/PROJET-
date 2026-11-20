package com.example.cast

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.service.ScreenMirrorStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class LocalServedMedia(
    val id: String,
    val uri: Uri,
    val title: String,
    val mimeType: String,
    val sizeBytes: Long
)

data class WebReceiverPayload(
    val mode: String = "idle", // idle | video | mirror
    val mediaUrl: String = "",
    val title: String = "DZ Cast",
    val formatTag: String = ""
)

class LocalCastServer(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private val activeMirrorClients = AtomicInteger(0)

    private val servedMediaMap = ConcurrentHashMap<String, LocalServedMedia>()

    @Volatile
    private var currentReceiverPayload = WebReceiverPayload()

    @Volatile
    var activePort: Int = 8899
        private set

    fun start(preferredPort: Int = 8899): Int {
        if (serverSocket != null && serverSocket?.isClosed == false) {
            return activePort
        }
        val socket = openAvailableSocket(preferredPort)
        serverSocket = socket
        activePort = socket.localPort

        acceptJob = scope.launch {
            while (isActive && !socket.isClosed) {
                try {
                    val client = socket.accept()
                    launch { handleClient(client) }
                } catch (_: Exception) {
                    if (socket.isClosed) break
                }
            }
        }
        return activePort
    }

    private fun openAvailableSocket(preferredPort: Int): ServerSocket {
        for (port in preferredPort..(preferredPort + 15)) {
            try {
                return ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
            } catch (_: Exception) {
                // Try next port
            }
        }
        return ServerSocket(0)
    }

    fun stop() {
        acceptJob?.cancel()
        runCatching { serverSocket?.close() }
        serverSocket = null
        activeMirrorClients.set(0)
        ScreenMirrorStateHolder.setActiveViewers(0)
    }

    fun registerLocalMedia(uri: Uri): LocalServedMedia {
        var displayName = "ملف محلي"
        var sizeBytes = -1L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
            }
        }
        val mime = context.contentResolver.getType(uri) ?: "video/mp4"
        val id = UUID.randomUUID().toString().replace("-", "").take(12)
        val entry = LocalServedMedia(
            id = id,
            uri = uri,
            title = displayName,
            mimeType = mime,
            sizeBytes = sizeBytes
        )
        servedMediaMap[id] = entry
        return entry
    }

    fun setReceiverVideo(title: String, mediaUrl: String, formatTag: String) {
        currentReceiverPayload = WebReceiverPayload(
            mode = "video",
            mediaUrl = mediaUrl,
            title = title,
            formatTag = formatTag
        )
    }

    fun setReceiverMirrorMode(resolutionLabel: String) {
        currentReceiverPayload = WebReceiverPayload(
            mode = "mirror",
            mediaUrl = "/mirror.mjpeg",
            title = "بث شاشة الهاتف • $resolutionLabel",
            formatTag = "LIVE"
        )
    }

    fun setReceiverIdle() {
        currentReceiverPayload = WebReceiverPayload()
    }

    fun getBaseUrl(): String {
        val ip = NetworkUtils.getLocalIpAddress(context)
        return "http://$ip:$activePort"
    }

    fun getMediaUrl(mediaId: String): String = "${getBaseUrl()}/media/$mediaId"

    fun getMirrorStreamUrl(): String = "${getBaseUrl()}/mirror.mjpeg"

    private suspend fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 15_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val out = BufferedOutputStream(client.getOutputStream())

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val path = parts[1].substringBefore("?")

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) break
                val sep = line.indexOf(':')
                if (sep > 0) {
                    headers[line.substring(0, sep).trim().lowercase()] =
                        line.substring(sep + 1).trim()
                }
            }

            when {
                path == "/" || path == "/index.html" -> sendReceiverHtml(out)
                path == "/api/status" -> sendStatusJson(out)
                path == "/frame.jpg" -> sendSingleFrame(out)
                path == "/mirror.mjpeg" -> streamMjpeg(client, out)
                path.startsWith("/media/") -> {
                    val mediaId = path.removePrefix("/media/")
                    serveLocalMedia(mediaId, headers["range"], out)
                }
                else -> sendTextResponse(out, 404, "text/plain", "Not Found")
            }
        }
    }

    private fun sendStatusJson(out: OutputStream) {
        val mirrorLive = ScreenMirrorStateHolder.state.value.isMirroring
        val payload = if (mirrorLive) {
            val cfg = ScreenMirrorStateHolder.state.value.activeConfig
            WebReceiverPayload(
                mode = "mirror",
                mediaUrl = "/mirror.mjpeg",
                title = "بث الشاشة المباشر (${cfg.preset.label} • ${cfg.targetFps} FPS)",
                formatTag = cfg.preset.label
            )
        } else {
            currentReceiverPayload
        }

        val safeTitle = payload.title.replace("\\", "\\\\").replace("\"", "\\\"")
        val safeUrl = payload.mediaUrl.replace("\\", "\\\\").replace("\"", "\\\"")
        val safeTag = payload.formatTag.replace("\"", "")

        val json = """{"mode":"${payload.mode}","mediaUrl":"$safeUrl","title":"$safeTitle","formatTag":"$safeTag","fps":${ScreenMirrorStateHolder.state.value.measuredFps}}"""
        sendTextResponse(out, 200, "application/json; charset=utf-8", json)
    }

    private fun sendSingleFrame(out: OutputStream) {
        val (_, bytes) = ScreenMirrorStateHolder.getLatestFrame()
        if (bytes == null) {
            sendTextResponse(out, 503, "text/plain", "No live frame")
            return
        }
        val header = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: image/jpeg\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-cache, no-store\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(header.toByteArray())
        out.write(bytes)
        out.flush()
    }

    private suspend fun streamMjpeg(socket: Socket, out: OutputStream) {
        socket.soTimeout = 0
        val boundary = "dzcastframe"
        val header = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: multipart/x-mixed-replace; boundary=$boundary\r\n")
            append("Cache-Control: no-cache, no-store, must-revalidate\r\n")
            append("Pragma: no-cache\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: keep-alive\r\n\r\n")
        }
        out.write(header.toByteArray())
        out.flush()

        val viewers = activeMirrorClients.incrementAndGet()
        ScreenMirrorStateHolder.setActiveViewers(viewers)
        var lastSeq = -1L

        try {
            while (!socket.isClosed) {
                val mirrorState = ScreenMirrorStateHolder.state.value
                if (!mirrorState.isMirroring) {
                    delay(200)
                    continue
                }
                val (seq, frameBytes) = ScreenMirrorStateHolder.getLatestFrame()
                if (frameBytes != null && seq != lastSeq) {
                    lastSeq = seq
                    val partHeader = "--$boundary\r\nContent-Type: image/jpeg\r\nContent-Length: ${frameBytes.size}\r\n\r\n"
                    out.write(partHeader.toByteArray())
                    out.write(frameBytes)
                    out.write("\r\n".toByteArray())
                    out.flush()
                } else {
                    delay(12)
                }
            }
        } catch (_: Exception) {
            // Client disconnected
        } finally {
            val remaining = activeMirrorClients.decrementAndGet().coerceAtLeast(0)
            ScreenMirrorStateHolder.setActiveViewers(remaining)
        }
    }

    private fun serveLocalMedia(mediaId: String, rangeHeader: String?, out: OutputStream) {
        val item = servedMediaMap[mediaId]
        if (item == null) {
            sendTextResponse(out, 404, "text/plain", "Media not found")
            return
        }

        val afd = runCatching { context.contentResolver.openAssetFileDescriptor(item.uri, "r") }.getOrNull()
        val totalSize = when {
            item.sizeBytes > 0 -> item.sizeBytes
            afd != null && afd.length > 0 -> afd.length
            else -> -1L
        }
        afd?.close()

        val inputStream = context.contentResolver.openInputStream(item.uri)
        if (inputStream == null) {
            sendTextResponse(out, 500, "text/plain", "Unable to read media")
            return
        }

        inputStream.use { input ->
            var start = 0L
            var end = if (totalSize > 0) totalSize - 1 else -1L
            var isPartial = false

            if (!rangeHeader.isNullOrBlank() && rangeHeader.startsWith("bytes=") && totalSize > 0) {
                val rangeSpec = rangeHeader.removePrefix("bytes=").substringBefore(",")
                val bounds = rangeSpec.split("-")
                val parsedStart = bounds.getOrNull(0)?.toLongOrNull() ?: 0L
                val parsedEnd = bounds.getOrNull(1)?.toLongOrNull() ?: (totalSize - 1)
                start = parsedStart.coerceIn(0L, totalSize - 1)
                end = parsedEnd.coerceIn(start, totalSize - 1)
                isPartial = true
            }

            val contentLength = if (totalSize > 0) (end - start + 1) else -1L
            val statusLine = if (isPartial) "HTTP/1.1 206 Partial Content" else "HTTP/1.1 200 OK"
            val header = buildString {
                append("$statusLine\r\n")
                append("Content-Type: ${item.mimeType}\r\n")
                append("Accept-Ranges: bytes\r\n")
                append("Access-Control-Allow-Origin: *\r\n")
                if (contentLength > 0) {
                    append("Content-Length: $contentLength\r\n")
                }
                if (isPartial && totalSize > 0) {
                    append("Content-Range: bytes $start-$end/$totalSize\r\n")
                }
                append("Connection: close\r\n\r\n")
            }
            out.write(header.toByteArray())

            if (start > 0) {
                var toSkip = start
                while (toSkip > 0) {
                    val skipped = input.skip(toSkip)
                    if (skipped <= 0) break
                    toSkip -= skipped
                }
            }

            val buffer = ByteArray(64 * 1024)
            var remaining = contentLength
            while (true) {
                val maxRead = if (remaining > 0) minOf(buffer.size.toLong(), remaining).toInt() else buffer.size
                val read = input.read(buffer, 0, maxRead)
                if (read <= 0) break
                out.write(buffer, 0, read)
                if (remaining > 0) {
                    remaining -= read
                    if (remaining <= 0) break
                }
            }
            out.flush()
        }
    }

    private fun sendTextResponse(out: OutputStream, status: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val statusText = if (status == 200) "OK" else if (status == 404) "Not Found" else "Error"
        val header = buildString {
            append("HTTP/1.1 $status $statusText\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(header.toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun sendReceiverHtml(out: OutputStream) {
        val html = """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>DZ Cast — مستقبل البث</title>
              <style>
                :root { color-scheme: dark; }
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  background: radial-gradient(circle at 20% 20%, rgba(200,46,71,.24), transparent 45%),
                              radial-gradient(circle at 80% 80%, rgba(47,107,255,.24), transparent 48%),
                              #06070B;
                  color: #F4F6FB;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                  min-height: 100vh;
                  display: flex;
                  flex-direction: column;
                  align-items: center;
                  justify-content: center;
                  padding: 18px;
                }
                .glass {
                  width: 100%;
                  max-width: 1120px;
                  background: rgba(255,255,255,0.06);
                  border: 1px solid rgba(255,255,255,0.18);
                  backdrop-filter: blur(22px);
                  border-radius: 26px;
                  padding: 20px;
                  box-shadow: 0 24px 60px rgba(0,0,0,0.55);
                }
                .topbar {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  margin-bottom: 14px;
                  gap: 12px;
                }
                .badge {
                  background: linear-gradient(135deg, rgba(200,46,71,.35), rgba(47,107,255,.35));
                  border: 1px solid rgba(255,255,255,.22);
                  padding: 6px 14px;
                  border-radius: 999px;
                  font-size: 13px;
                  font-weight: 600;
                }
                .stage {
                  position: relative;
                  width: 100%;
                  aspect-ratio: 16 / 9;
                  background: #020306;
                  border-radius: 18px;
                  overflow: hidden;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  border: 1px solid rgba(255,255,255,0.1);
                }
                img#mirror, video#player {
                  width: 100%;
                  height: 100%;
                  object-fit: contain;
                  display: none;
                }
                .idle {
                  text-align: center;
                  padding: 24px;
                }
                .idle h1 { font-size: 28px; margin-bottom: 10px; }
                .idle p { color: #A9B2C8; font-size: 15px; }
                button {
                  background: rgba(255,255,255,0.12);
                  color: #fff;
                  border: 1px solid rgba(255,255,255,0.22);
                  border-radius: 14px;
                  padding: 8px 16px;
                  cursor: pointer;
                  font-weight: 600;
                }
              </style>
            </head>
            <body>
              <div class="glass">
                <div class="topbar">
                  <div>
                    <strong id="title">DZ Cast — جاهز للاستقبال</strong>
                  </div>
                  <div style="display:flex;gap:8px;align-items:center;">
                    <span class="badge" id="statusBadge">في الانتظار</span>
                    <button onclick="toggleFs()">ملء الشاشة</button>
                  </div>
                </div>
                <div class="stage" id="stage">
                  <div class="idle" id="idleBox">
                    <h1>DZ Cast</h1>
                    <p>أول مشروع جزائري لبث الفيديوهات ومشاركة شاشة الهاتف بدون إعلانات</p>
                    <p style="margin-top:8px;opacity:.75;">ابدأ البث أو اختر فيديو من هاتفك وسيظهر هنا تلقائياً</p>
                  </div>
                  <img id="mirror" alt="Live Screen Mirror" />
                  <video id="player" controls autoplay playsinline></video>
                </div>
              </div>
              <script>
                let lastMode = '';
                let lastUrl = '';
                const idleBox = document.getElementById('idleBox');
                const mirrorImg = document.getElementById('mirror');
                const videoEl = document.getElementById('player');
                const titleEl = document.getElementById('title');
                const badgeEl = document.getElementById('statusBadge');

                function toggleFs() {
                  const stage = document.getElementById('stage');
                  if (!document.fullscreenElement) stage.requestFullscreen?.();
                  else document.exitFullscreen?.();
                }

                async function poll() {
                  try {
                    const res = await fetch('/api/status?t=' + Date.now());
                    const data = await res.json();
                    titleEl.textContent = data.title || 'DZ Cast';
                    if (data.mode === 'mirror') {
                      badgeEl.textContent = 'بث مباشر • ' + (data.fps || 0) + ' FPS';
                      if (lastMode !== 'mirror') {
                        videoEl.pause();
                        videoEl.style.display = 'none';
                        idleBox.style.display = 'none';
                        mirrorImg.src = '/mirror.mjpeg?t=' + Date.now();
                        mirrorImg.style.display = 'block';
                        lastMode = 'mirror';
                        lastUrl = '/mirror.mjpeg';
                      }
                    } else if (data.mode === 'video' && data.mediaUrl) {
                      badgeEl.textContent = data.formatTag || 'VIDEO';
                      if (lastMode !== 'video' || lastUrl !== data.mediaUrl) {
                        mirrorImg.src = '';
                        mirrorImg.style.display = 'none';
                        idleBox.style.display = 'none';
                        videoEl.src = data.mediaUrl;
                        videoEl.style.display = 'block';
                        videoEl.play().catch(() => {});
                        lastMode = 'video';
                        lastUrl = data.mediaUrl;
                      }
                    } else if (lastMode !== 'idle') {
                      mirrorImg.src = '';
                      mirrorImg.style.display = 'none';
                      videoEl.pause();
                      videoEl.style.display = 'none';
                      idleBox.style.display = 'block';
                      badgeEl.textContent = 'في الانتظار';
                      lastMode = 'idle';
                      lastUrl = '';
                    }
                  } catch (e) {}
                }
                poll();
                setInterval(poll, 1600);
              </script>
            </body>
            </html>
        """.trimIndent()
        sendTextResponse(out, 200, "text/html; charset=utf-8", html)
    }
}
