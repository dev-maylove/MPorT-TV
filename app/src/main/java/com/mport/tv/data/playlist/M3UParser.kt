package com.mport.tv.data.playlist

import com.mport.tv.core.model.Channel
import com.mport.tv.core.model.StreamType

object M3UParser {
    private val attributeRegex = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

    fun parse(text: String): List<Channel> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<Channel>()
        var attrs = emptyMap<String, String>()
        var name: String? = null

        for (line in lines) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    attrs = attributeRegex.findAll(line)
                        .associate { it.groupValues[1] to it.groupValues[2] }
                    name = line.substringAfter(",", "").trim()
                }
                !line.startsWith("#") && name != null -> {
                    val n = name!!
                    val url = line
                    val id = attrs["tvg-id"]?.takeIf { it.isNotBlank() }
                        ?: (n + "|" + url).hashCode().toUInt().toString()

                    result += Channel(
                        id = id,
                        name = n,
                        logoUrl = attrs["tvg-logo"],
                        groupTitle = attrs["group-title"],
                        streamUrl = url,
                        streamType = detectType(url),
                        epgId = attrs["tvg-id"]
                    )
                    attrs = emptyMap()
                    name = null
                }
            }
        }
        return result
    }

    private fun detectType(url: String): StreamType = when {
        ".m3u8" in url.lowercase() -> StreamType.HLS
        ".mpd" in url.lowercase() -> StreamType.DASH
        ".mp4" in url.lowercase() -> StreamType.MP4
        else -> StreamType.AUTO
    }
}
