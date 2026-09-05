package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadQueueScreenModel(
    private val downloadRuntime: EntryDownloadRuntimeFeature = Injekt.get(),
) : ScreenModel {

    private val _state = MutableStateFlow(emptyList<DownloadQueueHeaderItem>())
    val state = _state.asStateFlow()

    lateinit var controllerBinding: DownloadListBinding
    var adapter: DownloadQueueAdapter? = null

    val listener = object : DownloadQueueAdapter.DownloadQueueItemListener {
        override fun onItemReleased(position: Int) {
            val adapter = adapter ?: return
            val reorderedItems = mutableListOf<EntryDownloadQueueItem>()
            adapter.headerItems.forEach { header ->
                (header as DownloadQueueHeaderItem).subItems.forEach { item ->
                    reorderedItems += item.payloadAsDownloadQueueItem()
                }
            }
            downloadRuntime.reorderQueue(reorderedItems)
        }

        override fun onMenuItemClick(position: Int, menuItem: MenuItem) {
            val selectedItem = adapter?.getItem(position) as? DownloadQueueItem ?: return
            when (menuItem.itemId) {
                R.id.move_to_top, R.id.move_to_bottom -> {
                    val header = selectedItem.header as DownloadQueueHeaderItem
                    header.removeSubItem(selectedItem)
                    if (menuItem.itemId == R.id.move_to_top) {
                        header.addSubItem(0, selectedItem)
                    } else {
                        header.addSubItem(selectedItem)
                    }
                    onItemReleased(position)
                }
                R.id.move_to_top_series, R.id.move_to_bottom_series -> {
                    moveSeries(selectedItem, moveToTop = menuItem.itemId == R.id.move_to_top_series)
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

    init {
        screenModelScope.launch {
            downloadRuntime.state.map { it.queue }.distinctUntilChanged().collect { groups ->
                val current = state.value
                val sameStructure = current.size == groups.size && groups.withIndex().all { (index, group) ->
                    val header = current[index]
                    header.model.id == group.sourceId && header.model.entryType == group.entryType &&
                        header.model.title == group.sourceName &&
                        header.subItems.map { it.payload.identity } == group.items.map { it.identity }
                }
                if (sameStructure) {
                    // Update existing rows without rebuilding the adapter for every transfer tick.
                    groups.forEachIndexed { index, group ->
                        current[index].subItems.zip(group.items).forEach { (row, download) ->
                            updateDownload(row, download)
                        }
                    }
                    return@collect
                }
                val newList = groups.map { group ->
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
                _state.update { newList }
            }
        }
    }

    override fun onDispose() {
        adapter = null
    }

    val isDownloaderRunning = downloadRuntime.state.map { it.isRunning }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

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
        val adapter = adapter ?: return
        val reorderedItems = mutableListOf<EntryDownloadQueueItem>()
        adapter.headerItems.forEach { headerItem ->
            val header = headerItem as DownloadQueueHeaderItem
            header.subItems = header.subItems.sortedBy(selector).toMutableList().apply {
                if (reverse) reverse()
            }
            header.subItems.forEach { item ->
                reorderedItems += item.payloadAsDownloadQueueItem()
            }
        }
        downloadRuntime.reorderQueue(reorderedItems)
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

    private fun moveSeries(selectedItem: DownloadQueueItem, moveToTop: Boolean) {
        val selected = selectedItem.payloadAsDownloadQueueItem()
        downloadRuntime.reorderEntry(selected.entryType, selected.entryId, moveToTop)
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
