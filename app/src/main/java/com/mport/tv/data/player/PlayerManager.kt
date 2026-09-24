package com.mport.tv.data.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import com.mport.tv.core.model.Channel
import com.mport.tv.core.model.StreamType

class PlayerManager(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    fun play(channel: Channel, drm: DrmConfig = DrmConfig(DrmScheme.NONE)) {
        val b = MediaItem.Builder().setUri(channel.streamUrl).setMimeType(when(channel.streamType){
            StreamType.HLS -> MimeTypes.APPLICATION_M3U8
            StreamType.DASH -> MimeTypes.APPLICATION_MPD
            StreamType.MP4 -> MimeTypes.VIDEO_MP4
            StreamType.AUTO -> null
        })
        when(drm.scheme){
            DrmScheme.WIDEVINE -> b.setDrmConfiguration(MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID).setLicenseUri(drm.licenseUrl ?: "").setLicenseRequestHeaders(drm.requestHeaders).build())
            DrmScheme.CLEAR_KEY -> b.setDrmConfiguration(MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).setLicenseUri(drm.licenseUrl ?: "").setLicenseRequestHeaders(drm.requestHeaders).build())
            DrmScheme.NONE -> Unit
        }
        player.setMediaItem(b.build()); player.prepare(); player.playWhenReady = true
    }
    fun release() = player.release()
}
