package com.example.cast

import com.example.data.model.CastDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DlnaCastController {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val soapXmlMediaType = "text/xml; charset=\"utf-8\"".toMediaType()

    suspend fun castMedia(
        device: CastDevice,
        mediaUrl: String,
        title: String,
        mimeType: String = "video/mp4"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (device.avTransportControlUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("لا يوجد رابط تحكم DLNA لهذا الجهاز"))
        }

        val escapedUrl = escapeXml(mediaUrl)
        val escapedTitle = escapeXml(title)
        val didlMetadata = escapeXml(
            """<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"><item id="0" parentID="-1" restricted="1"><dc:title>$escapedTitle</dc:title><upnp:class>object.item.videoItem</upnp:class><res protocolInfo="http-get:*:$mimeType:*">$escapedUrl</res></item></DIDL-Lite>"""
        )

        val setUriBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <CurrentURI>$escapedUrl</CurrentURI>
                  <CurrentURIMetaData>$didlMetadata</CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val setRes = sendSoapAction(
            controlUrl = device.avTransportControlUrl,
            serviceType = "urn:schemas-upnp-org:service:AVTransport:1",
            action = "SetAVTransportURI",
            envelope = setUriBody
        )
        if (setRes.isFailure) return@withContext setRes

        play(device)
    }

    suspend fun play(device: CastDevice): Result<Unit> = withContext(Dispatchers.IO) {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <Speed>1</Speed>
                </u:Play>
              </s:Body>
            </s:Envelope>
        """.trimIndent()
        sendSoapAction(
            device.avTransportControlUrl,
            "urn:schemas-upnp-org:service:AVTransport:1",
            "Play",
            body
        )
    }

    suspend fun pause(device: CastDevice): Result<Unit> = withContext(Dispatchers.IO) {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:Pause xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                </u:Pause>
              </s:Body>
            </s:Envelope>
        """.trimIndent()
        sendSoapAction(
            device.avTransportControlUrl,
            "urn:schemas-upnp-org:service:AVTransport:1",
            "Pause",
            body
        )
    }

    suspend fun stop(device: CastDevice): Result<Unit> = withContext(Dispatchers.IO) {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:Stop xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                </u:Stop>
              </s:Body>
            </s:Envelope>
        """.trimIndent()
        sendSoapAction(
            device.avTransportControlUrl,
            "urn:schemas-upnp-org:service:AVTransport:1",
            "Stop",
            body
        )
    }

    suspend fun setVolume(device: CastDevice, volumePercent: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (device.renderingControlUrl.isBlank()) return@withContext Result.success(Unit)
        val clamped = volumePercent.coerceIn(0, 100)
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:SetVolume xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
                  <InstanceID>0</InstanceID>
                  <Channel>Master</Channel>
                  <DesiredVolume>$clamped</DesiredVolume>
                </u:SetVolume>
              </s:Body>
            </s:Envelope>
        """.trimIndent()
        sendSoapAction(
            device.renderingControlUrl,
            "urn:schemas-upnp-org:service:RenderingControl:1",
            "SetVolume",
            body
        )
    }

    private fun sendSoapAction(
        controlUrl: String,
        serviceType: String,
        action: String,
        envelope: String
    ): Result<Unit> {
        return runCatching {
            val request = Request.Builder()
                .url(controlUrl)
                .addHeader("SOAPACTION", "\"$serviceType#$action\"")
                .addHeader("Content-Type", "text/xml; charset=\"utf-8\"")
                .post(envelope.toRequestBody(soapXmlMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("خطأ من جهاز التلفاز (HTTP ${response.code})")
                }
            }
        }
    }

    private fun escapeXml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
