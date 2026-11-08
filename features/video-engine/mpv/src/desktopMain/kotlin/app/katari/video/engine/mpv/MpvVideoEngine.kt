package app.katari.video.engine.mpv

import app.katari.video.engine.mpv.player.DesktopMpvPlatform
import app.katari.video.engine.mpv.player.MpvPlayer
import app.katari.video.engine.mpv.quality.MpvBandwidthMeter
import app.katari.video.engine.spi.VideoEngine
import app.katari.video.engine.spi.VideoPlayerRole
import app.katari.video.engine.spi.loading.VideoRequestHeaders
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.VideoStream

/**
 * The desktop's one engine: the shared mpv player on the libmpv the app carries ([DesktopMpvPlatform]), drawing
 * into Compose, for every role. The seek preview and the feed's clips measure their own throughput, so their loads
 * never sway the watched player's choice of quality: the feed loads several clips at once, which each see a share of
 * the connection.
 */
class MpvVideoEngine(private val networkHelper: NetworkHelper) : VideoEngine {
    private val platform = DesktopMpvPlatform()
    private val bandwidthMeter = MpvBandwidthMeter()
    private val feedBandwidthMeter = MpvBandwidthMeter()

    override fun createPlayer(stream: VideoStream, role: VideoPlayerRole): VideoPlayer {
        return MpvPlayer(
            platform = platform,
            client = networkHelper.client,
            stream = stream,
            headers = VideoRequestHeaders(stream, networkHelper::defaultUserAgentProvider),
            role = role,
            bandwidthMeter = when (role) {
                is VideoPlayerRole.Watched -> bandwidthMeter
                VideoPlayerRole.Feed -> feedBandwidthMeter
                VideoPlayerRole.SeekPreview -> MpvBandwidthMeter()
            },
        )
    }
}
