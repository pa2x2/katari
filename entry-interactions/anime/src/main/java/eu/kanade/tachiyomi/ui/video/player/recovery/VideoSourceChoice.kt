package eu.kanade.tachiyomi.ui.video.player.recovery

import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor

/**
 * What a problem screen offers to change when the episode's source lists alternatives, named after the first the
 * source offers in this order: a server, a dub, a source quality, a stream.
 */
internal enum class VideoSourceChoice { Server, Dub, Quality, Stream }

/** The choice a way out of a problem changes, or null when the source lists nothing else to play. */
internal fun PlaybackDescriptor.sourceChoice(): VideoSourceChoice? = when {
    servers.size > 1 -> VideoSourceChoice.Server
    dubs.size > 1 -> VideoSourceChoice.Dub
    sourceQualities.size > 1 -> VideoSourceChoice.Quality
    streams.size > 1 -> VideoSourceChoice.Stream
    else -> null
}
