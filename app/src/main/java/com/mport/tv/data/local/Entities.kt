package com.mport.tv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String?,
    val streamUrl: String,
    val streamType: String,
    val epgId: String?,
    val isFavorite: Boolean,
    val headersJson: String? = null
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val lastWatchedEpochMs: Long
)
