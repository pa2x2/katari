package app.katari.sync.host

import app.katari.entry.interactions.download.EntryDownloadActionFeature
import app.katari.entry.interactions.source.EntrySourceRefreshFailure
import app.katari.entry.interactions.source.EntrySourceRefreshFeature
import app.katari.entry.interactions.source.EntrySourceRefreshRequest
import app.katari.entry.interactions.source.EntrySourceRefreshResult
import app.katari.sync.client.downloadrequest.DownloadRequestPerformer
import app.katari.sync.client.downloadrequest.DownloadRequestResult
import app.katari.sync.client.downloadrequest.ReceivedDownloadRequest
import app.katari.sync.client.downloadrequest.RequestedChapter
import app.katari.sync.ui.notice.DownloadRequestFailure
import app.katari.sync.ui.notice.SyncNotifications
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

/**
 * Downloads the chapters another device asked for, with this device's own download settings. A chapter not listed here
 * yet is looked for once more after refreshing the entry from its source. What can't be downloaded, for want of the
 * source or because it no longer lists the chapter, is told by notification; a refresh that failed is tried again after
 * the next sync, and errors while downloading are the download queue's to tell.
 */
@Inject
@ContributesBinding(AppScope::class)
class RequestedDownloads(
    private val entries: EntryRepository,
    private val chapters: EntryChapterRepository,
    private val sources: SourceManager,
    private val refresh: EntrySourceRefreshFeature,
    private val downloads: EntryDownloadActionFeature,
    private val notifications: SyncNotifications,
) : DownloadRequestPerformer {
    override suspend fun perform(request: ReceivedDownloadRequest): DownloadRequestResult {
        val entry = entries.getEntryById(request.entryId) ?: return DownloadRequestResult.DONE
        // Woken in the background, the app may not have loaded its sources yet; until then every source looks missing
        sources.isInitialized.first { it }
        if (sources.getDisplayInfo(entry.source).isMissing) {
            tell(entry, request, request.chapters, DownloadRequestFailure.Reason.SOURCE_MISSING)
            return DownloadRequestResult.DONE
        }
        var found = listed(entry, request.chapters)
        if (found.size < request.chapters.size) {
            val refreshed = refresh.refresh(
                EntrySourceRefreshRequest(entry, fetchDetails = false, manual = false, wholeChildList = true),
            )
            when (refreshed) {
                is EntrySourceRefreshResult.SourceUnavailable -> {
                    tell(entry, request, request.chapters, DownloadRequestFailure.Reason.SOURCE_MISSING)
                    return DownloadRequestResult.DONE
                }
                is EntrySourceRefreshResult.Failed ->
                    if (refreshed.reason is EntrySourceRefreshFailure.Operation) return DownloadRequestResult.TRY_LATER
                is EntrySourceRefreshResult.Refreshed -> Unit
            }
            found = listed(entry, request.chapters)
        }
        if (found.isNotEmpty()) downloads.download(entry, found)
        val foundUrls = found.mapTo(mutableSetOf()) { it.url }
        val gone = request.chapters.filter { it.url !in foundUrls }
        if (gone.isNotEmpty()) tell(entry, request, gone, DownloadRequestFailure.Reason.CHAPTERS_GONE)
        return DownloadRequestResult.DONE
    }

    private suspend fun listed(entry: Entry, requested: List<RequestedChapter>): List<EntryChapter> {
        val urls = requested.mapTo(mutableSetOf()) { it.url }
        return chapters.getChaptersByEntryIdAwait(entry.id).filter { it.url in urls }
    }

    private fun tell(
        entry: Entry,
        request: ReceivedDownloadRequest,
        chapters: List<RequestedChapter>,
        reason: DownloadRequestFailure.Reason,
    ) {
        notifications.showDownloadRequestFailed(
            DownloadRequestFailure(
                entryId = entry.id,
                profileId = entry.profileId,
                title = entry.title,
                from = request.fromName,
                chapters = chapters.map { it.name },
                sourceName = sources.getDisplayInfo(entry.source).name,
                reason = reason,
            ),
        )
    }
}
