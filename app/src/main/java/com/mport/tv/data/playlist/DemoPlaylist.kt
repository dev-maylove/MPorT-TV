package com.mport.tv.data.playlist

import com.mport.tv.core.model.Channel
import com.mport.tv.core.model.StreamType

/**
 * Built-in demo / sample streams (public test content only).
 * These are the same public HLS demo URLs that appear in many open players
 * and were present as test assets in common media libraries.
 * NOT a private IPTV service playlist.
 */
object DemoPlaylist {

    fun channels(): List<Channel> = listOf(
        Channel(
            id = "demo-sintel",
            name = "Sintel (Demo HLS)",
            logoUrl = null,
            groupTitle = "Demo / Sample",
            streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            streamType = StreamType.HLS
        ),
        Channel(
            id = "demo-tears",
            name = "Tears of Steel (Demo)",
            logoUrl = null,
            groupTitle = "Demo / Sample",
            streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            streamType = StreamType.HLS
        ),
        Channel(
            id = "demo-mux",
            name = "Mux Test Stream",
            logoUrl = null,
            groupTitle = "Demo / Sample",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            streamType = StreamType.HLS
        ),
        Channel(
            id = "demo-apple-bipbop",
            name = "Apple BipBop (Demo)",
            logoUrl = null,
            groupTitle = "Demo / Sample",
            streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8",
            streamType = StreamType.HLS
        ),
        Channel(
            id = "demo-apple-adv",
            name = "Apple Advanced (Demo)",
            logoUrl = null,
            groupTitle = "Demo / Sample",
            streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8",
            streamType = StreamType.HLS
        )
    )
}
