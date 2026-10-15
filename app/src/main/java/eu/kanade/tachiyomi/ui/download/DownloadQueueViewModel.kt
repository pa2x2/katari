package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import mihon.app.di.AppUiScope
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class DownloadQueueViewModel(
    private val downloadRuntime: EntryDownloadRuntimeFeature,
) : ViewModel() {

    // The rows last handed to the adapter; transfer ticks update them in place instead of emitting a new list
    private var rows = emptyList<DownloadQueueHeaderItem>()

    val state: StateFlow<List<DownloadQueueHeaderItem>> = downloadRuntime.state
        .map { it.queue }
        .distinctUntilChanged()
        .mapNotNull { groups ->
            val current = rows
            val sameStructure = current.size == groups.size && groups.withIndex().all { (index, group) ->
                val header = current[index]
                header.model.id == group.sourceId && header.model.entryType == group.entryType &&
                    header.model.title == group.sourceName &&
                    header.subItems.map { it.payload.identity } == group.items.map { it.identity }
            }
            if (sameStructure) {
                groups.forEachIndexed { index, group ->
                    current[index].subItems.zip(group.items).forEach { (row, download) ->
                        updateDownload(row, download)
                    }
                }
                return@mapNotNull null
            }
            groups.map { group ->
                DownloadQueueHeaderItem(
                    DownloadQueueHeaderModel(
                        id = group.sourceId,
                        entryType = group.entryType,
                        title = group.sourceName,
                        count = group.items.size,
                    ),
                ).apply {
                    addSubItems(
                        0,
                        group.items.map { download ->
                            DownloadQueueItem(
                                payload = download,
                                header = this,
                            )
                        },
                    )
                }
            }
                .also { rows = it }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    lateinit var controllerBinding: DownloadListBinding
    var adapter: DownloadQueueAdapter? = null
    private val reordering = DownloadQueueReordering(downloadRuntime) { adapter }

    val listener = object : DownloadQueueAdapter.DownloadQueueItemListener {
        override fun onItemReleased(position: Int) {
            reordering.submitDraggedOrder()
        }

        override fun onMenuItemClick(position: Int, menuItem: MenuItem) {
            val selectedItem = adapter?.getItem(position) as? DownloadQueueItem ?: return
            when (menuItem.itemId) {
                R.id.move_to_top, R.id.move_to_bottom -> {
                    reordering.moveItem(selectedItem, moveToTop = menuItem.itemId == R.id.move_to_top)
                }
                R.id.move_to_top_series, R.id.move_to_bottom_series -> {
                    reordering.moveSeries(selectedItem, moveToTop = menuItem.itemId == R.id.move_to_top_series)
                }
                R.id.cancel_download -> {
                    cancelItem(selectedItem)
                }
                R.id.cancel_series -> {
                    cancelSeries(selectedItem)
                }
            }
        }
    }

    override fun onCleared() {
        adapter = null
    }

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

    fun <R : Comparable<R>> reorderQueue(selector: (DownloadQueueItem) -> R, reverse: Boolean = false) {
        reordering.sort(selector, reverse)
    }

    private fun updateDownload(row: DownloadQueueItem, download: EntryDownloadQueueItem) {
        // Keep off-screen rows current too, so rebinding cannot restore stale queue values.
        if (row.payload == download) return
        row.update(download)
        getHolder(download)?.let { holder ->
            holder.notifyProgress()
            holder.notifyProgressText()
        }
    }

    private fun cancelItem(selectedItem: DownloadQueueItem) {
        downloadRuntime.cancelQueued(listOf(selectedItem.payloadAsDownloadQueueItem()))
    }

    private fun cancelSeries(selectedItem: DownloadQueueItem) {
        val adapter = adapter ?: return
        val selected = selectedItem.payloadAsDownloadQueueItem()
        val downloads = adapter.currentItems
            .filterIsInstance<DownloadQueueItem>()
            .map(DownloadQueueItem::payloadAsDownloadQueueItem)
            .filter { it.entryType == selected.entryType && it.entryId == selected.entryId }
        if (downloads.isNotEmpty()) {
            downloadRuntime.cancelQueued(downloads)
        }
    }

    private fun getHolder(download: EntryDownloadQueueItem): DownloadQueueHolder? {
        if (!::controllerBinding.isInitialized) return null
        val position = adapter?.currentItems?.indexOfFirst {
            it is DownloadQueueItem && it.payload.identity == download.identity
        }?.takeIf { it >= 0 } ?: return null
        return controllerBinding.root.findViewHolderForAdapterPosition(position) as? DownloadQueueHolder
    }
}

internal fun DownloadQueueItem.payloadAsDownloadQueueItem(): EntryDownloadQueueItem {
    return payloadAs<EntryDownloadQueueItem>() ?: error("Download queue item payload is not an entry download")
}
