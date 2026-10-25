package eu.kanade.tachiyomi.ui.video.player.markers

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind

/**
 * The skip the player offers at a position: [targetMs] is where skipping [marker] lands, never past the end. When the
 * rest of the episode is credits or a preview and another episode follows, the button moves on to it instead
 * ([goesToNextEpisode]). The button stands out on its own while [prominent], then only shows with the controls.
 */
internal data class VideoPlayerSkipOffer(
    val marker: PlaybackMarker,
    val targetMs: Long,
    val goesToNextEpisode: Boolean,
    val prominent: Boolean,
)

/** [durationMs] is 0 or less while unknown. */
internal fun skipOfferAt(
    positionMs: Long,
    durationMs: Long,
    markers: List<PlaybackMarker>,
    hasNextEpisode: Boolean,
): VideoPlayerSkipOffer? {
    val marker = markers
        .filter { it.skippable && positionMs >= it.startMs && positionMs < it.endMs }
        .minByOrNull(PlaybackMarker::startMs)
        ?: return null
    val knownDuration = durationMs.takeIf { it > 0L }
    val reachesEnd = knownDuration != null && (
        marker.endMs >= knownDuration - END_TOLERANCE_MS ||
            markers.any { other ->
                other.kind == PlaybackMarkerKind.PREVIEW &&
                    other.startMs <= marker.endMs + END_TOLERANCE_MS &&
                    other.endMs >= knownDuration - END_TOLERANCE_MS
            }
        )
    return VideoPlayerSkipOffer(
        marker = marker,
        targetMs = knownDuration?.let { marker.endMs.coerceAtMost(it) } ?: marker.endMs,
        goesToNextEpisode = reachesEnd && hasNextEpisode,
        prominent = positionMs < marker.startMs + PROMINENT_MS,
    )
}

internal val PlaybackMarker.skippable: Boolean
    get() = kind in SkippableKinds

private val SkippableKinds = setOf(
    PlaybackMarkerKind.INTRO,
    PlaybackMarkerKind.MIXED_INTRO,
    PlaybackMarkerKind.OUTRO,
    PlaybackMarkerKind.RECAP,
    PlaybackMarkerKind.PREVIEW,
)

// Marker times come from the source, often to the second, so credits ending just short of the end still reach it.
private const val END_TOLERANCE_MS = 2_000L
private const val PROMINENT_MS = 8_000L
