package com.mport.tv.domain.repository

import com.mport.tv.core.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun observeChannels(): Flow<List<Channel>>
    suspend fun replaceChannels(channels: List<Channel>)
    suspend fun setFavorite(id: String, favorite: Boolean)
}
