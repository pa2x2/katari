package app.katari.video.engine.mpv

import android.app.Application
import app.katari.video.engine.mpv.addon.MpvAddon
import app.katari.video.engine.mpv.player.AndroidMpvPlatform
import app.katari.video.engine.mpv.player.MpvPlayer
import app.katari.video.engine.mpv.quality.MpvBandwidthMeter
import app.katari.video.engine.spi.VideoEngine
import app.katari.video.engine.spi.VideoPlayerRole
import app.katari.video.engine.spi.loading.VideoRequestHeaders
import app.katari.video.engine.spi.player.VideoPlayer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.VideoStream

/**
 * The engine on the mpv add-on: the shared mpv player on Android's mpv client ([AndroidMpvPlatform]). It plays the
 * watched player only, in the foreground, the background and picture in picture; the seek preview and the feed stay
 * on ExoPlayer, and asking it for them is a programming error. Callers check [MpvAddon.isAvailable] first: a player
 * made without a usable add-on reports an error.
 */
@Inject
@SingleIn(AppScope::class)
class MpvVideoEngine(
    context: Application,
    addon: MpvAddon,
    private val networkHelper: NetworkHelper,
) : VideoEngine {
    override val name: String = "mpv"

    private val platform = AndroidMpvPlatform(context, addon)
    private val bandwidthMeter = MpvBandwidthMeter()

    override fun createPlayer(stream: VideoStream, role: VideoPlayerRole): VideoPlayer {
        require(role is VideoPlayerRole.Watched) { "mpv plays only the watched player, not $role" }
        return MpvPlayer(
            platform = platform,
            client = networkHelper.client,
            stream = stream,
            headers = VideoRequestHeaders(stream, networkHelper::defaultUserAgentProvider),
            role = role,
            bandwidthMeter = bandwidthMeter,
        )
    }
}
