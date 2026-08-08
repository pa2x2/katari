package mihon.entry.interactions.manga.download

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryDownloadIdentity
import mihon.entry.interactions.download.EntryDownloadPhase
import mihon.entry.interactions.download.EntryDownloadPresentation
import mihon.entry.interactions.download.EntryDownloadProgress
import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.download.EntryDownloadStatus
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
    val statusSnapshot = status
    val pagesSnapshot = pages
    val progressSnapshot = totalProgress
    val downloadedImagesSnapshot = downloadedImages
    val failureSnapshot = failure
    return EntryDownloadQueueItem(
        identity = EntryDownloadIdentity.from(entry, chapter),
        state = statusSnapshot.toEntryDownloadState(),
        title = entry.title,
        subtitle = chapter.name,
        dateUpload = chapter.dateUpload,
        chapterNumber = chapter.chapterNumber,
        progress = progressSnapshot,
        progressMax = pagesSnapshot?.size?.times(100) ?: 100,
        presentation = EntryDownloadPresentation(
            phase = statusSnapshot.toEntryDownloadPhase(),
            progress = if (statusSnapshot == DownloadState.DOWNLOADING) {
                pagesSnapshot?.let { EntryDownloadProgress.Units(downloadedImagesSnapshot, it.size) }
                    ?: EntryDownloadProgress.None
            } else {
                EntryDownloadProgress.None
            },
            failure = failureSnapshot.takeIf { statusSnapshot == DownloadState.ERROR },
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
