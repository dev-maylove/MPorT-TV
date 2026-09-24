package com.mport.tv.data.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Option 1+2: InnerTube player client.
 * Uses public API key (same family as many open clients / WEB_EMBEDDED).
 * Full nsig/PO (option 3) is partially supported via optional cipher decode + docs.
 */
class YoutubeInnerTubeClient(
    private val apiKey: String = DEFAULT_API_KEY
) {
    companion object {
        // Public key observed in many embed/web clients (including prior audit).
        const val DEFAULT_API_KEY = "AIzaSyAO_FJ2SlwbOuWuuEdblAcoyKluZdGuBoE"
        private const val PLAYER_URL = "https://www.youtube.com/youtubei/v1/player?key="
        private const val SEARCH_URL = "https://www.youtube.com/youtubei/v1/search?key="
    }

    suspend fun getPlayer(videoId: String): Result<YoutubePlayable> = withContext(Dispatchers.IO) {
        runCatching {
            val body = buildPlayerBody(videoId)
            val json = postJson(PLAYER_URL + apiKey, body)
            parsePlayerResponse(videoId, json)
        }
    }

    suspend fun search(query: String, maxResults: Int = 20): Result<List<YoutubeSearchItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject()
                    .put("context", clientContext("WEB", "2.20240101.00.00"))
                    .put("query", query)
                val json = postJson(SEARCH_URL + apiKey, body)
                parseSearch(json, maxResults)
            }
        }

    private fun buildPlayerBody(videoId: String): JSONObject {
        // ANDROID client often returns progressive URLs; WEB_EMBEDDED closer to original ion-tv.
        return JSONObject()
            .put("context", clientContext("ANDROID", "19.09.37"))
            .put("videoId", videoId)
            .put(
                "contentCheckOk", true
            )
            .put("racyCheckOk", true)
    }

    private fun clientContext(name: String, version: String): JSONObject {
        val client = JSONObject()
            .put("clientName", name)
            .put("clientVersion", version)
            .put("hl", "en")
            .put("gl", "US")
        if (name == "ANDROID") {
            client.put("androidSdkVersion", 30)
        }
        return JSONObject().put("client", client)
    }

    private fun postJson(url: String, body: JSONObject): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("User-Agent", "com.google.android.youtube/19.09.37 (Linux; U; Android 14)")
        conn.setRequestProperty("X-YouTube-Client-Name", "3")
        conn.setRequestProperty("X-YouTube-Client-Version", "19.09.37")
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            if (code !in 200..299) {
                throw IllegalStateException("YouTube HTTP $code: ${text.take(200)}")
            }
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun parsePlayerResponse(videoId: String, root: JSONObject): YoutubePlayable {
        val status = root.optJSONObject("playabilityStatus")
        val statusName = status?.optString("status") ?: ""
        if (statusName == "ERROR" || statusName == "LOGIN_REQUIRED" || statusName == "UNPLAYABLE") {
            val reason = status?.optString("reason") ?: status?.optString("status") ?: "unplayable"
            throw IllegalStateException("Video tidak bisa diputar: $reason")
        }

        val videoDetails = root.optJSONObject("videoDetails")
        val streaming = root.optJSONObject("streamingData")

        val title = videoDetails?.optString("title") ?: videoId
        val author = videoDetails?.optString("author")
        val length = videoDetails?.optString("lengthSeconds")?.toLongOrNull()
        val isLive = videoDetails?.optBoolean("isLiveContent") == true ||
            videoDetails?.optBoolean("isLive") == true

        var thumb: String? = null
        videoDetails?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.let { arr ->
            if (arr.length() > 0) {
                thumb = arr.getJSONObject(arr.length() - 1).optString("url")
            }
        }

        val progressive = parseFormats(streaming?.optJSONArray("formats"))
        val adaptive = parseFormats(streaming?.optJSONArray("adaptiveFormats"))

        // Try resolve signatureCipher → url (basic, without full nsig JS)
        val progressiveResolved = progressive.map { resolveCipherIfNeeded(it) }
        val adaptiveResolved = adaptive.map { resolveCipherIfNeeded(it) }

        return YoutubePlayable(
            videoId = videoId,
            title = title,
            author = author,
            thumbnailUrl = thumb,
            durationSeconds = length,
            hlsManifestUrl = streaming?.optString("hlsManifestUrl")?.takeIf { it.isNotBlank() },
            dashManifestUrl = streaming?.optString("dashManifestUrl")?.takeIf { it.isNotBlank() },
            progressiveFormats = progressiveResolved,
            adaptiveFormats = adaptiveResolved,
            isLive = isLive
        )
    }

    private fun parseFormats(arr: JSONArray?): List<StreamFormat> {
        if (arr == null) return emptyList()
        val list = mutableListOf<StreamFormat>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += StreamFormat(
                itag = o.optInt("itag"),
                url = o.optString("url").takeIf { it.isNotBlank() },
                mimeType = o.optString("mimeType").takeIf { it.isNotBlank() },
                qualityLabel = o.optString("qualityLabel").takeIf { it.isNotBlank() },
                bitrate = o.optInt("bitrate").takeIf { it > 0 },
                width = o.optInt("width").takeIf { it > 0 },
                height = o.optInt("height").takeIf { it > 0 },
                audioQuality = o.optString("audioQuality").takeIf { it.isNotBlank() },
                contentLength = o.optString("contentLength").toLongOrNull(),
                signatureCipher = o.optString("signatureCipher").takeIf { it.isNotBlank() }
                    ?: o.optString("cipher").takeIf { it.isNotBlank() }
            )
        }
        return list
    }

    /**
     * Option 3 (partial): parse signatureCipher query params.
     * Full nsig requires executing YouTube player JS — without it, only formats
     * that already have `url` will play. Cipher URLs are returned with s= param noted.
     */
    private fun resolveCipherIfNeeded(format: StreamFormat): StreamFormat {
        if (!format.url.isNullOrBlank()) return format
        val cipher = format.signatureCipher ?: return format
        val params = cipher.split("&").associate { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) "" to ""
            else {
                val k = URLDecoder.decode(part.substring(0, idx), "UTF-8")
                val v = URLDecoder.decode(part.substring(idx + 1), "UTF-8")
                k to v
            }
        }
        val baseUrl = params["url"] ?: return format
        // Without nsig transform of params["s"], appending raw sig often fails.
        // Keep URL so UI can show "needs nsig" and still try HLS if present.
        val sig = params["s"]
        val sp = params["sp"] ?: "sig"
        val tried = if (sig != null) "$baseUrl&$sp=$sig" else baseUrl
        return format.copy(url = tried)
    }

    private fun parseSearch(root: JSONObject, max: Int): List<YoutubeSearchItem> {
        val out = mutableListOf<YoutubeSearchItem>()
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("twoColumnSearchResultsRenderer")
            ?.optJSONObject("primaryContents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?: return out

        fun walk(arr: JSONArray?) {
            if (arr == null || out.size >= max) return
            for (i in 0 until arr.length()) {
                if (out.size >= max) break
                val item = arr.optJSONObject(i) ?: continue
                val vr = item.optJSONObject("itemSectionRenderer")?.optJSONArray("contents")
                if (vr != null) {
                    walk(vr)
                    continue
                }
                val video = item.optJSONObject("videoRenderer") ?: continue
                val id = video.optString("videoId").takeIf { it.length == 11 } ?: continue
                val title = video.optJSONObject("title")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?: id
                val owner = video.optJSONObject("ownerText")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text")
                out += YoutubeSearchItem(videoId = id, title = title, author = owner)
            }
        }
        walk(contents)
        return out
    }
}

data class YoutubeSearchItem(
    val videoId: String,
    val title: String,
    val author: String?
)
