package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryDownloadInteraction
import mihon.entry.interactions.EntryDownloadQueueItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadQueueScreenModel(
    private val entryDownloadInteraction: EntryDownloadInteraction = Injekt.get(),
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
            entryDownloadInteraction.reorderQueue(reorderedItems)
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
            entryDownloadInteraction.queueState.collect { groups ->
                val newList = groups.map { group ->
                    DownloadQueueHeaderItem(
                        DownloadQueueHeaderModel(
                            id = group.sourceId,
                            contentType = group.entryType.toDownloadQueueContentType(),
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
                                    modelProvider = download::toDownloadQueueItemModel,
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

    val isDownloaderRunning = entryDownloadInteraction.isRunning
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun getDownloadStatusFlow() = entryDownloadInteraction.queueStatusUpdates()
    fun getDownloadProgressFlow() = entryDownloadInteraction.queueProgressUpdates()

    fun startDownloads() {
        entryDownloadInteraction.startDownloads()
    }

    fun pauseDownloads() {
        entryDownloadInteraction.pauseDownloads()
    }

    fun clearQueue() {
        entryDownloadInteraction.clearQueue()
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
        entryDownloadInteraction.reorderQueue(reorderedItems)
    }

    fun onStatusChange(download: EntryDownloadQueueItem) {
        getHolder(download.childId)?.notifyProgress()
        getHolder(download.childId)?.notifyProgressText()
    }

    /**
     * Called when a page of a download is downloaded.
     *
     * @param download the download whose page has been downloaded.
     */
    fun onUpdateStepProgress(download: EntryDownloadQueueItem) {
        getHolder(download.childId)?.notifyProgress()
        getHolder(download.childId)?.notifyProgressText()
    }

    private fun moveSeries(selectedItem: DownloadQueueItem, moveToTop: Boolean) {
        val selected = selectedItem.payloadAsDownloadQueueItem()
        entryDownloadInteraction.reorderSeries(selected.entryType, selected.entryId, moveToTop)
    }

    private fun cancelItem(selectedItem: DownloadQueueItem) {
        entryDownloadInteraction.cancelQueuedDownloads(listOf(selectedItem.payloadAsDownloadQueueItem()))
    }

    private fun cancelSeries(selectedItem: DownloadQueueItem) {
        val adapter = adapter ?: return
        val selected = selectedItem.payloadAsDownloadQueueItem()
        val downloads = adapter.currentItems
            .filterIsInstance<DownloadQueueItem>()
            .map(DownloadQueueItem::payloadAsDownloadQueueItem)
            .filter { it.entryType == selected.entryType && it.entryId == selected.entryId }
        if (downloads.isNotEmpty()) {
            entryDownloadInteraction.cancelQueuedDownloads(downloads)
        }
    }

    private fun getHolder(itemId: Long): DownloadQueueHolder? {
        return controllerBinding.root.findViewHolderForItemId(itemId) as? DownloadQueueHolder
    }
}

internal fun DownloadQueueItem.payloadAsDownloadQueueItem(): EntryDownloadQueueItem {
    return payloadAs<EntryDownloadQueueItem>() ?: error("Download queue item payload is not an entry download")
}
