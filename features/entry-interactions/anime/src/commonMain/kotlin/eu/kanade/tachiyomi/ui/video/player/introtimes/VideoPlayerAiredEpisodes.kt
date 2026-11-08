package eu.kanade.tachiyomi.ui.video.player.introtimes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mihon.entry.interactions.anime.introtimes.theintrodb.AiredEpisode
import mihon.entry.interactions.anime.introtimes.theintrodb.TheIntroDbCandidate
import mihon.entry.interactions.anime.introtimes.theintrodb.TheIntroDbLookup

internal sealed interface VideoPlayerAiredEpisodesState {
    data object Loading : VideoPlayerAiredEpisodesState

    /** [episodes] is empty when TVmaze doesn't list the series. */
    data class Loaded(val episodes: List<AiredEpisode>) : VideoPlayerAiredEpisodesState
    data object Failed : VideoPlayerAiredEpisodesState
}

/** The episodes of the series picked on TheIntroDB's page, among which the viewer finds the one playing. */
internal class VideoPlayerAiredEpisodes(
    private val scope: CoroutineScope,
    private val lookup: TheIntroDbLookup,
) {
    private val mutableState = MutableStateFlow<VideoPlayerAiredEpisodesState>(VideoPlayerAiredEpisodesState.Loading)
    val state: StateFlow<VideoPlayerAiredEpisodesState> = mutableState.asStateFlow()

    private var job: Job? = null

    fun load(series: TheIntroDbCandidate) {
        job?.cancel()
        mutableState.value = VideoPlayerAiredEpisodesState.Loading
        job = scope.launch {
            mutableState.value = lookup.episodes(series)
                ?.let(VideoPlayerAiredEpisodesState::Loaded)
                ?: VideoPlayerAiredEpisodesState.Failed
        }
    }
}
