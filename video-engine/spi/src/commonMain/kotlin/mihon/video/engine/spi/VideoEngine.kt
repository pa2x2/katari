package mihon.video.engine.spi

import eu.kanade.tachiyomi.source.entry.VideoStream
import mihon.core.common.platform.PlatformFacility
import mihon.video.engine.spi.player.VideoPlayer

/**
 * Makes the players the anime player plays through and loads streams into them: the data sources, request headers,
 * downloaded files and media cache are the engine's. Everything above it holds the [VideoPlayer] it returns, already
 * set to [stream] and its external tracks but not yet prepared; the caller prepares and releases it. [stream] is already
 * resolved, so its request is set. A stream the engine can't play doesn't make [createPlayer] throw: the player reports
 * it as an error, where every caller already handles failures. Each platform has engines of its own.
 */
@PlatformFacility
interface VideoEngine {
    fun createPlayer(stream: VideoStream, role: VideoPlayerRole): VideoPlayer
}
