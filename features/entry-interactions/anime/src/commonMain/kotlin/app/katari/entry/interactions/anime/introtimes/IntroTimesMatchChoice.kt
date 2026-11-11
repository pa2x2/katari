package app.katari.entry.interactions.anime.introtimes

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import java.time.LocalDate

/**
 * What a service's match for a scope is picked from: the series and films [search] finds, of type [C], each opened
 * into the episodes the playing one is pointed at among. [M] is what the service keeps as the scope's match.
 */
internal interface IntroTimesMatchSource<C, M> {
    suspend fun search(query: String): List<C>

    /** [match], which is in use, with [episode] where it places it; null when its episodes can't be reached. */
    suspend fun openMatch(match: M, episode: IntroTimesEpisode): IntroTimesMatchChoice<M>?

    /** [candidate] with [episode] where an automatic match would place it; null when its episodes can't be reached. */
    suspend fun openCandidate(candidate: C, episode: IntroTimesEpisode): IntroTimesMatchChoice<M>?
}

/**
 * A series or film opened for picking which of its episodes is the playing one.
 *
 * @property film a film has no episode to pick: [seasons] is empty and its times are asked for with no pick.
 * @property seasons the seasons the episode is picked among. A series that isn't a [film] and lists none has its
 * season and episode typed as numbers.
 * @property located where the playing episode is by the match the choice was opened with; null when that is nowhere
 * in [seasons].
 */
internal interface IntroTimesMatchChoice<M> {
    val title: String
    val year: Int?
    val film: Boolean
    val seasons: List<IntroTimesMatchSeason>
    val located: IntroTimesMatchPick?

    /** The service's times for [pick] on a cut [lengthMs] long, empty when it has none; null when it's unreachable. */
    suspend fun times(pick: IntroTimesMatchPick?, lengthMs: Long): List<PlaybackMarker>?

    /** The match that makes [pick] the playing episode, and counts the scope's other episodes from it. */
    fun match(pick: IntroTimesMatchPick?): M
}

/**
 * @property number the season's number, which names it among the others.
 * @property title the season's own name where the service keeps it as a series of its own, and [year] its year.
 * @property episodes null when the service doesn't say how many it has, so the episode's number is typed.
 */
internal data class IntroTimesMatchSeason(
    val number: Int,
    val title: String?,
    val year: Int?,
    val episodes: List<IntroTimesMatchEpisode>?,
)

/** [name] and [aired] are null where the service's sources don't have them. */
internal data class IntroTimesMatchEpisode(val number: Int, val name: String?, val aired: LocalDate?)

/** An episode by its season's [IntroTimesMatchSeason.number] and its own number there. */
internal data class IntroTimesMatchPick(val season: Int, val episode: Int)
