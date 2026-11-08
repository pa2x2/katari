package eu.kanade.tachiyomi.ui.video.player.handoff

import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mihon.entry.interactions.media.handoff.EntryProgressOffers
import tachiyomi.domain.entry.service.sortedForReading

/**
 * The player's offers to go where another device got to (D053), for the title playing in [ready]: one stays until it
 * is taken or dismissed, or another episode starts.
 */
internal class VideoPlayerProgressOffers(
    scope: CoroutineScope,
    offers: EntryProgressOffers,
    private val ready: StateFlow<VideoPlayerViewModel.State>,
    readyStates: Flow<VideoPlayerViewModel.State.Ready>,
) {
    private val shown = MutableStateFlow<VideoProgressOffer?>(null)
    val offer: StateFlow<VideoProgressOffer?> = shown.asStateFlow()

    init {
        scope.launch {
            readyStates.map { it.chapterId }.distinctUntilChanged().collect { shown.value = null }
        }
        scope.launch {
            readyStates.map { setOf(it.ownerEntryId) + it.memberIds }.distinctUntilChanged().collectLatest { entryIds ->
                offers.offers(entryIds).collect { offer ->
                    val current = ready.value as? VideoPlayerViewModel.State.Ready ?: return@collect
                    val episodes = current.allChapters.sortedForReading(current.entry, current.memberIds)
                    videoProgressOffer(offer, episodes, current.chapterId, current.resumePositionMs)
                        ?.let { shown.value = it }
                }
            }
        }
    }

    fun dismiss() {
        shown.value = null
    }
}
