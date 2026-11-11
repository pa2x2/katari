package app.katari.entry.interactions.anime.introtimes.aniskip

import app.katari.entry.interactions.anime.introtimes.IntroTimesAsked
import app.katari.entry.interactions.anime.introtimes.IntroTimesEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesResult
import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import app.katari.entry.interactions.anime.introtimes.askOnline
import app.katari.entry.interactions.anime.introtimes.seasonNumber
import app.katari.entry.interactions.anime.introtimes.titleSeason
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import tachiyomi.domain.entry.model.IntroTimesMatch
import tachiyomi.domain.entry.model.IntroTimesSeries
import tachiyomi.domain.entry.repository.IntroTimesMatchRepository
import kotlin.time.Duration.Companion.days

/**
 * Finds AniSkip's intro and credits times for episodes whose source marks none: the episode's series is matched to an
 * AniList anime once and kept, by its title and the season its group or title names, and AniSkip is asked for the
 * episode under that anime's MyAnimeList id. An episode numbered past the anime's end is counted on into the show's
 * next seasons.
 */
@Inject
@SingleIn(AppScope::class)
internal class AniSkipLookup(
    private val matchRepository: IntroTimesMatchRepository,
    private val seriesSearch: AniListSeriesSearch,
    private val seasons: AniListSeasons,
    private val aniSkip: AniSkipTimes,
) {
    // AniSkip's answers for this run of the app, including empty ones, so replaying an episode doesn't ask again.
    // A new run asks again, as times for a recent episode are submitted in the days after it airs.
    private val answers = object : LinkedHashMap<AnswerKey, List<PlaybackMarker>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<AnswerKey, List<PlaybackMarker>>) =
            size > MAX_ANSWERS
    }

    /** [lengthMs] is the episode's playing length, which AniSkip needs to tell cuts of the episode apart. */
    suspend fun find(episode: IntroTimesEpisode, lengthMs: Long): IntroTimesResult<IntroTimesSeries> {
        val series = when (val found = series(episode.scope)) {
            is SeriesLookup.Found -> found.series
            SeriesLookup.NotFound -> return IntroTimesResult.NoMatch(episode.scope.title)
            SeriesLookup.TurnedOff -> return IntroTimesResult.TurnedOff
            SeriesLookup.Unreachable -> return IntroTimesResult.Unreachable
        }
        val number = episode.number ?: return IntroTimesResult.Unnumbered(series)
        val counted = IntroTimesAsked(season = null, episode = series.firstEpisode + number - 1)
        val place = place(series, counted.episode)
        val target = place.episode ?: return IntroTimesResult.NoSuchEpisode(series, counted)
        val markers = times(target, lengthMs) ?: return IntroTimesResult.Unreachable
        // An episode counted into another season is named as that season has it.
        val season = place.seasons.firstOrNull { it.malId == target.malId }?.takeIf { it.malId != series.malId }
        val asked = season?.let { IntroTimesAsked(season = null, episode = target.episode, series = it.title) }
        return IntroTimesResult.Found(series, asked ?: counted, markers)
    }

    /** AniSkip's times for [episode] on a cut [lengthMs] long, empty when it has none; null when it's unreachable. */
    suspend fun times(episode: AniSkipEpisode, lengthMs: Long): List<PlaybackMarker>? {
        val key = AnswerKey(episode, lengthSeconds = (lengthMs + MS_PER_SECOND / 2) / MS_PER_SECOND)
        return synchronized(answers) { answers[key] }
            ?: askOnline("AniSkip") { aniSkip.fetch(episode.malId, episode.episode, key.lengthSeconds) }
                ?.also { synchronized(answers) { answers[key] = it } }
    }

    /**
     * Where the [episode]-th episode counted from the start of [series] is, among the seasons of its show. The
     * seasons are only looked up for an episode [series] doesn't have itself, or when [withSeasons] asks for them.
     */
    suspend fun place(series: IntroTimesSeries, episode: Int, withSeasons: Boolean = false): AniSkipPlace {
        val count = series.episodes
        val within = episode >= 1 && (count == null || episode <= count)
        val own = AniSkipEpisode(series.malId, episode)
        if (within && !withSeasons) return AniSkipPlace(emptyList(), own)
        // Without its seasons, an episode outside the series is asked for as it is numbered.
        val seasons = askOnline("AniList") { seasons.of(series.anilistId) }.orEmpty()
        val index = seasons.indexOfFirst { it.anilistId == series.anilistId }
        if (within || index < 0) return AniSkipPlace(seasons, own.takeIf { episode >= 1 })
        val numberings = when (index) {
            0 -> askOnline("AniSkip") { aniSkip.numberings(series.malId) }.orEmpty()
            else -> emptyList()
        }
        return AniSkipPlace(seasons, episodeInSeasons(seasons, index, episode, numberings))
    }

    /** AniList's series by [query], each show's seasons together in the order they aired. */
    suspend fun search(query: String): List<AniListSeries> = seasonsTogether(seriesSearch.search(query))

    /** The seasons [series] is one of, first to last; null when AniList can't be reached. */
    suspend fun seasons(series: AniListSeries): List<AniListSeries>? = askOnline("AniList") { seasons.of(series) }

    /** Keeps [series] for [scope] in place of any found by title; null is None, which looks up nothing. */
    suspend fun pick(scope: IntroTimesScope, series: IntroTimesSeries?) {
        matchRepository.upsert(
            IntroTimesMatch(
                entryId = scope.entryId,
                groupKey = scope.groupKey,
                series = series,
                pickedByUser = true,
                checkedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun series(scope: IntroTimesScope): SeriesLookup {
        val stored = matchRepository.get(scope.entryId, scope.groupKey)
        stored?.series?.let { return SeriesLookup.Found(it) }
        if (stored?.pickedByUser == true) return SeriesLookup.TurnedOff
        val now = System.currentTimeMillis()
        // A series AniList didn't have may be added later, so it is searched for again after a while.
        if (stored != null && now - stored.checkedAt < SEARCH_AGAIN_AFTER_MS) return SeriesLookup.NotFound
        val series = (askOnline("AniList") { Matched(match(scope)) } ?: return SeriesLookup.Unreachable).series
        matchRepository.upsert(
            IntroTimesMatch(
                entryId = scope.entryId,
                groupKey = scope.groupKey,
                series = series,
                pickedByUser = false,
                checkedAt = now,
            ),
        )
        return series?.let(SeriesLookup::Found) ?: SeriesLookup.NotFound
    }

    /**
     * The series [scope]'s episodes belong to. Its whole title is tried first, as AniList keeps each season under a
     * name of its own; a season its group names, or its title in words AniList doesn't use, is then looked for among
     * the seasons of the show.
     */
    private suspend fun match(scope: IntroTimesScope): IntroTimesSeries? {
        val candidates = seriesSearch.search(scope.title)
        val groupSeason = seasonNumber(scope.seasonName)
        if (groupSeason != null && groupSeason > 1) {
            return firstSeason(candidates, scope.title, scope)?.let { season(it, groupSeason, scope) }
        }
        bestSeries(candidates, scope)?.let { return it.toIntroTimesSeries() }
        if (groupSeason != null) return null
        val named = titleSeason(scope.title) ?: return null
        val first = firstSeason(seriesSearch.search(named.series), named.series, scope) ?: return null
        return season(first, named.season, scope)
    }

    private suspend fun season(first: AniListSeries, season: Int, scope: IntroTimesScope): IntroTimesSeries? {
        val seasons = seasons.of(first)
        seasonSeries(seasons, season, scope)?.let { return it.toIntroTimesSeries() }
        return firstEpisodeIn(seasons, scope)?.let { first.toIntroTimesSeries(firstEpisode = it) }
    }

    private sealed interface SeriesLookup {
        data class Found(val series: IntroTimesSeries) : SeriesLookup
        data object NotFound : SeriesLookup
        data object TurnedOff : SeriesLookup
        data object Unreachable : SeriesLookup
    }

    // Tells a search that found nothing from one that failed, which answers null.
    private class Matched(val series: IntroTimesSeries?)

    private data class AnswerKey(val episode: AniSkipEpisode, val lengthSeconds: Long)

    private companion object {
        const val MAX_ANSWERS = 200
        const val MS_PER_SECOND = 1000L
        val SEARCH_AGAIN_AFTER_MS = 7.days.inWholeMilliseconds
    }
}

/**
 * Where an episode is among a show's [seasons]; they are empty when they weren't looked up or AniList couldn't be
 * reached. [episode] is null when the show has no such episode.
 */
internal data class AniSkipPlace(val seasons: List<AniListSeries>, val episode: AniSkipEpisode?)

/** [firstEpisode] is the number the matched episodes' first has in the series. */
internal fun AniListSeries.toIntroTimesSeries(firstEpisode: Int = 1) = IntroTimesSeries(
    anilistId = anilistId,
    malId = malId,
    title = title,
    year = year,
    episodes = episodes,
    firstEpisode = firstEpisode,
)
