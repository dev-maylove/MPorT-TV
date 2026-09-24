package com.mport.tv.data.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.mport.tv.core.model.Channel
import com.mport.tv.core.model.StreamType

class PlayerManager(context: Context) {

    private val appContext = context.applicationContext

    private fun buildPlayer(headers: Map<String, String>): ExoPlayer {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setUserAgent(
                headers["User-Agent"]
                    ?: "MPorT-TV/1.0 (Linux; Android) ExoPlayer"
            )
        if (headers.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(headers)
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
            .setDataSourceFactory(httpFactory)
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    var player: ExoPlayer = buildPlayer(emptyMap())
        private set

    fun play(channel: Channel, drm: DrmConfig = DrmConfig(DrmScheme.NONE)) {
        // Rebuild player when headers change (dens.tv etc.)
        player.release()
        player = buildPlayer(channel.headers)

        val mime = when (channel.streamType) {
            StreamType.HLS -> MimeTypes.APPLICATION_M3U8
            StreamType.DASH -> MimeTypes.APPLICATION_MPD
            StreamType.MP4 -> MimeTypes.VIDEO_MP4
            StreamType.AUTO -> null
        }
        val builder = MediaItem.Builder().setUri(channel.streamUrl)
        if (mime != null) builder.setMimeType(mime)

        when (drm.scheme) {
            DrmScheme.WIDEVINE -> builder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(drm.licenseUrl ?: "")
                    .setLicenseRequestHeaders(drm.requestHeaders)
                    .build()
            )
            DrmScheme.CLEAR_KEY -> builder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                    .setLicenseUri(drm.licenseUrl ?: "")
                    .setLicenseRequestHeaders(drm.requestHeaders)
                    .build()
            )
            DrmScheme.NONE -> Unit
        }
        player.setMediaItem(builder.build())
        player.prepare()
        player.playWhenReady = true
    }

    fun release() {
        player.release()
    }
}
