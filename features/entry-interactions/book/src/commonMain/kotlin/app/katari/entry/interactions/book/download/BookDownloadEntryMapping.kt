package app.katari.entry.interactions.book.download

import app.katari.entry.interactions.book.download.model.BookDownload
import app.katari.entry.interactions.book.download.model.BookDownloadFailure
import app.katari.entry.interactions.download.EntryDownloadIdentity
import app.katari.entry.interactions.download.EntryDownloadMessage
import app.katari.entry.interactions.download.EntryDownloadPhase
import app.katari.entry.interactions.download.EntryDownloadPresentation
import app.katari.entry.interactions.download.EntryDownloadProgress
import app.katari.entry.interactions.download.EntryDownloadQueueGroup
import app.katari.entry.interactions.download.EntryDownloadQueueItem
import app.katari.entry.interactions.download.EntryDownloadState
import app.katari.entry.interactions.download.EntryDownloadStatus
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*

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
    entryId = entry.id,
    sourceId = entry.source,
)

internal fun BookDownload.toEntryDownloadQueueItem(): EntryDownloadQueueItem {
    val statusSnapshot = status
    val progressSnapshot = progress
    val failureSnapshot = failure
    return EntryDownloadQueueItem(
        identity = EntryDownloadIdentity.from(entry, chapter),
        state = statusSnapshot.toEntryDownloadState(),
        title = entry.title,
        subtitle = chapter.name,
        dateUpload = chapter.dateUpload,
        chapterNumber = chapter.chapterNumber,
        progress = progressSnapshot,
        progressMax = 100,
        presentation = EntryDownloadPresentation(
            phase = statusSnapshot.toEntryDownloadPhase(),
            progress = if (statusSnapshot == BookDownload.State.DOWNLOADING && progressSnapshot > 0) {
                EntryDownloadProgress.Percent(progressSnapshot)
            } else {
                EntryDownloadProgress.None
            },
            failure = failureSnapshot
                ?.takeIf { statusSnapshot == BookDownload.State.ERROR }
                ?.toEntryDownloadMessage(),
        ),
    )
}

private fun BookDownload.State.toEntryDownloadPhase(): EntryDownloadPhase = when (this) {
    BookDownload.State.NOT_DOWNLOADED -> EntryDownloadPhase.IDLE
    BookDownload.State.QUEUE -> EntryDownloadPhase.QUEUED
    BookDownload.State.RESOLVING -> EntryDownloadPhase.RESOLVING
    BookDownload.State.DOWNLOADING -> EntryDownloadPhase.TRANSFERRING
    BookDownload.State.FINALIZING -> EntryDownloadPhase.FINALIZING
    BookDownload.State.DOWNLOADED -> EntryDownloadPhase.COMPLETED
    BookDownload.State.ERROR -> EntryDownloadPhase.FAILED
}

internal fun BookDownload.State.toEntryDownloadState(): EntryDownloadState = when (this) {
    BookDownload.State.NOT_DOWNLOADED -> EntryDownloadState.NOT_DOWNLOADED
    BookDownload.State.QUEUE -> EntryDownloadState.QUEUE
    BookDownload.State.RESOLVING,
    BookDownload.State.DOWNLOADING,
    BookDownload.State.FINALIZING,
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
