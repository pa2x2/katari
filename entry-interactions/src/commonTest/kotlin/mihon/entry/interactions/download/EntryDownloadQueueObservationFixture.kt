package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

internal class EntryDownloadQueueObservationFixture {
    val download = MutableObservedDownload(
        EntryDownloadQueueItem(
            identity = EntryDownloadIdentity(1, EntryType.BOOK, 2, 3, 4),
            state = EntryDownloadState.QUEUE,
            title = "Book",
            subtitle = "Chapter",
            dateUpload = 0,
            chapterNumber = 1.0,
            progress = 0,
            progressMax = 100,
        ),
    )
    val queue = MutableStateFlow(listOf(download))
    val status = MutableSharedFlow<MutableObservedDownload>()
    val progress = MutableSharedFlow<MutableObservedDownload>()
    val snapshots = observeEntryDownloadQueue(queue, status, progress) { downloads ->
        if (downloads.isEmpty()) {
            emptyList()
        } else {
            listOf(EntryDownloadQueueGroup(3, "Source", EntryType.BOOK, downloads.map { it.item.copy() }))
        }
    }
}

internal class MutableObservedDownload(var item: EntryDownloadQueueItem)
