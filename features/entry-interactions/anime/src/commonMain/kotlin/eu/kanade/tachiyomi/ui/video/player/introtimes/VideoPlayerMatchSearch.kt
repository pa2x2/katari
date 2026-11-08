package eu.kanade.tachiyomi.ui.video.player.introtimes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

internal sealed interface VideoPlayerMatchSearchState<out C> {
    data object Searching : VideoPlayerMatchSearchState<Nothing>
    data class Results<C>(val found: List<C>) : VideoPlayerMatchSearchState<C>
    data object Failed : VideoPlayerMatchSearchState<Nothing>
}

/** The search behind picking what a service's intro and credits times for an episode come from. */
internal class VideoPlayerMatchSearch<C>(
    private val scope: CoroutineScope,
    private val find: suspend (query: String) -> List<C>,
) {
    private val mutableState = MutableStateFlow<VideoPlayerMatchSearchState<C>>(VideoPlayerMatchSearchState.Searching)
    val state: StateFlow<VideoPlayerMatchSearchState<C>> = mutableState.asStateFlow()

    private var job: Job? = null

    /** Searches once typing pauses, so the services' request limits aren't spent on every letter. */
    fun search(query: String, immediately: Boolean = false) {
        job?.cancel()
        mutableState.value = VideoPlayerMatchSearchState.Searching
        job = scope.launch {
            if (!immediately) delay(TYPING_PAUSE_MS)
            val text = query.trim()
            mutableState.value = if (text.isEmpty()) {
                VideoPlayerMatchSearchState.Results(emptyList())
            } else {
                try {
                    VideoPlayerMatchSearchState.Results(find(text))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    VideoPlayerMatchSearchState.Failed
                }
            }
        }
    }

    private companion object {
        const val TYPING_PAUSE_MS = 500L
    }
}
