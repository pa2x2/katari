package eu.kanade.tachiyomi.ui.video.player.introtimes

import app.katari.entry.interactions.anime.introtimes.IntroTimesEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchChoice
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchPick
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchSource
import app.katari.entry.interactions.anime.introtimes.IntroTimesResult
import app.katari.entry.interactions.anime.introtimes.matched
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What a service has for the episode picked on its match page. */
internal sealed interface VideoPlayerMatchTimes {
    /** No episode is picked to ask for. */
    data object Unpicked : VideoPlayerMatchTimes
    data object Looking : VideoPlayerMatchTimes

    /** [markers] is empty when the service has no times for the episode. */
    data class Found(val markers: List<PlaybackMarker>) : VideoPlayerMatchTimes
    data object Unreachable : VideoPlayerMatchTimes
}

/** The series or film opened on a match page. */
internal sealed interface VideoPlayerMatchOpened<out M> {
    data object Loading : VideoPlayerMatchOpened<Nothing>
    data object Failed : VideoPlayerMatchOpened<Nothing>

    /**
     * @property season the number of the season whose episodes show; null where the choice lists no seasons.
     * @property pick the episode taken for the playing one; null while none is, and for a film.
     */
    data class Loaded<M>(
        val choice: IntroTimesMatchChoice<M>,
        val season: Int?,
        val pick: IntroTimesMatchPick?,
        val times: VideoPlayerMatchTimes,
    ) : VideoPlayerMatchOpened<M> {
        /** The match that using the pick keeps; null while there is no episode to count the others from. */
        val picked: M? get() = if (choice.film || pick != null) choice.match(pick) else null
    }
}

/**
 * A service's match page for [episode], whose cut is [lengthMs] long.
 *
 * @property current the match in use, and [pickedNone] whether the viewer picked None.
 * @property searching whether the search shows in place of what is [opened].
 * @property opened null when nothing is in use and nothing was picked from the search yet.
 */
internal data class VideoPlayerMatchState<M>(
    val episode: IntroTimesEpisode,
    val lengthMs: Long,
    val current: M?,
    val pickedNone: Boolean,
    val searching: Boolean,
    val opened: VideoPlayerMatchOpened<M>?,
)

/**
 * The page where a service's match for the playing episode is changed: it opens on the match in use, shows what the
 * service has for each episode the viewer points at, and keeps nothing until the viewer uses a pick.
 */
internal class VideoPlayerIntroTimesMatch<C, M : Any>(
    private val scope: CoroutineScope,
    private val source: IntroTimesMatchSource<C, M>,
) {
    val search = VideoPlayerMatchSearch(scope, source::search)

    private val mutableState = MutableStateFlow<VideoPlayerMatchState<M>?>(null)
    val state: StateFlow<VideoPlayerMatchState<M>?> = mutableState.asStateFlow()

    private var open: (suspend () -> IntroTimesMatchChoice<M>?)? = null
    private var job: Job? = null

    /** Opens the page on [current], or on the search when nothing is in use. */
    fun open(episode: IntroTimesEpisode, lengthMs: Long, current: M?, pickedNone: Boolean) {
        job?.cancel()
        mutableState.value = VideoPlayerMatchState(
            episode = episode,
            lengthMs = lengthMs,
            current = current,
            pickedNone = pickedNone,
            searching = current == null,
            opened = null,
        )
        if (current != null) load { source.openMatch(current, episode) }
    }

    fun showSearch() {
        mutableState.update { it?.copy(searching = true) }
    }

    /** Returns from the search to what is opened; false when nothing is, so there is nothing to return to. */
    fun leaveSearch(): Boolean {
        val state = mutableState.value?.takeIf { it.searching && it.opened != null } ?: return false
        mutableState.value = state.copy(searching = false)
        return true
    }

    fun openCandidate(candidate: C) {
        val episode = mutableState.value?.episode ?: return
        load { source.openCandidate(candidate, episode) }
    }

    fun showSeason(number: Int) {
        val loaded = loaded()?.takeIf { it.season != number } ?: return
        job?.cancel()
        show(loaded.copy(season = number, pick = null, times = VideoPlayerMatchTimes.Unpicked))
    }

    fun pick(pick: IntroTimesMatchPick) {
        val loaded = loaded()?.takeIf { it.pick != pick } ?: return
        // Stepping through episodes asks only for the one the viewer stops at.
        ask(loaded.copy(season = pick.season.takeIf { loaded.season != null }, pick = pick), pauseMs = PICK_PAUSE_MS)
    }

    /** Opens again what couldn't be, or asks again for the times that couldn't be reached. */
    fun retry() {
        val loaded = loaded()
        if (loaded != null) ask(loaded, pauseMs = 0L) else open?.let(::load)
    }

    private fun load(open: suspend () -> IntroTimesMatchChoice<M>?) {
        this.open = open
        job?.cancel()
        mutableState.update { it?.copy(searching = false, opened = VideoPlayerMatchOpened.Loading) }
        job = scope.launch {
            val choice = open()
            if (choice == null) {
                mutableState.update { it?.copy(opened = VideoPlayerMatchOpened.Failed) }
                return@launch
            }
            val located = choice.located
            val season = (located?.season ?: choice.seasons.firstOrNull()?.number).takeIf {
                choice.seasons.isNotEmpty()
            }
            val loaded = VideoPlayerMatchOpened.Loaded(choice, season, located, VideoPlayerMatchTimes.Unpicked)
            show(loaded.asking())
            answer(loaded)
        }
    }

    private fun ask(loaded: VideoPlayerMatchOpened.Loaded<M>, pauseMs: Long) {
        job?.cancel()
        show(loaded.asking())
        job = scope.launch {
            delay(pauseMs)
            answer(loaded)
        }
    }

    private suspend fun answer(loaded: VideoPlayerMatchOpened.Loaded<M>) {
        if (!loaded.asks) return
        val lengthMs = mutableState.value?.lengthMs ?: return
        val markers = loaded.choice.times(loaded.pick, lengthMs)
        show(loaded.copy(times = markers?.let(VideoPlayerMatchTimes::Found) ?: VideoPlayerMatchTimes.Unreachable))
    }

    private fun show(loaded: VideoPlayerMatchOpened.Loaded<M>) {
        mutableState.update { it?.copy(opened = loaded) }
    }

    private fun loaded() = mutableState.value?.opened as? VideoPlayerMatchOpened.Loaded<M>

    // A film is asked for whole; a series only once an episode of it is picked.
    private val VideoPlayerMatchOpened.Loaded<M>.asks: Boolean get() = pick != null || choice.film

    private fun VideoPlayerMatchOpened.Loaded<M>.asking() =
        copy(times = if (asks) VideoPlayerMatchTimes.Looking else VideoPlayerMatchTimes.Unpicked)

    private companion object {
        const val PICK_PAUSE_MS = 300L
    }
}

/** Opens the page on what the service's lookup for the playing episode, [times], came to. */
internal fun <M : Any> VideoPlayerIntroTimesMatch<*, M>.open(
    introTimes: VideoPlayerIntroTimesState,
    times: VideoPlayerServiceTimes<M>?,
) {
    val episode = introTimes.episode ?: return
    val result = (times as? VideoPlayerServiceTimes.Done)?.result
    open(episode, introTimes.lengthMs, result?.matched, pickedNone = result == IntroTimesResult.TurnedOff)
}
