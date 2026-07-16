package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryDownloadMessage
import mihon.entry.interactions.EntryDownloadQueueGroup
import mihon.entry.interactions.EntryDownloadQueueItem
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR

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
    state = status.toEntryDownloadState(),
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

internal fun BookDownloadFailure.toEntryDownloadMessage(): EntryDownloadMessage {
    message?.takeIf(String::isNotBlank)?.let { return EntryDownloadMessage.Text(it) }
    val resource = when (reason) {
        BookDownloadFailure.Reason.SOURCE_NOT_FOUND -> MR.strings.download_notifier_source_not_available
        BookDownloadFailure.Reason.CONTENT_UNAVAILABLE -> MR.strings.download_notifier_book_content_unavailable
        BookDownloadFailure.Reason.UNSUPPORTED_FORMAT -> MR.strings.download_notifier_book_unsupported_format
        BookDownloadFailure.Reason.AMBIGUOUS_RESOURCE -> MR.strings.download_notifier_book_ambiguous_resource
        BookDownloadFailure.Reason.STORAGE -> MR.strings.download_notifier_book_storage_error
        BookDownloadFailure.Reason.INTEGRITY -> MR.strings.download_notifier_book_integrity_error
        BookDownloadFailure.Reason.NETWORK -> MR.strings.download_notifier_book_network_error
        BookDownloadFailure.Reason.UNKNOWN -> MR.strings.download_notifier_unknown_error
    }
    return EntryDownloadMessage.Resource(resource)
}
