package com.mport.tv.data.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class M3UParserTest {
    @Test
    fun parsesBasicPlaylist() {
        val input = """#EXTM3U
#EXTINF:-1 tvg-id="news" tvg-logo="https://example.invalid/logo.png" group-title="News",News One
https://example.invalid/live/news.m3u8"""

        val result = M3UParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("News One", result.first().name)
    }
}
