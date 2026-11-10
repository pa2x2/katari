package app.katari.video.engine.spi

import app.katari.core.common.platform.PlatformFacility
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.source.entry.VideoStream

/**
 * Makes the players the anime player plays through and loads streams into them: the data sources, request headers,
 * downloaded files and media cache are the engine's. Everything above it holds the [VideoPlayer] it returns, already
 * set to [stream] and its external tracks but not yet prepared; the caller prepares and releases it. [stream] is already
 * resolved, so its request is set. A stream the engine can't play doesn't make [createPlayer] throw: the player reports
 * it as an error, where every caller already handles failures. Each platform has engines of its own.
 */
@PlatformFacility
interface VideoEngine {
    /** The engine's name in lower case, such as `mpv`, as the player's usage statistics report it. */
    val name: String

    fun createPlayer(stream: VideoStream, role: VideoPlayerRole): VideoPlayer
}
