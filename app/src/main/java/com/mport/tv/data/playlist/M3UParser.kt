package com.mport.tv.data.playlist

import com.mport.tv.core.model.Channel
import com.mport.tv.core.model.StreamType
import org.json.JSONObject

object M3UParser {
    private val attributeRegex = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

    fun parse(text: String): List<Channel> {
        val lines = text.lines().map { it.trimEnd() }
        val result = mutableListOf<Channel>()
        var attrs = emptyMap<String, String>()
        var name: String? = null
        val pendingHeaders = linkedMapOf<String, String>()

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    attrs = attributeRegex.findAll(line)
                        .associate { it.groupValues[1] to it.groupValues[2] }
                    name = line.substringAfter(",", "").trim()
                    pendingHeaders.clear()
                }
                line.startsWith("#EXTHTTP:", ignoreCase = true) -> {
                    parseExtHttp(line.substringAfter(":"), pendingHeaders)
                }
                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    parseVlcOpt(line.substringAfter(":"), pendingHeaders)
                }
                line.startsWith("#KODIPROP:inputstream.adaptive.stream_headers=", ignoreCase = true) -> {
                    parseKodiHeaders(line.substringAfter("="), pendingHeaders)
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
                        epgId = attrs["tvg-id"],
                        headers = pendingHeaders.toMap()
                    )
                    attrs = emptyMap()
                    name = null
                    pendingHeaders.clear()
                }
            }
        }
        return result
    }

    private fun parseExtHttp(json: String, out: MutableMap<String, String>) {
        runCatching {
            val o = JSONObject(json)
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = o.optString(k)
                if (v.isNotBlank()) out[normalizeHeader(k)] = v
            }
        }
    }

    private fun parseVlcOpt(opt: String, out: MutableMap<String, String>) {
        val idx = opt.indexOf('=')
        if (idx <= 0) return
        val key = opt.substring(0, idx).trim()
        val value = opt.substring(idx + 1).trim()
        when (key.lowercase()) {
            "http-referrer", "http-referer" -> out["Referer"] = value
            "http-origin" -> out["Origin"] = value
            "http-user-agent" -> out["User-Agent"] = value
            else -> if (value.isNotBlank()) out[key] = value
        }
    }

    private fun parseKodiHeaders(raw: String, out: MutableMap<String, String>) {
        raw.split("|").forEach { part ->
            val idx = part.indexOf('=')
            if (idx > 0) {
                val k = normalizeHeader(part.substring(0, idx).trim())
                val v = part.substring(idx + 1).trim()
                if (v.isNotBlank()) out[k] = v
            }
        }
    }

    private fun normalizeHeader(k: String): String = when (k.lowercase()) {
        "user-agent" -> "User-Agent"
        "referrer", "referer" -> "Referer"
        "origin" -> "Origin"
        else -> k
    }

    private fun detectType(url: String): StreamType {
        val u = url.lowercase()
        return when {
            ".m3u8" in u -> StreamType.HLS
            ".mpd" in u -> StreamType.DASH
            ".mp4" in u -> StreamType.MP4
            else -> StreamType.AUTO
        }
    }
}
