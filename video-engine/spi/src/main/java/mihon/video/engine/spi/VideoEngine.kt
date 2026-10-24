package mihon.video.engine.spi

import android.content.Context
import androidx.media3.common.Player
import eu.kanade.tachiyomi.source.entry.VideoStream

/**
 * Makes the players the anime player plays through and loads streams into them: the data sources, request headers,
 * downloaded files and media cache are the engine's. Everything above it holds the Media3 [Player] it returns, already
 * set to [stream] and its external tracks but not yet prepared; the caller prepares and releases it. [stream] is already
 * resolved, so its request is set. A stream the engine can't play doesn't make [createPlayer] throw: the player reports
 * it through [androidx.media3.common.Player.Listener.onPlayerError], where every caller already handles failures.
 */
interface VideoEngine {
    fun createPlayer(
        context: Context,
        stream: VideoStream,
        role: VideoPlayerRole,
    ): Player
}
