package eu.kanade.tachiyomi.ui.video.player.markers

import androidx.compose.runtime.mutableStateSetOf
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind

/**
 * The skips that happen by themselves in one episode's visit, at once or after a countdown. Each marker gets one chance:
 * once it is skipped, or its countdown is stopped, seeking back into it or undoing the skip plays it. An intro or
 * credits that carry story never skip by themselves. Reading [pending] in composition follows claims and declines.
 */
internal class MarkerAutoSkip {
    private val handled = mutableStateSetOf<PlaybackMarker>()

    fun pending(offer: VideoPlayerSkipOffer): Boolean =
        offer.marker.kind !in CarriesStory && offer.marker !in handled

    /** Whether to skip [offer] now; the first yes for a marker is the only one. */
    fun claim(offer: VideoPlayerSkipOffer): Boolean = pending(offer) && handled.add(offer.marker)

    /** Lets [marker] play on without skipping by itself for the rest of the visit. */
    fun decline(marker: PlaybackMarker) {
        handled.add(marker)
    }
}

private val CarriesStory = setOf(PlaybackMarkerKind.MIXED_INTRO, PlaybackMarkerKind.MIXED_OUTRO)
