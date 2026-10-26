package eu.kanade.tachiyomi.ui.video.player.markers

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind

/**
 * The automatic skips of one episode's visit. Each marker is skipped once: seeking back into it, or undoing the skip,
 * plays it. An intro or credits that carry story are never skipped automatically.
 */
internal class MarkerAutoSkip {
    private val skipped = mutableSetOf<PlaybackMarker>()

    /** Whether to skip [offer] now; the first yes for a marker is the only one. */
    fun claim(offer: VideoPlayerSkipOffer): Boolean {
        if (offer.marker.kind in CarriesStory) return false
        return skipped.add(offer.marker)
    }
}

private val CarriesStory = setOf(PlaybackMarkerKind.MIXED_INTRO, PlaybackMarkerKind.MIXED_OUTRO)
