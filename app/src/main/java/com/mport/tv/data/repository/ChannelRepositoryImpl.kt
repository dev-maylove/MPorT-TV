package com.mport.tv.data.repository

import com.mport.tv.core.model.Channel
import com.mport.tv.core.model.StreamType
import com.mport.tv.data.local.AppDatabase
import com.mport.tv.data.local.ChannelEntity
import com.mport.tv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChannelRepositoryImpl(private val db: AppDatabase) : ChannelRepository {
    override fun observeChannels(): Flow<List<Channel>> =
        db.channelDao().observeAll().map { rows ->
            rows.map {
                Channel(
                    id = it.id,
                    name = it.name,
                    logoUrl = it.logoUrl,
                    groupTitle = it.groupTitle,
                    streamUrl = it.streamUrl,
                    streamType = runCatching { StreamType.valueOf(it.streamType) }
                        .getOrDefault(StreamType.AUTO),
                    epgId = it.epgId,
                    isFavorite = it.isFavorite
                )
            }
        }

    override suspend fun replaceChannels(channels: List<Channel>) {
        db.channelDao().clear()
        db.channelDao().insertAll(channels.map {
            ChannelEntity(
                it.id, it.name, it.logoUrl, it.groupTitle, it.streamUrl,
                it.streamType.name, it.epgId, it.isFavorite
            )
        })
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) =
        db.channelDao().setFavorite(id, favorite)
}
