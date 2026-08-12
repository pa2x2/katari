package mihon.entry.interactions.anime.download

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.anime.download.model.AnimeDownloadFailure
import mihon.entry.interactions.download.EntryDownloadIdentity
import mihon.entry.interactions.download.EntryDownloadMessage
import mihon.entry.interactions.download.EntryDownloadPhase
import mihon.entry.interactions.download.EntryDownloadPresentation
import mihon.entry.interactions.download.EntryDownloadProgress
import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.download.EntryDownloadStatus
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR

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
        entryId = anime.id,
        sourceId = anime.source,
    )
}

internal fun AnimeDownload.toEntryDownloadQueueItem(): EntryDownloadQueueItem {
    val statusSnapshot = status
    val progressSnapshot = progress
    val failureSnapshot = failure
    return EntryDownloadQueueItem(
        identity = EntryDownloadIdentity.from(anime, episode),
        state = statusSnapshot.toEntryDownloadState(),
        title = anime.title,
        subtitle = episode.name,
        dateUpload = episode.dateUpload,
        chapterNumber = episode.chapterNumber,
        progress = progressSnapshot,
        progressMax = 100,
        presentation = EntryDownloadPresentation(
            phase = statusSnapshot.toEntryDownloadPhase(),
            progress = if (statusSnapshot == AnimeDownload.State.DOWNLOADING) {
                EntryDownloadProgress.Percent(progressSnapshot)
            } else {
                EntryDownloadProgress.None
            },
            failure = failureSnapshot
                ?.takeIf { statusSnapshot == AnimeDownload.State.ERROR }
                ?.toEntryDownloadMessage(),
        ),
    )
}

private fun AnimeDownload.State.toEntryDownloadPhase(): EntryDownloadPhase = when (this) {
    AnimeDownload.State.NOT_DOWNLOADED -> EntryDownloadPhase.IDLE
    AnimeDownload.State.QUEUE -> EntryDownloadPhase.QUEUED
    AnimeDownload.State.RESOLVING -> EntryDownloadPhase.RESOLVING
    AnimeDownload.State.DOWNLOADING -> EntryDownloadPhase.TRANSFERRING
    AnimeDownload.State.DOWNLOADED -> EntryDownloadPhase.COMPLETED
    AnimeDownload.State.ERROR -> EntryDownloadPhase.FAILED
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

internal fun AnimeDownloadFailure.toEntryDownloadMessage(): EntryDownloadMessage {
    val resource = when (reason) {
        AnimeDownloadFailure.Reason.SOURCE_NOT_FOUND -> MR.strings.download_notifier_source_not_available
        AnimeDownloadFailure.Reason.EPISODE_NOT_FOUND -> MR.strings.download_notifier_episode_not_found
        AnimeDownloadFailure.Reason.PREFERENCES_NOT_SUPPORTED -> MR.strings.download_notifier_preferences_not_supported
        AnimeDownloadFailure.Reason.DUB_NOT_AVAILABLE -> MR.strings.download_notifier_dub_not_available
        AnimeDownloadFailure.Reason.STREAM_NOT_AVAILABLE -> MR.strings.download_notifier_stream_not_available
        AnimeDownloadFailure.Reason.SUBTITLE_NOT_AVAILABLE -> MR.strings.download_notifier_subtitle_not_available
        AnimeDownloadFailure.Reason.QUALITY_NOT_AVAILABLE -> MR.strings.download_notifier_quality_not_available
        AnimeDownloadFailure.Reason.STREAM_EXPIRED -> MR.strings.download_notifier_stream_expired
        AnimeDownloadFailure.Reason.UNSUPPORTED_STREAM -> MR.strings.download_notifier_unsupported_stream
        AnimeDownloadFailure.Reason.INSUFFICIENT_STORAGE -> MR.strings.download_notifier_insufficient_storage
        AnimeDownloadFailure.Reason.NETWORK -> MR.strings.download_notifier_stream_network_error
        AnimeDownloadFailure.Reason.UNKNOWN -> MR.strings.download_notifier_unknown_error
    }
    return EntryDownloadMessage.Resource(resource)
}
