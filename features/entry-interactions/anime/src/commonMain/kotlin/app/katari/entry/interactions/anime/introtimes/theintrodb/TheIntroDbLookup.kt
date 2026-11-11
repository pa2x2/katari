package app.katari.entry.interactions.anime.introtimes.theintrodb

import app.katari.entry.interactions.anime.introtimes.IntroTimesAsked
import app.katari.entry.interactions.anime.introtimes.IntroTimesEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesResult
import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import app.katari.entry.interactions.anime.introtimes.askOnline
import app.katari.entry.interactions.anime.introtimes.titleSeason
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import tachiyomi.domain.entry.model.TheIntroDbMatch
import tachiyomi.domain.entry.model.TheIntroDbMedia
import tachiyomi.domain.entry.model.TheIntroDbPlacement
import tachiyomi.domain.entry.repository.TheIntroDbMatchRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.days

/**
 * Finds TheIntroDB's intro and credits times for episodes whose source marks none, on its own match: the episode's
 * scope is matched once to a series or film found by title on Wikidata, or on TVmaze when Wikidata has none, and kept
 * with where its episodes sit in the series. TheIntroDB is then asked by that series' or film's id, for the episode
 * TVmaze's list of the series puts the scope's at, so one numbered past a season's end is found in the next.
 */
