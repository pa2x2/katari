package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.Serializable

/**
 * Playback metadata returned by anime sources.
 *
 * Dubs, servers and source qualities are independent axes: picking one makes the host call
 * [UnifiedSource.getMedia] again with the new [PlaybackSelection], so a source only needs to
 * return the streams of the selected combination.
 *
 * @param selection The resolved or fallback [PlaybackSelection].
 * @param dubs Available dub options.
 * @param servers Available servers (hosters) for the selected dub. When the selected server
 * fails before playback starts, the host tries the next one in this order.
 * @param sourceQualities Available source quality options.
 * @param streams Playable streams of the selected combination; the player picks the best match.
 */
@Serializable
data class PlaybackDescriptor(
    val selection: PlaybackSelection = PlaybackSelection(),
    val dubs: List<VideoPlaybackOption> = emptyList(),
    val servers: List<VideoPlaybackOption> = emptyList(),
    val sourceQualities: List<VideoPlaybackOption> = emptyList(),
    val streams: List<VideoStream> = emptyList(),
)
