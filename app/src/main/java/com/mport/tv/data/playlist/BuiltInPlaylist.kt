package com.mport.tv.data.playlist

import android.content.Context
import com.mport.tv.core.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BuiltInPlaylist {
    const val ASSET_NAME = "mporttv.m3u8"

    suspend fun load(context: Context): List<Channel> = withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        M3UParser.parse(text)
    }
}
