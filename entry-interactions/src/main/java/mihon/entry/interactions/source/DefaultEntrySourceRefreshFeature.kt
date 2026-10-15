package mihon.entry.interactions.source

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.download.EntryAutomaticDownloadCoordinator
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized

internal class DefaultEntrySourceRefreshFeature(
    private val automaticDownloads: () -> EntryAutomaticDownloadCoordinator,
    private val sourceManager: SourceManager,
    private val syncEntryWithSource: SyncEntryWithSource,
    private val downloadRuntime: () -> EntryDownloadRuntimeFeature,
    private val updateLibraryTitles: (profileId: Long) -> Boolean,
) : EntrySourceRefreshFeature {

    override suspend fun refresh(request: EntrySourceRefreshRequest): EntrySourceRefreshResult {
        require(request.fetchDetails || request.fetchChildren) {
            "Source refresh must request details, children, or both"
        }

        // A library update starting with the process would otherwise fail every entry as having no source
        sourceManager.awaitInitialized()
        val sourceInstalled = sourceManager.get(request.entry.source) != null
        if (!sourceInstalled) return EntrySourceRefreshResult.SourceUnavailable(request.entry.source)

        return try {
            val sync = syncEntryWithSource.syncStrictly(
                entry = request.entry,
                profileId = request.entry.profileId,
                updateLibraryTitles = updateLibraryTitles(request.entry.profileId),
                fetchDetails = request.fetchDetails,
                fetchChapters = request.fetchChildren,
                manualFetch = request.manual,
                fetchWindow = request.fetchWindow.let { it.lowerBound to it.upperBound },
            )
            // The source no longer lists these, so their queued downloads could only fail
            sync.removedChapterIds.forEach { downloadRuntime().cancelQueued(request.entry.type, it) }
            val result = sync.toRefreshResult()
            if (request.manual && result.insertedChildren.isNotEmpty()) {
                automaticDownloads().downloadAfterEntryRefresh(request.entry, result.insertedChildren)
            }
            result
        } catch (error: CancellationException) {
            throw error
        } catch (_: NoChaptersException) {
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.NoChildren)
        } catch (error: Throwable) {
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.Operation(error))
        }
    }
}

private fun SyncEntryWithSource.SyncResult.toRefreshResult(): EntrySourceRefreshResult.Refreshed {
    return EntrySourceRefreshResult.Refreshed(
        insertedChildren = insertedChapters,
        insertedChildrenTotal = insertedChaptersTotal,
        updatedChildren = updatedChapters,
        removedChildren = removedChapters,
        metadataChanged = hasMetadataChanges,
    )
}
