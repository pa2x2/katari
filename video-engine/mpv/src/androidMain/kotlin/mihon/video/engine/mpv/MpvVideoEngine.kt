package mihon.video.engine.mpv

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.VideoStream
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.player.MpvPlayer
import mihon.video.engine.mpv.quality.MpvBandwidthMeter
import mihon.video.engine.spi.VideoEngine
import mihon.video.engine.spi.VideoPlayerRole
import mihon.video.engine.spi.loading.VideoRequestHeaders
import mihon.video.engine.spi.media3.Media3VideoPlayer
import mihon.video.engine.spi.player.VideoPlayer

/**
 * The engine on the mpv add-on. It plays the watched player only, in the foreground, the background and picture in
 * picture; the seek preview and the feed stay on ExoPlayer, and asking it for them is a programming error. Callers
 * check [MpvAddon.isAvailable] first: a player made without a usable add-on reports an error.
 */
@Inject
@SingleIn(AppScope::class)
class MpvVideoEngine(
    private val context: Application,
    private val addon: MpvAddon,
    private val networkHelper: NetworkHelper,
) : VideoEngine {

    private val bandwidthMeter = MpvBandwidthMeter()

    override fun createPlayer(stream: VideoStream, role: VideoPlayerRole): VideoPlayer {
        require(role is VideoPlayerRole.Watched) { "mpv plays only the watched player, not $role" }
        val player = MpvPlayer(
            context = context,
            addon = addon,
            client = networkHelper.client,
            stream = stream,
            headers = VideoRequestHeaders(stream, networkHelper::defaultUserAgentProvider),
            role = role,
            bandwidthMeter = bandwidthMeter,
        )
        return Media3VideoPlayer(player)
    }
}
