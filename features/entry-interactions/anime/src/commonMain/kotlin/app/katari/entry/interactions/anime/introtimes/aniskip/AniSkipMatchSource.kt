package app.katari.entry.interactions.anime.introtimes.aniskip

import app.katari.entry.interactions.anime.introtimes.IntroTimesEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchChoice
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchPick
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchSeason
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchSource
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import tachiyomi.domain.entry.model.IntroTimesSeries

/**
 * AniSkip's match picked by hand: AniList's series, each opened into the seasons of its show, which AniList keeps as
 * series of their own, so the playing episode can be pointed at in any of them.
 */
internal class AniSkipMatchSource(private val lookup: AniSkipLookup) :
    IntroTimesMatchSource<AniListSeries, IntroTimesSeries> {

    override suspend fun search(query: String): List<AniListSeries> = lookup.search(query)

    override suspend fun openMatch(match: IntroTimesSeries, episode: IntroTimesEpisode) = choice(match, episode)

    override suspend fun openCandidate(
        candidate: AniListSeries,
        episode: IntroTimesEpisode,
    ): IntroTimesMatchChoice<IntroTimesSeries>? {
        val seasons = lookup.seasons(candidate) ?: return null
        return choice(candidate.toIntroTimesSeries(firstEpisodeIn(seasons, episode.scope) ?: 1), episode)
    }

    private suspend fun choice(series: IntroTimesSeries, episode: IntroTimesEpisode): AniSkipMatchChoice {
        // An episode without a number of its own is placed as the scope's first.
        val number = episode.number ?: 1
        val place = lookup.place(series, series.firstEpisode + number - 1, withSeasons = true)
        // Without AniList's answer the matched series is the only season known.
        val seasons = place.seasons
            .map { AniSkipMatchSeason(it.toIntroTimesSeries(), it.episodes ?: it.airedEpisodes, it.episodeNames) }
            .ifEmpty { listOf(AniSkipMatchSeason(series.copy(firstEpisode = 1), series.episodes, emptyMap())) }
        val located = place.episode?.let { at ->
            seasons.indexOfFirst { it.series.malId == at.malId }.takeIf { it >= 0 }
                ?.let { IntroTimesMatchPick(season = it + 1, episode = at.episode) }
        }
        return AniSkipMatchChoice(lookup, series, number, seasons, located)
    }
}

/** A season of a show: its [series], how many [episodes] are out, and the [names] AniList has for them. */
internal data class AniSkipMatchSeason(val series: IntroTimesSeries, val episodes: Int?, val names: Map<Int, String>)

/**
 * [series] opened among the seasons of its show, [listed] first to last. [number] is the playing episode's place in
 * its scope.
 */
internal class AniSkipMatchChoice(
    private val lookup: AniSkipLookup,
    private val series: IntroTimesSeries,
    private val number: Int,
    private val listed: List<AniSkipMatchSeason>,
    override val located: IntroTimesMatchPick?,
) : IntroTimesMatchChoice<IntroTimesSeries> {
    override val title get() = series.title
    override val year get() = series.year
    override val film get() = false

    override val seasons: List<IntroTimesMatchSeason> = listed.mapIndexed { index, season ->
        // AniList is a few hours behind an episode that just aired, so the playing one is listed past its count.
        val playing = located?.takeIf { it.season == index + 1 }?.episode ?: 0
        IntroTimesMatchSeason(
            number = index + 1,
            title = season.series.title,
            year = season.series.year,
            episodes = season.episodes?.let { count ->
                val last = if (season.series.episodes == null) maxOf(count, playing) else count
                (1..last).map { IntroTimesMatchEpisode(number = it, name = season.names[it], aired = null) }
            },
        )
    }

    override suspend fun times(pick: IntroTimesMatchPick?, lengthMs: Long): List<PlaybackMarker>? {
        val season = pick?.let { listed.getOrNull(it.season - 1) } ?: return emptyList()
        return lookup.times(AniSkipEpisode(season.series.malId, pick.episode), lengthMs)
    }

    override fun match(pick: IntroTimesMatchPick?): IntroTimesSeries {
        val season = pick?.takeIf { it != located }?.let { listed.getOrNull(it.season - 1) } ?: return series
        return season.series.copy(firstEpisode = pick.episode - (number - 1))
    }
}
