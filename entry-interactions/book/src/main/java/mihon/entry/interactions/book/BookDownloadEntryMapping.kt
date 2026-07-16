package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryDownloadQueueGroup
import mihon.entry.interactions.EntryDownloadQueueItem
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import mihon.entry.interactions.book.download.model.BookDownload
import tachiyomi.domain.source.service.SourceManager

internal fun List<BookDownload>.toBookEntryDownloadQueueGroups(
    sourceManager: SourceManager,
): List<EntryDownloadQueueGroup> = groupBy { it.entry.source }
    .map { (sourceId, downloads) ->
        EntryDownloadQueueGroup(
            sourceId = sourceId,
            sourceName = sourceManager.get(sourceId)?.name ?: sourceId.toString(),
            entryType = EntryType.BOOK,
            items = downloads.map(BookDownload::toEntryDownloadQueueItem),
        )
    }

internal fun BookDownload.toEntryDownloadStatus(): EntryDownloadStatus = EntryDownloadStatus(
    entryType = EntryType.BOOK,
    chapterId = chapter.id,
    state = status.toEntryDownloadState(),
    progress = progress,
)

internal fun BookDownload.toEntryDownloadQueueItem(): EntryDownloadQueueItem = EntryDownloadQueueItem(
    entryType = EntryType.BOOK,
    entryId = entry.id,
    childId = chapter.id,
    title = entry.title,
    subtitle = chapter.name,
    dateUpload = chapter.dateUpload,
    chapterNumber = chapter.chapterNumber,
    progress = progress,
    progressMax = 100,
    progressText = when (status) {
        BookDownload.State.ERROR -> failure?.message ?: failure?.reason?.name ?: "Error"
        BookDownload.State.DOWNLOADED -> "Downloaded"
        BookDownload.State.RESOLVING -> "Resolving"
        BookDownload.State.DOWNLOADING -> "$progress%"
        BookDownload.State.QUEUE -> "Queued"
        BookDownload.State.NOT_DOWNLOADED -> ""
    },
)

internal fun BookDownload.State.toEntryDownloadState(): EntryDownloadState = when (this) {
    BookDownload.State.NOT_DOWNLOADED -> EntryDownloadState.NOT_DOWNLOADED
    BookDownload.State.QUEUE -> EntryDownloadState.QUEUE
    BookDownload.State.RESOLVING,
    BookDownload.State.DOWNLOADING,
    -> EntryDownloadState.DOWNLOADING
    BookDownload.State.DOWNLOADED -> EntryDownloadState.DOWNLOADED
    BookDownload.State.ERROR -> EntryDownloadState.ERROR
}
