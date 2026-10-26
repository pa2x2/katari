package eu.kanade.tachiyomi.ui.video.player.introtimes

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mihon.entry.interactions.anime.introtimes.AniListSeries
import mihon.entry.interactions.anime.introtimes.IntroTimesEpisode
import mihon.entry.interactions.anime.introtimes.IntroTimesLookup
import mihon.entry.interactions.anime.introtimes.IntroTimesResult

/**
 * The online intro and credits times of the playing episode. [episode] is null until the episode's series scope is
 * read, and [result] is null while the times are being looked up.
 */
internal data class VideoPlayerIntroTimesState(
    val chapterId: Long,
    val episode: IntroTimesEpisode?,
    val result: IntroTimesResult?,
) {
    val markers: List<PlaybackMarker>
        get() = (result as? IntroTimesResult.Matched)?.markers.orEmpty()
}

/** Looks up the playing episode's times once its length is known, and again after its series is changed. */
internal class VideoPlayerIntroTimes(
    private val scope: CoroutineScope,
    private val lookup: IntroTimesLookup,
) {
    private val mutableState = MutableStateFlow<VideoPlayerIntroTimesState?>(null)
    val state: StateFlow<VideoPlayerIntroTimesState?> = mutableState.asStateFlow()

    private var request: Request? = null
    private var job: Job? = null

    fun request(chapterId: Long, lengthMs: Long) {
        val next = Request(chapterId, lengthMs)
        if (next == request) return
        load(next)
    }

    fun clear() {
        job?.cancel()
        request = null
        mutableState.value = null
    }

    fun retry() {
        request?.let(::load)
    }

    /** Keeps [series] for the playing episode's series, or None when null, and looks its times up again. */
    fun pick(series: AniListSeries?) {
        val current = request ?: return
        val episode = mutableState.value?.episode ?: return
        job?.cancel()
        mutableState.value = VideoPlayerIntroTimesState(current.chapterId, episode, result = null)
        job = scope.launch {
            lookup.pick(episode.scope, series)
            mutableState.value = VideoPlayerIntroTimesState(
                chapterId = current.chapterId,
                episode = episode,
                result = lookup.find(episode, current.lengthMs),
            )
        }
    }

    private fun load(next: Request) {
        request = next
        job?.cancel()
        val known = mutableState.value?.takeIf { it.chapterId == next.chapterId }?.episode
        mutableState.value = VideoPlayerIntroTimesState(next.chapterId, known, result = null)
        job = scope.launch {
            val episode = known ?: lookup.episode(next.chapterId) ?: run {
                mutableState.value = null
                return@launch
            }
            mutableState.value = VideoPlayerIntroTimesState(next.chapterId, episode, result = null)
            mutableState.value =
                VideoPlayerIntroTimesState(next.chapterId, episode, lookup.find(episode, next.lengthMs))
        }
    }

    private data class Request(val chapterId: Long, val lengthMs: Long)
}
