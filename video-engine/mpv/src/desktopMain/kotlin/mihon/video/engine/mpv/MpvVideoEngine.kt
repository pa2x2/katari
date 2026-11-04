package mihon.video.engine.mpv

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.VideoStream
import mihon.video.engine.mpv.player.DesktopMpvPlatform
import mihon.video.engine.mpv.player.MpvPlayer
import mihon.video.engine.mpv.quality.MpvBandwidthMeter
import mihon.video.engine.spi.VideoEngine
import mihon.video.engine.spi.VideoPlayerRole
import mihon.video.engine.spi.loading.VideoRequestHeaders
import mihon.video.engine.spi.player.VideoPlayer

/**
 * The desktop's one engine: the shared mpv player on the libmpv the app carries ([DesktopMpvPlatform]), drawing
 * into Compose. It plays the watched player and the seek preview; the desktop has no feed yet, and asking it for a feed
 * player is a programming error. The seek preview measures its own throughput, so its loads never sway the watched
 * player's choice of quality.
 */
class MpvVideoEngine(private val networkHelper: NetworkHelper) : VideoEngine {
    private val platform = DesktopMpvPlatform()
    private val bandwidthMeter = MpvBandwidthMeter()

    override fun createPlayer(stream: VideoStream, role: VideoPlayerRole): VideoPlayer {
        require(role !is VideoPlayerRole.Feed) { "The desktop has no feed for mpv to play" }
        return MpvPlayer(
            platform = platform,
            client = networkHelper.client,
            stream = stream,
            headers = VideoRequestHeaders(stream, networkHelper::defaultUserAgentProvider),
            role = role,
            bandwidthMeter = if (role is VideoPlayerRole.Watched) bandwidthMeter else MpvBandwidthMeter(),
        )
    }
}
