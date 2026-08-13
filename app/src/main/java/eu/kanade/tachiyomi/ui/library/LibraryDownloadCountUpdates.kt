package eu.kanade.tachiyomi.ui.library

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.scan
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadStatus
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

internal fun observeLibraryDownloadCountUpdates(
    initialItems: List<LibraryItem>,
    statusUpdates: Flow<EntryDownloadStatus>,
    calculateDownloadCount: (LibraryItem) -> Int,
): Flow<List<LibraryItem>> {
    val itemIndexByMemberKey = buildMap {
        initialItems.forEachIndexed { itemIndex, item ->
            item.memberEntries.forEach { member ->
                putIfAbsent(LibraryItemKey(member.type, member.id), itemIndex)
            }
        }
    }
    val persistentItems = initialItems.toPersistentList()

    return statusUpdates
        .filter { status -> status.persistedContentChanged && status.entryId != null }
        .scan(persistentItems) { items, status ->
            val memberKey = LibraryItemKey(status.entryType, status.entryId ?: return@scan items)
            val affectedIndex = itemIndexByMemberKey[memberKey] ?: return@scan items
            val affectedItem = items[affectedIndex]
            val downloadCount = calculateDownloadCount(affectedItem)
            if (downloadCount == affectedItem.downloadCount) {
                items
            } else {
                items.replacingAt(affectedIndex, affectedItem.copy(downloadCount = downloadCount))
            }
        }
        .distinctUntilChanged { previous, current -> previous === current }
}

internal fun LibraryItem.calculateDownloadCount(downloadRuntime: EntryDownloadRuntimeFeature): Int {
    return memberEntries.sumOf(downloadRuntime::downloadCount)
}
