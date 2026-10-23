package eu.kanade.tachiyomi.ui.video.player.engine

import android.content.Context
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoSubtitle

/**
 * Makes the players the anime player plays through and loads streams into them: the data sources, request headers,
 * downloaded files and media cache are the engine's. Everything above it holds the Media3 [Player] it returns, already
 * set to [stream] and [subtitles] but not yet prepared; the caller prepares and releases it.
 *
 * Internal like the rest of this type module, whose public API the entry interaction boundary check limits to its
 * activities, graph entry points and binding containers. An engine in another module needs this moved to a module
 * both can depend on; its signature already uses only public types.
 */
internal interface VideoEngine {
    fun createPlayer(
        context: Context,
        stream: VideoStream,
        subtitles: List<VideoSubtitle>,
        role: VideoPlayerRole,
    ): Player
}

/** What a player is for, which decides how an engine sets it up. */
internal sealed interface VideoPlayerRole {
    /**
     * The player the viewer watches. It holds audio focus, pauses when the audio output goes away, keeps the device
     * awake while it plays, and seeks back and forward by [seekStepMs]. [metadata] is what the media notification
     * shows.
     */
    class Watched(
        val metadata: MediaMetadata,
        val seekStepMs: Long,
    ) : VideoPlayerRole

    /** The silent player behind the seek preview: small video frames only, and it never starts playing by itself. */
    data object SeekPreview : VideoPlayerRole

    /** A player in the immersive feed, which plays without taking audio focus. */
    data object Feed : VideoPlayerRole
}
