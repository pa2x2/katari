package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.Serializable

/**
 * Playback metadata returned by anime sources.
 *
 * @param selection The resolved or fallback [PlaybackSelection].
 * @param dubs Available dub options.
 * @param sourceQualities Available source quality options.
 * @param streams Playable streams; the player picks the best match for the selection.
 */
@Serializable
data class PlaybackDescriptor(
    val selection: PlaybackSelection = PlaybackSelection(),
    val dubs: List<VideoPlaybackOption> = emptyList(),
    val sourceQualities: List<VideoPlaybackOption> = emptyList(),
    val streams: List<VideoStream> = emptyList(),
)
