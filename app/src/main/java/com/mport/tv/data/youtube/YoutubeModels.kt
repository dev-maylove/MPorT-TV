package com.mport.tv.data.youtube

/**
 * Minimal models for YouTube InnerTube player response.
 * Not a full mirror of proprietary clients.
 */
data class YoutubePlayerRequest(
    val videoId: String,
    val clientName: String = "ANDROID",
    val clientVersion: String = "19.09.37"
)

data class StreamFormat(
    val itag: Int,
    val url: String?,
    val mimeType: String?,
    val qualityLabel: String?,
    val bitrate: Int?,
    val width: Int?,
    val height: Int?,
    val audioQuality: String?,
    val contentLength: Long?,
    /** Signature cipher when URL is not direct (needs nsig). */
    val signatureCipher: String? = null
)

data class YoutubePlayable(
    val videoId: String,
    val title: String,
    val author: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Long?,
    val hlsManifestUrl: String?,
    val dashManifestUrl: String?,
    val progressiveFormats: List<StreamFormat>,
    val adaptiveFormats: List<StreamFormat>,
    val isLive: Boolean = false
) {
    /** Best single URL for Media3 when possible (prefer HLS, then progressive mp4). */
    fun bestDirectUrl(): String? {
        hlsManifestUrl?.takeIf { it.isNotBlank() }?.let { return it }
        dashManifestUrl?.takeIf { it.isNotBlank() }?.let { return it }
        progressiveFormats
            .filter { !it.url.isNullOrBlank() && (it.mimeType?.contains("mp4") == true || it.mimeType == null) }
            .maxByOrNull { (it.height ?: 0) * 1000 + (it.bitrate ?: 0) }
            ?.url
            ?.let { return it }
        adaptiveFormats
            .filter { !it.url.isNullOrBlank() && it.mimeType?.contains("video") == true }
            .maxByOrNull { (it.height ?: 0) * 1000 + (it.bitrate ?: 0) }
            ?.url
            ?.let { return it }
        return adaptiveFormats.firstOrNull { !it.url.isNullOrBlank() }?.url
            ?: progressiveFormats.firstOrNull { !it.url.isNullOrBlank() }?.url
    }

    fun needsNsig(): Boolean =
        progressiveFormats.any { it.url.isNullOrBlank() && !it.signatureCipher.isNullOrBlank() } ||
            adaptiveFormats.any { it.url.isNullOrBlank() && !it.signatureCipher.isNullOrBlank() }
}
