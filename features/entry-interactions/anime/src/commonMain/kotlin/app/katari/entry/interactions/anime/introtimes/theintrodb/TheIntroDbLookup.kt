package app.katari.entry.interactions.anime.introtimes.theintrodb

import app.katari.entry.interactions.anime.introtimes.IntroTimesEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesResult
import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import app.katari.entry.interactions.anime.introtimes.askOnline
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import kotlinx.coroutines.async
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
 * with where its episodes sit in the series. TheIntroDB is then asked by that series' or film's id.
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

    /** [lengthMs] is the episode's playing length, which TheIntroDB picks the release by. */
    suspend fun find(episode: IntroTimesEpisode, lengthMs: Long): IntroTimesResult<TheIntroDbMedia> {
        val media = when (val found = media(episode.scope)) {
            is MediaLookup.Found -> found.media
            MediaLookup.NotFound -> return IntroTimesResult.NoMatch(episode.scope.title)
            MediaLookup.TurnedOff -> return IntroTimesResult.TurnedOff
            MediaLookup.Unreachable -> return IntroTimesResult.Unreachable
        }
        val asked = media.placement?.let { placement ->
            val number = episode.number ?: return IntroTimesResult.Unnumbered(media)
            SeasonEpisode(placement.season, placement.firstEpisode + number - 1)
        }
        val key = AnswerKey(media.tvdbId, media.tmdbId, media.imdbId, asked, lengthMs / MS_PER_SECOND)
        val markers = synchronized(answers) { answers[key] }
            ?: askOnline("TheIntroDB") { times.fetch(media, asked, lengthMs) }
                ?.also { synchronized(answers) { answers[key] = it } }
            ?: return IntroTimesResult.Unreachable
        return IntroTimesResult.Found(media, markers)
    }

    /**
     * Wikidata's and TVmaze's series and films by [query], Wikidata's first and one both know listed once. Either
     * fills the list alone; it fails only when neither answers.
     */
    suspend fun search(query: String): List<TheIntroDbCandidate> = coroutineScope {
        val fromWikidata = async { answer { wikidata.search(query) } }
        val fromTvMaze = async { answer { tvMaze.search(query) } }
        val answered = listOf(fromWikidata.await(), fromTvMaze.await())
        if (answered.all { it.isFailure }) answered.first().getOrThrow()
        answered.mapNotNull { it.getOrNull() }.flatten().fold(emptyList()) { listed, candidate ->
            if (listed.any(candidate::isSameAs)) listed else listed + candidate
        }
    }

    /** The aired episodes of [candidate], a series; null when TVmaze couldn't be reached. */
    suspend fun episodes(candidate: TheIntroDbCandidate): List<AiredEpisode>? =
        askOnline("TVmaze") { tvMaze.episodes(candidate.tvdbId, candidate.imdbId) }

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
        val fromWikidata = askOnline("Wikidata") { wikidata.search(scope.title) }
        val candidate = fromWikidata?.let { bestMedia(it, scope) } ?: run {
            val fromTvMaze = askOnline("TVmaze") { tvMaze.search(scope.title) }
            val best = fromTvMaze?.let { bestMedia(it, scope) }
            // Nothing found while a service was down isn't kept, so the next episode searches again.
            if (best == null && (fromWikidata == null || fromTvMaze == null)) return MediaLookup.Unreachable
            best
        }
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

    private suspend fun placement(scope: IntroTimesScope, series: TheIntroDbCandidate): TheIntroDbPlacement {
        // Only an aired date finds where the scope starts, and only a dated first episode can be compared with one.
        val aired = if (scope.firstAired != null) episodes(series).orEmpty() else emptyList()
        return guessPlacement(scope, aired)
    }

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
        val episode: SeasonEpisode?,
        val lengthSeconds: Long,
    )

    private companion object {
        const val MAX_ANSWERS = 200
        const val MS_PER_SECOND = 1000L
        val SEARCH_AGAIN_AFTER_MS = 7.days.inWholeMilliseconds
    }
}

private inline fun <T> answer(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
