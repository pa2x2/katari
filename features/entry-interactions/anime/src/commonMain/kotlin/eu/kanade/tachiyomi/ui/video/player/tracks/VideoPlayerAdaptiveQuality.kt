package eu.kanade.tachiyomi.ui.video.player.tracks

import app.katari.video.engine.spi.player.VideoPlayer
import app.katari.video.engine.spi.tracks.TrackKind
import app.katari.video.engine.spi.tracks.VideoTrackSelection
import app.katari.video.engine.spi.tracks.VideoTracks
import eu.kanade.tachiyomi.ui.video.player.VideoAdaptiveQualityOption
import eu.kanade.tachiyomi.ui.video.player.VideoAdaptiveQualityPreference
import kotlin.math.abs

internal fun VideoTracks.availableAdaptiveQualities(): List<VideoAdaptiveQualityOption> {
    val groups = groups.filter { it.kind == TrackKind.Video && it.supported }
    if (groups.isEmpty()) return emptyList()

    val heights = groups
        .flatMap { group -> group.tracks.filter { it.supported } }
        .mapNotNull { track -> track.height?.takeIf { it > 0 } }
        .distinct()
        .sorted()

    return buildList {
        add(VideoAdaptiveQualityOption(label = "Auto", preference = VideoAdaptiveQualityPreference.Auto))
        heights.forEach { height ->
            add(
                VideoAdaptiveQualityOption(
                    label = "${height}p",
                    preference = VideoAdaptiveQualityPreference.SpecificHeight(height),
                ),
            )
        }
    }
}

/**
 * Plays the quality [preference] asks for: the variant nearest its height when the stream lists variants, otherwise any
 * variant no taller than it.
 */
internal fun VideoPlayer.applyAdaptiveQuality(preference: VideoAdaptiveQualityPreference) {
    selectVideo(
        when (preference) {
            VideoAdaptiveQualityPreference.Auto -> VideoTrackSelection.Auto
            is VideoAdaptiveQualityPreference.SpecificHeight ->
                state.value.tracks.nearestVariant(preference.height)
                    ?: VideoTrackSelection.MaxHeight(preference.height)
        },
    )
}

private fun VideoTracks.nearestVariant(targetHeight: Int): VideoTrackSelection.Variant? =
    groups.withIndex()
        .filter { (_, group) -> group.kind == TrackKind.Video && group.supported }
        .mapNotNull { (groupIndex, group) ->
            group.tracks.withIndex()
                .filter { (_, track) -> track.supported }
                .mapNotNull { (trackIndex, track) ->
                    track.height?.takeIf { it > 0 }?.let { abs(it - targetHeight) to trackIndex }
                }
                .minByOrNull { (distance, _) -> distance }
                ?.let { (distance, trackIndex) -> distance to VideoTrackSelection.Variant(groupIndex, trackIndex) }
        }
        .minByOrNull { (distance, _) -> distance }
        ?.second
