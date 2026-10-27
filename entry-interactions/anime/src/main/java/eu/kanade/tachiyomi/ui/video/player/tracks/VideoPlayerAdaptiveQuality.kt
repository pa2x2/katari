package eu.kanade.tachiyomi.ui.video.player.tracks

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import eu.kanade.tachiyomi.ui.video.player.VideoAdaptiveQualityOption
import eu.kanade.tachiyomi.ui.video.player.VideoAdaptiveQualityPreference

internal fun Player.availableAdaptiveQualities(): List<VideoAdaptiveQualityOption> {
    val groups = currentTracks.groups
        .filter { it.getType() == C.TRACK_TYPE_VIDEO && it.isSupported() }
    if (groups.isEmpty()) return emptyList()

    val heights = groups
        .flatMap { group ->
            (0 until group.length)
                .filter { group.isTrackSupported(it) }
                .mapNotNull { index -> group.getTrackFormat(index).height.takeIf { it > 0 } }
        }
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

internal fun Player.applyAdaptiveQuality(preference: VideoAdaptiveQualityPreference) {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .setForceLowestBitrate(false)
        .setForceHighestSupportedBitrate(false)

    when (preference) {
        VideoAdaptiveQualityPreference.Auto -> {
            builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        }
        is VideoAdaptiveQualityPreference.SpecificHeight -> {
            val override = currentTracks.groups
                .asSequence()
                .filter { it.getType() == C.TRACK_TYPE_VIDEO && it.isSupported() }
                .mapNotNull { group -> preferredTrackOverride(group, preference.height) }
                .minByOrNull { it.first }
                ?.second

            if (override != null) {
                builder.setOverrideForType(override)
            } else {
                builder.setMaxVideoSize(Int.MAX_VALUE, preference.height)
            }
        }
    }

    trackSelectionParameters = builder.build()
}

private fun preferredTrackOverride(group: Tracks.Group, targetHeight: Int): Pair<Int, TrackSelectionOverride>? {
    val candidate = (0 until group.length)
        .filter { group.isTrackSupported(it) }
        .mapNotNull { index ->
            val height = group.getTrackFormat(index).height.takeIf { it > 0 } ?: return@mapNotNull null
            index to height
        }
        .minByOrNull { (_, height) -> kotlin.math.abs(height - targetHeight) }
        ?: return null

    val trackIndex = candidate.first
    val heightDistance = kotlin.math.abs(candidate.second - targetHeight)
    return heightDistance to TrackSelectionOverride(group.getMediaTrackGroup(), trackIndex)
}
