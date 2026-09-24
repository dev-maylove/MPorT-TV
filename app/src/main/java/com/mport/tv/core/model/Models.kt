package com.mport.tv.core.model

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val streamUrl: String,
    val streamType: StreamType = StreamType.AUTO,
    val epgId: String? = null,
    val isFavorite: Boolean = false,
    /** HTTP headers for stream requests (Referer, User-Agent, Origin, …) */
    val headers: Map<String, String> = emptyMap()
)

enum class StreamType { AUTO, HLS, DASH, MP4 }

data class Playlist(
    val id: String,
    val name: String,
    val sourceUrl: String? = null,
    val channels: List<Channel> = emptyList()
)

data class EpgProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startEpochMs: Long,
    val endEpochMs: Long
)