@Inject
@SingleIn(AppScope::class)
internal class TheIntroDbLookup(
    private val matchRepository: TheIntroDbMatchRepository,
    private val wikidata: WikidataMediaSearch,
    private val tvMaze: TvMazeShows,
    private val times: TheIntroDbTimes,
) {
    // TheIntroDB's answers for this run of the app, as AniSkip's are kept.
    private val answers = object : LinkedHashMap<AnswerKey, List<PlaybackMarker>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<AnswerKey, List<PlaybackMarker>>) =
            size > MAX_ANSWERS
    }

    // TVmaze's lists for this run, so stepping through a series' episodes asks for its list once.
    private val airedLists = object : LinkedHashMap<MediaKey, List<AiredEpisode>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<MediaKey, List<AiredEpisode>>) =
            size > MAX_AIRED_LISTS
    }

    /** [lengthMs] is the episode's playing length, which TheIntroDB picks the release by. */
    suspend fun find(episode: IntroTimesEpisode, lengthMs: Long): IntroTimesResult<TheIntroDbMedia> {
        val media = when (val found = media(episode.scope)) {
            is MediaLookup.Found -> found.media
            MediaLookup.NotFound -> return IntroTimesResult.NoMatch(episode.scope.title)
            MediaLookup.TurnedOff -> return IntroTimesResult.TurnedOff
            MediaLookup.Unreachable -> return IntroTimesResult.Unreachable
        }
        val placement = media.placement
            ?: return times(media, null, lengthMs)?.let { IntroTimesResult.Found(media, null, it) }
                ?: IntroTimesResult.Unreachable
        val number = episode.number ?: return IntroTimesResult.Unnumbered(media)
        // Without TVmaze's list the episode is asked for as the placement numbers it.
        val asks = episodeAsks(placement, number, aired(media.tvdbId, media.imdbId).orEmpty())
        val first = asks.firstOrNull() ?: return IntroTimesResult.NoSuchEpisode(
            media,
            IntroTimesAsked(placement.season, placement.firstEpisode + number - 1),
        )
        val markers = times(media, asks, lengthMs) ?: return IntroTimesResult.Unreachable
        return IntroTimesResult.Found(media, first.asked, markers)
    }

    /** The first times TheIntroDB has under one of [asks], the ways to ask for one episode of [media]. */
    suspend fun times(media: TheIntroDbMedia, asks: List<TheIntroDbEpisode>, lengthMs: Long): List<PlaybackMarker>? {
        asks.forEach { ask ->
            val markers = times(media, ask, lengthMs) ?: return null
            if (markers.isNotEmpty()) return markers
        }
        return emptyList()
    }

    /**
     * TheIntroDB's times for [episode] of [media], or for the film when null, on a cut [lengthMs] long; empty when it
     * has none, null when it can't be reached.
     */
    suspend fun times(media: TheIntroDbMedia, episode: TheIntroDbEpisode?, lengthMs: Long): List<PlaybackMarker>? {
        val key = AnswerKey(media.tvdbId, media.tmdbId, media.imdbId, episode, lengthMs / MS_PER_SECOND)
        return synchronized(answers) { answers[key] }
            ?: askOnline("TheIntroDB") { times.fetch(media, episode, lengthMs) }
                ?.also { synchronized(answers) { answers[key] = it } }
    }

    /**
     * Wikidata's and TVmaze's series and films by [query], Wikidata's first and one both know listed once. Either
     * fills the list alone; it fails only when neither answers. A series Wikidata lists without a cover takes TVmaze's.
     */
    suspend fun search(query: String): List<TheIntroDbCandidate> = coroutineScope {
        val fromWikidata = async { answer { wikidata.search(query) } }
        val fromTvMaze = async { answer { tvMaze.search(query) } }
        val answered = listOf(fromWikidata.await(), fromTvMaze.await())
        if (answered.all { it.isFailure }) answered.first().getOrThrow()
        val found = mutableListOf<TheIntroDbCandidate>()
        answered.mapNotNull { it.getOrNull() }.flatten().forEach { candidate ->
            val same = found.indexOfFirst(candidate::isSameAs)
            when {
                same < 0 -> found += candidate
                found[same].coverUrl == null -> found[same] = found[same].copy(coverUrl = candidate.coverUrl)
            }
        }
        found.map { candidate ->
            async {
                if (candidate.film || candidate.coverUrl != null) return@async candidate
                candidate.copy(coverUrl = askOnline("TVmaze") { tvMaze.cover(candidate.tvdbId, candidate.imdbId) })
            }
        }.awaitAll()
    }

    /**
     * The aired episodes of the series known by [tvdbId] or [imdbId], empty when TVmaze doesn't list it; null when
     * TVmaze couldn't be reached.
     */
    suspend fun aired(tvdbId: Long?, imdbId: String?): List<AiredEpisode>? {
        val key = MediaKey(tvdbId, imdbId)
        return synchronized(airedLists) { airedLists[key] }
            ?: askOnline("TVmaze") { tvMaze.episodes(tvdbId, imdbId) }
                ?.also { synchronized(airedLists) { airedLists[key] = it } }
    }

    /** Keeps [media] for [scope] in place of any found by title; null is None, which looks up nothing. */
    suspend fun pick(scope: IntroTimesScope, media: TheIntroDbMedia?) {
        matchRepository.upsert(
            TheIntroDbMatch(
                entryId = scope.entryId,
                groupKey = scope.groupKey,
                media = media,
                pickedByUser = true,
                checkedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun media(scope: IntroTimesScope): MediaLookup {
        val stored = matchRepository.get(scope.entryId, scope.groupKey)
        stored?.media?.let { return MediaLookup.Found(it) }
        if (stored?.pickedByUser == true) return MediaLookup.TurnedOff
        val now = System.currentTimeMillis()
        // A series Wikidata and TVmaze didn't have may be added later, so it is searched for again after a while.
        if (stored != null && now - stored.checkedAt < SEARCH_AGAIN_AFTER_MS) return MediaLookup.NotFound
        // The whole title first; one that names a season is then looked for as the series, which both keep whole.
        val titles = listOfNotNull(scope.title, titleSeason(scope.title)?.series)
        var unreached = false
        val candidate = titles.firstNotNullOfOrNull { title ->
            val fromWikidata = askOnline("Wikidata") { wikidata.search(title) }
            fromWikidata?.let { bestMedia(it, scope, title) } ?: run {
                val fromTvMaze = askOnline("TVmaze") { tvMaze.search(title) }
                unreached = unreached || fromWikidata == null || fromTvMaze == null
                fromTvMaze?.let { bestMedia(it, scope, title) }
            }
        }
        // Nothing found while a service was down isn't kept, so the next episode searches again.
        if (candidate == null && unreached) return MediaLookup.Unreachable
        val media = candidate?.toMedia(placement = if (candidate.film) null else placement(scope, candidate))
        matchRepository.upsert(
            TheIntroDbMatch(
                entryId = scope.entryId,
                groupKey = scope.groupKey,
                media = media,
                pickedByUser = false,
                checkedAt = now,
            ),
        )
        return media?.let(MediaLookup::Found) ?: MediaLookup.NotFound
    }

    private suspend fun placement(scope: IntroTimesScope, series: TheIntroDbCandidate): TheIntroDbPlacement =
        guessPlacement(scope, aired(series.tvdbId, series.imdbId).orEmpty())

    private sealed interface MediaLookup {
        data class Found(val media: TheIntroDbMedia) : MediaLookup
        data object NotFound : MediaLookup
        data object TurnedOff : MediaLookup
        data object Unreachable : MediaLookup
    }

    private data class AnswerKey(
        val tvdbId: Long?,
        val tmdbId: Long?,
        val imdbId: String?,
        val episode: TheIntroDbEpisode?,
        val lengthSeconds: Long,
    )

    private data class MediaKey(val tvdbId: Long?, val imdbId: String?)

    private companion object {
        const val MAX_ANSWERS = 200
        const val MAX_AIRED_LISTS = 20
        const val MS_PER_SECOND = 1000L
        val SEARCH_AGAIN_AFTER_MS = 7.days.inWholeMilliseconds
    }
}

/** The episode as the settings name it: a running number has no season. */
internal val TheIntroDbEpisode.asked: IntroTimesAsked
    get() = when (this) {
        is TheIntroDbEpisode.InSeason -> IntroTimesAsked(season, episode)
        is TheIntroDbEpisode.Running -> IntroTimesAsked(season = null, episode = number)
    }

private inline fun <T> answer(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
