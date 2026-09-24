package com.mport.tv.data.stalker

/** Authorized-portal adapter boundary. Configure only services you own or are authorized to use. */
class StalkerClient(private val config: PortalConfig) {
    suspend fun authenticate(): Result<String> = Result.failure(UnsupportedOperationException("Configure an authorized portal adapter."))
    suspend fun loadGroups(): List<PortalGroup> = emptyList()
    suspend fun loadChannels(): List<PortalChannel> = emptyList()
    suspend fun resolveStream(channelId: String): Result<String> = Result.failure(UnsupportedOperationException("Configure an authorized portal adapter."))
}
