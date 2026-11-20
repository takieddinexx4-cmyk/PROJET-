package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cast.AdBlockAndSniffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches DZ Cast`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DZ Cast", appName)
    }

    @Test
    fun `ad blocker blocks known ad domains`() {
        assertTrue(AdBlockAndSniffer.shouldBlockUrl("https://googleads.g.doubleclick.net/pagead/ads"))
        assertTrue(AdBlockAndSniffer.shouldBlockUrl("https://cdn.popads.net/pop.js"))
    }

    @Test
    fun `stream sniffer detects hls and mp4 streams`() {
        val hls = AdBlockAndSniffer.detectMediaFromUrl("https://stream.example.dz/live/channel.m3u8?token=1")
        assertNotNull(hls)
        assertEquals("HLS", hls?.formatTag)

        val mp4 = AdBlockAndSniffer.detectMediaFromUrl("https://cdn.example.dz/video/movie.mp4")
        assertNotNull(mp4)
        assertEquals("MP4", mp4?.formatTag)
    }

    @Test
    fun `m3u parser extracts channels cleanly`() {
        val sample = """
            #EXTM3U
            #EXTINF:-1 group-title="الجزائر",القناة الأولى
            https://live.example.dz/ch1.m3u8
            #EXTINF:-1 group-title="رياضة",قناة الرياضة
            https://live.example.dz/sport.m3u8
        """.trimIndent()
        val channels = AdBlockAndSniffer.parseM3uPlaylist(sample)
        assertEquals(2, channels.size)
        assertEquals("القناة الأولى", channels[0].title)
        assertEquals("الجزائر", channels[0].group)
    }
}
