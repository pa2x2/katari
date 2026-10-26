package eu.kanade.tachiyomi.ui.video.player.recovery

import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import mihon.entry.interactions.anime.stream.streamChoiceKey
import mihon.entry.interactions.anime.stream.streamDisplayLabel

/**
 * A switch to another dub, server, stream or quality, or a retry, that failed while the episode played on. [selection]
 * is what was tried, for a retry; [target] names what it switched to, when it changed one choice the source lists.
 */
internal data class VideoSwitchFailure(
    val problem: VideoPlayerProblem,
    val selection: PlaybackSelection,
    val target: VideoSwitchTarget?,
)

/** [label] is null when the source didn't list the option by name among the playing stream's options. */
internal data class VideoSwitchTarget(val choice: VideoSourceChoice, val label: String?)

/** What [attempted] changed from [playing], the selection whose options [listed] holds; null for a retry. */
internal fun switchTarget(
    listed: PlaybackDescriptor,
    playing: PlaybackSelection,
    attempted: PlaybackSelection,
): VideoSwitchTarget? = when {
    attempted.dubKey != playing.dubKey ->
        VideoSwitchTarget(VideoSourceChoice.Dub, listed.dubs.firstOrNull { it.key == attempted.dubKey }?.label)
    attempted.serverKey != playing.serverKey ->
        VideoSwitchTarget(VideoSourceChoice.Server, listed.servers.firstOrNull { it.key == attempted.serverKey }?.label)
    attempted.sourceQualityKey != playing.sourceQualityKey -> VideoSwitchTarget(
        VideoSourceChoice.Quality,
        listed.sourceQualities.firstOrNull { it.key == attempted.sourceQualityKey }?.label,
    )
    attempted.streamKey != null && attempted.streamKey != playing.streamKey -> VideoSwitchTarget(
        VideoSourceChoice.Stream,
        listed.streams.firstOrNull { streamChoiceKey(it) == attempted.streamKey }?.let(::streamDisplayLabel),
    )
    else -> null
}
