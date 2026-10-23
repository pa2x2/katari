package mihon.video.engine.mpv

import android.content.Context
import androidx.media3.common.Player
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.player.MpvPlayer
import mihon.video.engine.spi.VideoEngine
import mihon.video.engine.spi.VideoPlayerRole
import mihon.video.engine.spi.loading.VideoRequestHeaders

/**
 * The engine on the mpv add-on. It plays the watched player only, in the foreground, the background and picture in
 * picture; the seek preview and the feed stay on ExoPlayer, and asking it for them is a programming error. Callers
 * check [MpvAddon.isAvailable] first: a player made without a usable add-on reports an error.
 */
@Inject
@SingleIn(AppScope::class)
class MpvVideoEngine(
    private val addon: MpvAddon,
    private val networkHelper: NetworkHelper,
) : VideoEngine {

    override fun createPlayer(
        context: Context,
        stream: VideoStream,
        subtitles: List<VideoSubtitle>,
        role: VideoPlayerRole,
    ): Player {
        require(role is VideoPlayerRole.Watched) { "mpv plays only the watched player, not $role" }
        return MpvPlayer(
            context = context,
            addon = addon,
            client = networkHelper.client,
            stream = stream,
            subtitles = subtitles,
            headers = VideoRequestHeaders(stream, subtitles, networkHelper::defaultUserAgentProvider),
            role = role,
        )
    }
}
