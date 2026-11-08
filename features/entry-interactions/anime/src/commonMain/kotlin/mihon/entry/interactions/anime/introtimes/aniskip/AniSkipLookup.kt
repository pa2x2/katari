package mihon.entry.interactions.anime.introtimes.aniskip

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import mihon.entry.interactions.anime.introtimes.IntroTimesEpisode
import mihon.entry.interactions.anime.introtimes.IntroTimesResult
import mihon.entry.interactions.anime.introtimes.IntroTimesScope
import mihon.entry.interactions.anime.introtimes.askOnline
import tachiyomi.domain.entry.model.IntroTimesMatch
import tachiyomi.domain.entry.model.IntroTimesSeries
import tachiyomi.domain.entry.repository.IntroTimesMatchRepository
import kotlin.time.Duration.Companion.days

/**
 * Finds AniSkip's intro and credits times for episodes whose source marks none: the episode's series is matched to an
 * AniList anime once and kept, and AniSkip is asked for the episode under that anime's MyAnimeList id.
 */
@Inject
@SingleIn(AppScope::class)
internal class AniSkipLookup(
    private val matchRepository: IntroTimesMatchRepository,
    private val seriesSearch: AniListSeriesSearch,
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
        val key = AnswerKey(series.malId, number, lengthSeconds = (lengthMs + MS_PER_SECOND / 2) / MS_PER_SECOND)
        val markers = synchronized(answers) { answers[key] }
            ?: askOnline("AniSkip") { aniSkip.fetch(key.malId, key.episode, key.lengthSeconds) }
                ?.also { synchronized(answers) { answers[key] = it } }
            ?: return IntroTimesResult.Unreachable
        return IntroTimesResult.Found(series, markers)
    }

    suspend fun search(query: String): List<AniListSeries> = seriesSearch.search(query)

    /** Keeps [series] for [scope] in place of any found by title; null is None, which looks up nothing. */
    suspend fun pick(scope: IntroTimesScope, series: AniListSeries?) {
        matchRepository.upsert(
            IntroTimesMatch(
                entryId = scope.entryId,
                groupKey = scope.groupKey,
                series = series?.toIntroTimesSeries(),
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
        val candidates = askOnline("AniList") { seriesSearch.search(scope.title) } ?: return SeriesLookup.Unreachable
        val series = bestSeries(candidates, scope)?.toIntroTimesSeries()
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

    private sealed interface SeriesLookup {
        data class Found(val series: IntroTimesSeries) : SeriesLookup
        data object NotFound : SeriesLookup
        data object TurnedOff : SeriesLookup
        data object Unreachable : SeriesLookup
    }

    private data class AnswerKey(val malId: Long, val episode: Int, val lengthSeconds: Long)

    private companion object {
        const val MAX_ANSWERS = 200
        const val MS_PER_SECOND = 1000L
        val SEARCH_AGAIN_AFTER_MS = 7.days.inWholeMilliseconds
    }
}

private fun AniListSeries.toIntroTimesSeries() = IntroTimesSeries(
    anilistId = anilistId,
    malId = malId,
    title = title,
    year = year,
)
