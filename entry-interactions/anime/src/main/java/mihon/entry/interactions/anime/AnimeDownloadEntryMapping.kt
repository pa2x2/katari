package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryDownloadQueueGroup
import mihon.entry.interactions.EntryDownloadQueueItem
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import mihon.entry.interactions.anime.download.model.AnimeDownload
import tachiyomi.domain.source.service.SourceManager

internal fun List<AnimeDownload>.toAnimeEntryDownloadQueueGroups(
    sourceManager: SourceManager,
): List<EntryDownloadQueueGroup> {
    return groupBy { it.anime.source }
        .map { (sourceId, downloads) ->
            EntryDownloadQueueGroup(
                sourceId = sourceId,
                sourceName = sourceManager.get(sourceId)?.name ?: sourceId.toString(),
                entryType = EntryType.ANIME,
                items = downloads.map { it.toEntryDownloadQueueItem() },
            )
        }
}

internal fun AnimeDownload.toEntryDownloadStatus(): EntryDownloadStatus {
    return EntryDownloadStatus(
        entryType = EntryType.ANIME,
        chapterId = episode.id,
        state = status.toEntryDownloadState(),
        progress = progress,
    )
}

internal fun AnimeDownload.toEntryDownloadQueueItem(): EntryDownloadQueueItem {
    return EntryDownloadQueueItem(
        entryType = EntryType.ANIME,
        entryId = anime.id,
        childId = episode.id,
        title = anime.title,
        subtitle = episode.name,
        dateUpload = episode.dateUpload,
        chapterNumber = episode.chapterNumber,
        progress = progress,
        progressMax = 100,
        progressText = when (status) {
            AnimeDownload.State.ERROR -> failure?.message ?: failure?.reason?.name ?: "Error"
            AnimeDownload.State.DOWNLOADED -> "Downloaded"
            AnimeDownload.State.RESOLVING -> "Resolving"
            AnimeDownload.State.DOWNLOADING -> "$progress%"
            AnimeDownload.State.QUEUE -> "Queued"
            AnimeDownload.State.NOT_DOWNLOADED -> ""
        },
    )
}

internal fun AnimeDownload.State.toEntryDownloadState(): EntryDownloadState {
    return when (this) {
        AnimeDownload.State.NOT_DOWNLOADED -> EntryDownloadState.NOT_DOWNLOADED
        AnimeDownload.State.QUEUE -> EntryDownloadState.QUEUE
        AnimeDownload.State.RESOLVING,
        AnimeDownload.State.DOWNLOADING,
        -> EntryDownloadState.DOWNLOADING
        AnimeDownload.State.DOWNLOADED -> EntryDownloadState.DOWNLOADED
        AnimeDownload.State.ERROR -> EntryDownloadState.ERROR
    }
}
