package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryDownloadIdentity
import mihon.entry.interactions.EntryDownloadPhase
import mihon.entry.interactions.EntryDownloadPresentation
import mihon.entry.interactions.EntryDownloadProgress
import mihon.entry.interactions.EntryDownloadQueueGroup
import mihon.entry.interactions.EntryDownloadQueueItem
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import mihon.entry.interactions.manga.download.model.DownloadState
import mihon.entry.interactions.manga.download.model.MangaDownload

internal fun List<MangaDownload>.toMangaEntryDownloadQueueGroups(): List<EntryDownloadQueueGroup> {
    return groupBy { it.source.id }
        .map { (sourceId, downloads) ->
            EntryDownloadQueueGroup(
                sourceId = sourceId,
                sourceName = downloads.firstOrNull()?.source?.name ?: sourceId.toString(),
                entryType = EntryType.MANGA,
                items = downloads.map { it.toEntryDownloadQueueItem() },
            )
        }
}

internal fun MangaDownload.toEntryDownloadStatus(): EntryDownloadStatus {
    return EntryDownloadStatus(
        entryType = EntryType.MANGA,
        chapterId = chapter.id,
        state = status.toEntryDownloadState(),
        progress = progress,
    )
}

internal fun MangaDownload.toEntryDownloadQueueItem(): EntryDownloadQueueItem {
    return EntryDownloadQueueItem(
        identity = EntryDownloadIdentity.from(entry, chapter),
        state = status.toEntryDownloadState(),
        title = entry.title,
        subtitle = chapter.name,
        dateUpload = chapter.dateUpload,
        chapterNumber = chapter.chapterNumber,
        progress = totalProgress,
        progressMax = pages?.size?.times(100) ?: 100,
        presentation = EntryDownloadPresentation(
            phase = status.toEntryDownloadPhase(),
            progress = if (status == DownloadState.DOWNLOADING) {
                pages?.let { EntryDownloadProgress.Units(downloadedImages, it.size) }
                    ?: EntryDownloadProgress.None
            } else {
                EntryDownloadProgress.None
            },
            failure = failure.takeIf { status == DownloadState.ERROR },
        ),
    )
}

private fun DownloadState.toEntryDownloadPhase(): EntryDownloadPhase = when (this) {
    DownloadState.NOT_DOWNLOADED -> EntryDownloadPhase.IDLE
    DownloadState.QUEUE -> EntryDownloadPhase.QUEUED
    DownloadState.DOWNLOADING -> EntryDownloadPhase.TRANSFERRING
    DownloadState.DOWNLOADED -> EntryDownloadPhase.COMPLETED
    DownloadState.ERROR -> EntryDownloadPhase.FAILED
}

internal fun DownloadState.toEntryDownloadState(): EntryDownloadState {
    return when (this) {
        DownloadState.NOT_DOWNLOADED -> EntryDownloadState.NOT_DOWNLOADED
        DownloadState.QUEUE -> EntryDownloadState.QUEUE
        DownloadState.DOWNLOADING -> EntryDownloadState.DOWNLOADING
        DownloadState.DOWNLOADED -> EntryDownloadState.DOWNLOADED
        DownloadState.ERROR -> EntryDownloadState.ERROR
    }
}
