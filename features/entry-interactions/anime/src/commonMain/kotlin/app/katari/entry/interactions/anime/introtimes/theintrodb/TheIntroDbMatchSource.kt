package app.katari.entry.interactions.anime.introtimes.theintrodb

import app.katari.entry.interactions.anime.introtimes.IntroTimesEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchChoice
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchPick
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchSeason
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchSource
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import tachiyomi.domain.entry.model.TheIntroDbMedia
import tachiyomi.domain.entry.model.TheIntroDbPlacement

/**
 * TheIntroDB's match picked by hand: Wikidata's and TVmaze's series and films, a series opened into the episodes
 * TVmaze lists for it so the playing one can be pointed at.
 */
internal class TheIntroDbMatchSource(private val lookup: TheIntroDbLookup) :
    IntroTimesMatchSource<TheIntroDbCandidate, TheIntroDbMedia> {

    override suspend fun search(query: String): List<TheIntroDbCandidate> = lookup.search(query)

    override suspend fun openMatch(match: TheIntroDbMedia, episode: IntroTimesEpisode): TheIntroDbMatchChoice? {
        if (match.placement == null) return TheIntroDbMatchChoice(lookup, match, emptyList(), PLAYING_FIRST)
        val aired = lookup.aired(match.tvdbId, match.imdbId) ?: return null
        return TheIntroDbMatchChoice(lookup, match, aired, episode.number ?: PLAYING_FIRST)
    }

    override suspend fun openCandidate(
        candidate: TheIntroDbCandidate,
        episode: IntroTimesEpisode,
    ): TheIntroDbMatchChoice? {
        if (candidate.film) return TheIntroDbMatchChoice(lookup, candidate.toMedia(null), emptyList(), PLAYING_FIRST)
        val aired = lookup.aired(candidate.tvdbId, candidate.imdbId) ?: return null
        val media = candidate.toMedia(guessPlacement(episode.scope, aired))
        return TheIntroDbMatchChoice(lookup, media, aired, episode.number ?: PLAYING_FIRST)
    }

    private companion object {
        // An episode without a number of its own is placed as the scope's first.
        const val PLAYING_FIRST = 1
    }
}

/**
 * [media] opened among [aired], TVmaze's list of it, which is empty for a film and for a series TVmaze doesn't list.
 * [number] is the playing episode's place in its scope. A series whose seasons TVmaze numbers by year is one run of
 * episodes numbered through, as TheIntroDB is asked for it.
 */
internal class TheIntroDbMatchChoice(
    private val lookup: TheIntroDbLookup,
    private val media: TheIntroDbMedia,
    private val aired: List<AiredEpisode>,
    private val number: Int,
) : IntroTimesMatchChoice<TheIntroDbMedia> {
    private val oneRun = aired.numbersSeasonsByYear

    override val title get() = media.title
    override val year get() = media.year
    override val film get() = media.placement == null

    override val seasons: List<IntroTimesMatchSeason> = when {
        oneRun -> listOf(
            IntroTimesMatchSeason(
                number = ONE_RUN_SEASON,
                title = null,
                year = null,
                episodes = aired.mapIndexed { index, episode ->
                    IntroTimesMatchEpisode(index + 1, episode.name, episode.aired)
                },
            ),
        )
        else -> aired.groupBy { it.season }.map { (season, episodes) ->
            IntroTimesMatchSeason(
                number = season,
                title = null,
                year = null,
                episodes = episodes.map { IntroTimesMatchEpisode(it.number, it.name, it.aired) },
            )
        }
    }

    override val located: IntroTimesMatchPick? = media.placement?.let { placement ->
        if (aired.isEmpty()) {
            val episode = placement.firstEpisode + number - 1
            return@let IntroTimesMatchPick(placement.season, episode).takeIf { episode >= 1 }
        }
        val index = airedIndex(placement, number, aired) ?: return@let null
        val episode = aired.getOrNull(index) ?: return@let null
        when {
            oneRun -> IntroTimesMatchPick(ONE_RUN_SEASON, index + 1)
            else -> IntroTimesMatchPick(episode.season, episode.number)
        }
    }

    override suspend fun times(pick: IntroTimesMatchPick?, lengthMs: Long): List<PlaybackMarker>? {
        if (film) return lookup.times(media, null, lengthMs)
        val placement = pick?.let { placement(it, playing = 1) } ?: return emptyList()
        return lookup.times(media, episodeAsks(placement, 1, aired), lengthMs)
    }

    override fun match(pick: IntroTimesMatchPick?): TheIntroDbMedia {
        val placement = pick?.takeIf { it != located }?.let { placement(it, number) } ?: return media
        return media.copy(placement = placement)
    }

    /** Where a scope starts whose [playing]-th episode is [pick]; null when [pick] isn't an episode of the series. */
    private fun placement(pick: IntroTimesMatchPick, playing: Int): TheIntroDbPlacement? {
        if (aired.isEmpty()) return TheIntroDbPlacement(pick.season, pick.episode - (playing - 1))
        val index = when {
            oneRun -> pick.episode - 1
            else -> aired.indexOfFirst { it.season == pick.season && it.number == pick.episode }
        }
        return if (index in aired.indices) placementAt(index, playing, aired) else null
    }

    private companion object {
        const val ONE_RUN_SEASON = 1
    }
}
