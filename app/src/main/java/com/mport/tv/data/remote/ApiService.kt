package com.mport.tv.data.remote

import retrofit2.http.GET

interface ApiService {
    @GET("channels")
    suspend fun channels(): List<RemoteChannel>

    @GET("epg")
    suspend fun epg(): List<RemoteEpg>
}

data class RemoteChannel(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val streamUrl: String,
    val streamType: String? = null,
    val epgId: String? = null
)

data class RemoteEpg(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startEpochMs: Long,
    val endEpochMs: Long
)
