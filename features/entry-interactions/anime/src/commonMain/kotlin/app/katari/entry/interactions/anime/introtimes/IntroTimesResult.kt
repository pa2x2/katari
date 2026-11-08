package app.katari.entry.interactions.anime.introtimes

import eu.kanade.tachiyomi.source.entry.PlaybackMarker

/** What looking an episode's times up with one service came to; [M] is what the service matched the episode's scope to. */
internal sealed interface IntroTimesResult<out M> {
    /** [markers] is empty when the service has no times for the episode. */
    data class Found<M>(val match: M, val markers: List<PlaybackMarker>) : IntroTimesResult<M>

    /** The scope matched [match], but the episode has no number to ask the service by. */
    data class Unnumbered<M>(val match: M) : IntroTimesResult<M>

    /** The service has nothing by the scope's [title]. */
    data class NoMatch(val title: String) : IntroTimesResult<Nothing>

    /** The viewer picked None for the scope. */
    data object TurnedOff : IntroTimesResult<Nothing>

    /** The service couldn't be reached, or answered with an error. */
    data object Unreachable : IntroTimesResult<Nothing>
}

/** What the scope matched, when it matched anything. */
internal val <M> IntroTimesResult<M>.matched: M?
    get() = when (this) {
        is IntroTimesResult.Found -> match
        is IntroTimesResult.Unnumbered -> match
        else -> null
    }
