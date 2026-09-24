package com.mport.tv.data.repository

import com.mport.tv.core.model.Channel
import com.mport.tv.core.model.StreamType
import com.mport.tv.data.local.AppDatabase
import com.mport.tv.data.local.ChannelEntity
import com.mport.tv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class ChannelRepositoryImpl(private val db: AppDatabase) : ChannelRepository {
    override fun observeChannels(): Flow<List<Channel>> =
        db.channelDao().observeAll().map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun replaceChannels(channels: List<Channel>) {
        db.channelDao().clear()
        db.channelDao().insertAll(channels.map { it.toEntity() })
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) =
        db.channelDao().setFavorite(id, favorite)

    private fun ChannelEntity.toDomain(): Channel {
        val headers = linkedMapOf<String, String>()
        if (!headersJson.isNullOrBlank()) {
            runCatching {
                val o = JSONObject(headersJson)
                val keys = o.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    headers[k] = o.optString(k)
                }
            }
        }
        return Channel(
            id = id,
            name = name,
            logoUrl = logoUrl,
            groupTitle = groupTitle,
            streamUrl = streamUrl,
            streamType = runCatching { StreamType.valueOf(streamType) }
                .getOrDefault(StreamType.AUTO),
            epgId = epgId,
            isFavorite = isFavorite,
            headers = headers
        )
    }

    private fun Channel.toEntity(): ChannelEntity {
        val json = if (headers.isEmpty()) null else {
            JSONObject().apply { headers.forEach { (k, v) -> put(k, v) } }.toString()
        }
        return ChannelEntity(
            id = id,
            name = name,
            logoUrl = logoUrl,
            groupTitle = groupTitle,
            streamUrl = streamUrl,
            streamType = streamType.name,
            epgId = epgId,
            isFavorite = isFavorite,
            headersJson = json
        )
    }
}
