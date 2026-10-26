package eu.kanade.tachiyomi.ui.video.player.introtimes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mihon.entry.interactions.anime.introtimes.AniListSeries
import mihon.entry.interactions.anime.introtimes.IntroTimesLookup
import kotlin.coroutines.cancellation.CancellationException

internal sealed interface VideoPlayerSeriesSearchState {
    data object Searching : VideoPlayerSeriesSearchState
    data class Results(val series: List<AniListSeries>) : VideoPlayerSeriesSearchState
    data object Failed : VideoPlayerSeriesSearchState
}

/** The AniList search behind picking the series an episode's intro and credits times come from. */
internal class VideoPlayerSeriesSearch(
    private val scope: CoroutineScope,
    private val lookup: IntroTimesLookup,
) {
    private val mutableState = MutableStateFlow<VideoPlayerSeriesSearchState>(VideoPlayerSeriesSearchState.Searching)
    val state: StateFlow<VideoPlayerSeriesSearchState> = mutableState.asStateFlow()

    private var job: Job? = null

    /** Searches once typing pauses, so AniList's request limit isn't spent on every letter. */
    fun search(query: String, immediately: Boolean = false) {
        job?.cancel()
        mutableState.value = VideoPlayerSeriesSearchState.Searching
        job = scope.launch {
            if (!immediately) delay(TYPING_PAUSE_MS)
            val text = query.trim()
            mutableState.value = if (text.isEmpty()) {
                VideoPlayerSeriesSearchState.Results(emptyList())
            } else {
                try {
                    VideoPlayerSeriesSearchState.Results(lookup.search(text))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    VideoPlayerSeriesSearchState.Failed
                }
            }
        }
    }

    private companion object {
        const val TYPING_PAUSE_MS = 500L
    }
}
