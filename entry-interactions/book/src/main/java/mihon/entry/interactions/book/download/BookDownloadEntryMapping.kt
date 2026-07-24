package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryDownloadIdentity
import mihon.entry.interactions.EntryDownloadMessage
import mihon.entry.interactions.EntryDownloadPhase
import mihon.entry.interactions.EntryDownloadPresentation
import mihon.entry.interactions.EntryDownloadProgress
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
    identity = EntryDownloadIdentity.from(entry, chapter),
    state = status.toEntryDownloadState(),
    title = entry.title,
    subtitle = chapter.name,
    dateUpload = chapter.dateUpload,
    chapterNumber = chapter.chapterNumber,
    progress = progress,
    progressMax = 100,
    presentation = EntryDownloadPresentation(
        phase = status.toEntryDownloadPhase(),
        progress = if (status == BookDownload.State.DOWNLOADING) {
            EntryDownloadProgress.Percent(progress)
        } else {
            EntryDownloadProgress.None
        },
        failure = failure
            ?.takeIf { status == BookDownload.State.ERROR }
            ?.toEntryDownloadMessage(),
    ),
)

private fun BookDownload.State.toEntryDownloadPhase(): EntryDownloadPhase = when (this) {
    BookDownload.State.NOT_DOWNLOADED -> EntryDownloadPhase.IDLE
    BookDownload.State.QUEUE -> EntryDownloadPhase.QUEUED
    BookDownload.State.RESOLVING -> EntryDownloadPhase.RESOLVING
    BookDownload.State.DOWNLOADING -> EntryDownloadPhase.TRANSFERRING
    BookDownload.State.DOWNLOADED -> EntryDownloadPhase.COMPLETED
    BookDownload.State.ERROR -> EntryDownloadPhase.FAILED
}

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
