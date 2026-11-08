package eu.kanade.tachiyomi.ui.video.player.introtimes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.entry.interactions.anime.introtimes.IntroTimesEpisode
import mihon.entry.interactions.anime.introtimes.IntroTimesEpisodeReader
import mihon.entry.interactions.anime.introtimes.IntroTimesResult
import mihon.entry.interactions.anime.introtimes.IntroTimesService
import mihon.entry.interactions.anime.introtimes.OnlineMarker
import mihon.entry.interactions.anime.introtimes.aniskip.AniListSeries
import mihon.entry.interactions.anime.introtimes.aniskip.AniSkipLookup
import mihon.entry.interactions.anime.introtimes.mergeIntroTimes
import mihon.entry.interactions.anime.introtimes.theintrodb.TheIntroDbLookup
import tachiyomi.domain.entry.model.IntroTimesSeries
import tachiyomi.domain.entry.model.TheIntroDbMedia

/** Where one service's lookup for the playing episode stands. */
internal sealed interface VideoPlayerServiceTimes<out M> {
    data object Looking : VideoPlayerServiceTimes<Nothing>
    data class Done<M>(val result: IntroTimesResult<M>) : VideoPlayerServiceTimes<M>
}

/**
 * The online intro and credits times of the playing episode. [episode] is null until the episode's scope is read, and a
 * service turned off in the settings is null.
 */
internal data class VideoPlayerIntroTimesState(
    val chapterId: Long,
    val episode: IntroTimesEpisode?,
    val aniSkip: VideoPlayerServiceTimes<IntroTimesSeries>?,
    val theIntroDb: VideoPlayerServiceTimes<TheIntroDbMedia>?,
) {
    /** The markers the player goes by, each service's times arriving as it answers. */
    val markers: List<OnlineMarker> = mergeIntroTimes(
        mapOf(IntroTimesService.ANISKIP to aniSkip.found(), IntroTimesService.THEINTRODB to theIntroDb.found()),
    )
}

private fun VideoPlayerServiceTimes<*>?.found() =
    ((this as? VideoPlayerServiceTimes.Done)?.result as? IntroTimesResult.Found)?.markers.orEmpty()

/**
 * Looks up the playing episode's times with each service turned on, side by side, once its length is known, and a
 * service's again after its match is changed.
 */
internal class VideoPlayerIntroTimes(
    private val scope: CoroutineScope,
    private val episodes: IntroTimesEpisodeReader,
    private val aniSkip: AniSkipLookup,
    private val theIntroDb: TheIntroDbLookup,
) {
    private val mutableState = MutableStateFlow<VideoPlayerIntroTimesState?>(null)
    val state: StateFlow<VideoPlayerIntroTimesState?> = mutableState.asStateFlow()

    private var request: Request? = null
    private var episodeJob: Job? = null
    private val serviceJobs = mutableMapOf<IntroTimesService, Job>()

    /** [services] are the ones turned on; none clears the times. */
    fun request(chapterId: Long, lengthMs: Long, services: Set<IntroTimesService>) {
        val next = Request(chapterId, lengthMs, services)
        if (next == request) return
        load(next)
    }

    fun clear() {
        cancelAll()
        request = null
        mutableState.value = null
    }

    fun retry(service: IntroTimesService) {
        val current = request ?: return
        val episode = mutableState.value?.episode ?: return
        lookUp(service, current, episode)
    }

    /** Keeps [series] for the playing episode's scope on AniSkip, or None when null, and asks AniSkip again. */
    fun pickAniSkipSeries(series: AniListSeries?) {
        val current = request ?: return
        val episode = mutableState.value?.episode ?: return
        launchLookup(IntroTimesService.ANISKIP, current, { copy(aniSkip = it) }) {
            aniSkip.pick(episode.scope, series)
            aniSkip.find(episode, current.lengthMs)
        }
    }

    /** Keeps [media] for the playing episode's scope on TheIntroDB, or None when null, and asks TheIntroDB again. */
    fun pickTheIntroDbMedia(media: TheIntroDbMedia?) {
        val current = request ?: return
        val episode = mutableState.value?.episode ?: return
        launchLookup(IntroTimesService.THEINTRODB, current, { copy(theIntroDb = it) }) {
            theIntroDb.pick(episode.scope, media)
            theIntroDb.find(episode, current.lengthMs)
        }
    }

    private fun load(next: Request) {
        cancelAll()
        request = next
        if (next.services.isEmpty()) {
            mutableState.value = null
            return
        }
        val known = mutableState.value?.takeIf { it.chapterId == next.chapterId }?.episode
        mutableState.value = VideoPlayerIntroTimesState(
            chapterId = next.chapterId,
            episode = known,
            aniSkip = VideoPlayerServiceTimes.Looking.takeIf { IntroTimesService.ANISKIP in next.services },
            theIntroDb = VideoPlayerServiceTimes.Looking.takeIf { IntroTimesService.THEINTRODB in next.services },
        )
        episodeJob = scope.launch {
            val episode = known ?: episodes.episode(next.chapterId) ?: run {
                mutableState.value = null
                return@launch
            }
            mutableState.update { it?.copy(episode = episode) }
            next.services.forEach { lookUp(it, next, episode) }
        }
    }

    private fun lookUp(service: IntroTimesService, current: Request, episode: IntroTimesEpisode) {
        when (service) {
            IntroTimesService.ANISKIP -> launchLookup(service, current, { copy(aniSkip = it) }) {
                aniSkip.find(episode, current.lengthMs)
            }
            IntroTimesService.THEINTRODB -> launchLookup(service, current, { copy(theIntroDb = it) }) {
                theIntroDb.find(episode, current.lengthMs)
            }
        }
    }

    private fun <M> launchLookup(
        service: IntroTimesService,
        current: Request,
        withTimes: VideoPlayerIntroTimesState.(VideoPlayerServiceTimes<M>) -> VideoPlayerIntroTimesState,
        find: suspend () -> IntroTimesResult<M>,
    ) {
        serviceJobs.remove(service)?.cancel()
        val update: (VideoPlayerServiceTimes<M>) -> Unit = { times ->
            // An answer that comes in as the next episode starts belongs to the one before.
            mutableState.update { state ->
                state?.takeIf { it.chapterId == current.chapterId }?.withTimes(times)
                    ?: state
            }
        }
        update(VideoPlayerServiceTimes.Looking)
        serviceJobs[service] = scope.launch { update(VideoPlayerServiceTimes.Done(find())) }
    }

    private fun cancelAll() {
        episodeJob?.cancel()
        serviceJobs.values.forEach(Job::cancel)
        serviceJobs.clear()
    }

    private data class Request(val chapterId: Long, val lengthMs: Long, val services: Set<IntroTimesService>)
}
