package com.example.cast

import android.content.Context
import android.net.wifi.WifiManager
import com.example.data.model.CastDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.TimeUnit

class SsdpDiscoveryManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    suspend fun discoverDevices(timeoutMs: Long = 3200L): List<CastDevice> = withContext(Dispatchers.IO) {
        val discoveredLocations = linkedSetOf<String>()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("dz_cast_ssdp_lock")?.apply {
            setReferenceCounted(false)
            runCatching { acquire() }
        }

        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 700
                val group = InetAddress.getByName("239.255.255.250")
                val searchTargets = listOf(
                    "urn:schemas-upnp-org:device:MediaRenderer:1",
                    "urn:schemas-upnp-org:service:AVTransport:1"
                )
                for (st in searchTargets) {
                    val query = buildString {
                        append("M-SEARCH * HTTP/1.1\r\n")
                        append("HOST: 239.255.255.250:1900\r\n")
                        append("MAN: \"ssdp:discover\"\r\n")
                        append("MX: 2\r\n")
                        append("ST: $st\r\n\r\n")
                    }.toByteArray()
                    val packet = DatagramPacket(query, query.size, group, 1900)
                    runCatching { socket.send(packet) }
                }

                val endTime = System.currentTimeMillis() + timeoutMs
                val buffer = ByteArray(4096)
                while (System.currentTimeMillis() < endTime) {
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(responsePacket)
                        val responseText = String(responsePacket.data, 0, responsePacket.length)
                        extractHeader(responseText, "LOCATION")?.let { loc ->
                            discoveredLocations.add(loc)
                        }
                    } catch (_: SocketTimeoutException) {
                        // Continue until overall timeout expires
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        } catch (_: Exception) {
            // Network or multicast unavailable
        } finally {
            runCatching {
                if (multicastLock?.isHeld == true) multicastLock.release()
            }
        }

        discoveredLocations.mapNotNull { locationUrl ->
            fetchAndParseDeviceDescription(locationUrl)
        }.distinctBy { it.ipAddress + it.avTransportControlUrl }
    }

    private fun extractHeader(raw: String, headerName: String): String? {
        return raw.lines().firstOrNull { line ->
            line.startsWith("$headerName:", ignoreCase = true)
        }?.substringAfter(":")?.trim()
    }

    private fun fetchAndParseDeviceDescription(locationUrl: String): CastDevice? {
        return runCatching {
            val req = Request.Builder().url(locationUrl).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val xml = resp.body?.string().orEmpty()
                if (xml.isBlank()) return null
                parseDeviceXml(locationUrl, xml)
            }
        }.getOrNull()
    }

    private fun parseDeviceXml(locationUrl: String, xml: String): CastDevice? {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var friendlyName = ""
        var manufacturer = ""
        var modelName = ""
        var currentServiceType = ""
        var avTransportControl = ""
        var renderingControl = ""
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> currentTag = parser.name.orEmpty()
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        when (currentTag) {
                            "friendlyName" -> if (friendlyName.isEmpty()) friendlyName = text
                            "manufacturer" -> if (manufacturer.isEmpty()) manufacturer = text
                            "modelName" -> if (modelName.isEmpty()) modelName = text
                            "serviceType" -> currentServiceType = text
                            "controlURL" -> {
                                when {
                                    currentServiceType.contains("AVTransport", ignoreCase = true) ->
                                        avTransportControl = resolveControlUrl(locationUrl, text)
                                    currentServiceType.contains("RenderingControl", ignoreCase = true) ->
                                        renderingControl = resolveControlUrl(locationUrl, text)
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "service") currentServiceType = ""
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        if (avTransportControl.isBlank()) return null

        val parsedUrl = URL(locationUrl)
        return CastDevice(
            id = "dlna_${parsedUrl.host}_${parsedUrl.port}",
            name = friendlyName.ifBlank { "Smart TV (${parsedUrl.host})" },
            manufacturer = manufacturer.ifBlank { "DLNA / UPnP" },
            modelName = modelName.ifBlank { "Media Renderer" },
            ipAddress = parsedUrl.host,
            avTransportControlUrl = avTransportControl,
            renderingControlUrl = renderingControl,
            locationUrl = locationUrl,
            isWebReceiver = false
        )
    }

    private fun resolveControlUrl(locationUrl: String, controlPath: String): String {
        return runCatching {
            val base = URL(locationUrl)
            URL(base, controlPath).toString()
        }.getOrDefault(controlPath)
    }
}
