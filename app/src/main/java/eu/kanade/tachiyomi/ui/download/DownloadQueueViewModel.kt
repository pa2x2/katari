package eu.kanade.tachiyomi.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import mihon.app.di.AppUiScope
import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class DownloadQueueViewModel(
    private val downloadRuntime: EntryDownloadRuntimeFeature,
) : ViewModel() {

    // The order a drag arranged, shown before the queue reports it so rows don't jump back in between.
    private val arranged = MutableStateFlow<DownloadQueueOrder?>(null)

    private val queue = downloadRuntime.state
        .map { it.queue }
        .distinctUntilChanged()
        .onEach { queue -> arranged.update { order -> order?.takeUnless { it.isMetBy(queue) } } }

    val state: StateFlow<List<EntryDownloadQueueGroup>> = combine(queue, arranged) { queue, order ->
        order?.applyTo(queue) ?: queue
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    val isDownloaderRunning = downloadRuntime.state.map { it.isRunning }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)

    fun startDownloads() {
        downloadRuntime.start()
    }

    fun pauseDownloads() {
        downloadRuntime.pause()
    }

    fun clearQueue() {
        downloadRuntime.clearQueue()
    }

    /** Moves the row or source with key [from] to where [to] is while a drag is in progress. */
    fun arrange(from: String, to: String) {
        arranged.update { order -> (order ?: DownloadQueueOrder.of(state.value)).moved(from, to) ?: order }
    }

    /** Submits the order the drag arranged once it is released. */
    fun submitArrangement() {
        if (arranged.value != null) downloadRuntime.reorderQueue(state.value.flatMap { it.items })
    }

    fun <R : Comparable<R>> sort(selector: (EntryDownloadQueueItem) -> R, reverse: Boolean) {
        val items = state.value.sortedWithinGroups(selector, reverse)
        arranged.value = null
        downloadRuntime.reorderQueue(items)
    }

    fun moveItem(item: EntryDownloadQueueItem, moveToTop: Boolean) {
        val items = state.value.withItemMoved(item, moveToTop)
        arranged.value = null
        downloadRuntime.reorderQueue(items)
    }

    fun moveSeries(item: EntryDownloadQueueItem, moveToTop: Boolean) {
        arranged.value = null
        downloadRuntime.reorderEntry(item.entryType, item.entryId, moveToTop)
    }

    fun cancel(item: EntryDownloadQueueItem) {
        downloadRuntime.cancelQueued(listOf(item))
    }

    fun cancelSeries(item: EntryDownloadQueueItem) {
        val downloads = state.value
            .flatMap { it.items }
            .filter { it.entryType == item.entryType && it.entryId == item.entryId }
        if (downloads.isNotEmpty()) {
            downloadRuntime.cancelQueued(downloads)
        }
    }
}
