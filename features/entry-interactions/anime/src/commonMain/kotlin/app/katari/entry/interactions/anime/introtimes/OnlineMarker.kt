package app.katari.entry.interactions.anime.introtimes

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind

/** A marker found online, with the service it came from. */
internal data class OnlineMarker(val marker: PlaybackMarker, val service: IntroTimesService)

/**
 * The markers the player goes by out of each service's [found] ones. A service earlier in [IntroTimesService] wins: a
 * later one only adds the kinds the earlier ones don't mark, and none of its spans that overlap one already taken.
 */
internal fun mergeIntroTimes(found: Map<IntroTimesService, List<PlaybackMarker>>): List<OnlineMarker> {
    val taken = mutableListOf<OnlineMarker>()
    IntroTimesService.entries.forEach { service ->
        val takenParts = taken.mapTo(HashSet()) { it.marker.kind.part }
        val earlier = taken.toList()
        found[service].orEmpty()
            .filter { marker -> marker.kind.part !in takenParts && earlier.none { it.marker overlaps marker } }
            .mapTo(taken) { OnlineMarker(it, service) }
    }
    return taken
}

// An intro or credits over story is still the episode's intro or credits.
private val PlaybackMarkerKind.part: PlaybackMarkerKind
    get() = when (this) {
        PlaybackMarkerKind.MIXED_INTRO -> PlaybackMarkerKind.INTRO
        PlaybackMarkerKind.MIXED_OUTRO -> PlaybackMarkerKind.OUTRO
        else -> this
    }

private infix fun PlaybackMarker.overlaps(other: PlaybackMarker): Boolean =
    startMs < other.endMs && other.startMs < endMs
