package eu.kanade.tachiyomi.ui.download

import mihon.entry.interactions.download.EntryDownloadRuntimeFeature

/** Submits queue edits without mutating the headers that describe the currently rendered order. */
internal class DownloadQueueReordering(
    private val downloadRuntime: EntryDownloadRuntimeFeature,
    private val adapter: () -> DownloadQueueAdapter?,
) {
    fun submitDraggedOrder() {
        submit { it.subItems }
    }

    fun moveItem(selected: DownloadQueueItem, moveToTop: Boolean) {
        submit { header ->
            if (header === selected.header) {
                header.subItems.toMutableList().apply {
                    remove(selected)
                    add(if (moveToTop) 0 else size, selected)
                }
            } else {
                header.subItems
            }
        }
    }

    fun moveSeries(selectedItem: DownloadQueueItem, moveToTop: Boolean) {
        val selected = selectedItem.payloadAsDownloadQueueItem()
        downloadRuntime.reorderEntry(selected.entryType, selected.entryId, moveToTop)
    }

    fun <R : Comparable<R>> sort(selector: (DownloadQueueItem) -> R, reverse: Boolean) {
        submit { header ->
            val sorted = header.subItems.sortedBy(selector)
            if (reverse) sorted.asReversed() else sorted
        }
    }

    private fun submit(order: (DownloadQueueHeaderItem) -> List<DownloadQueueItem>) {
        val currentAdapter = adapter() ?: return
        val items = currentAdapter.headerItems.flatMap { header ->
            order(header as DownloadQueueHeaderItem).map(DownloadQueueItem::payloadAsDownloadQueueItem)
        }
        downloadRuntime.reorderQueue(items)
    }
}
