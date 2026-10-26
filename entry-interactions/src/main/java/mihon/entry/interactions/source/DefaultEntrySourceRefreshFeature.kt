package mihon.entry.interactions.source

import eu.kanade.tachiyomi.source.entry.SEntry
import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.download.EntryAutomaticDownloadCoordinator
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.merge.EntryMergeSourceGroupFeature
import mihon.entry.interactions.merge.EntryMergeSourceGroupResult
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.interactor.FinishChapterListWalk
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized

internal class DefaultEntrySourceRefreshFeature(
    private val automaticDownloads: () -> EntryAutomaticDownloadCoordinator,
    private val sourceManager: SourceManager,
    private val syncEntryWithSource: SyncEntryWithSource,
    private val downloadRuntime: () -> EntryDownloadRuntimeFeature,
    private val updateLibraryTitles: (profileId: Long) -> Boolean,
    private val networkToLocalEntry: NetworkToLocalEntry,
    private val mergeSourceGroups: () -> EntryMergeSourceGroupFeature,
    private val finishChapterListWalk: FinishChapterListWalk,
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
                firstFetchIsBackCatalogue = request.firstFetchIsBackCatalogue,
            )
            if (request.wholeChildList && sync.olderChaptersPending) finishChapterListWalk(request.entry)
            val joinedEntryIds = sync.mergedEntries
                ?.let { listed -> syncMergedEntries(request.entry, listed) }
                .orEmpty()
            // The source no longer lists these, so their queued downloads could only fail
            sync.removedChapterIds.forEach { downloadRuntime().cancelQueued(request.entry.type, it) }
            val result = sync.toRefreshResult(joinedEntryIds)
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

    private suspend fun syncMergedEntries(root: Entry, listed: List<SEntry>): Set<Long> {
        val members = networkToLocalEntry(listed.map { it.toEntry(root.source) }, root.profileId)
        // Storing keeps a Library entry's title, but a member is named by its listing
        val named = members.zip(listed) { member, listedMember -> member.copy(title = listedMember.title) }
        val result = mergeSourceGroups().sync(root, named)
        check(result is EntryMergeSourceGroupResult.Applied) { "Merged entry members couldn't be applied: $result" }
        return result.joinedEntryIds
    }
}

private fun SyncEntryWithSource.SyncResult.toRefreshResult(
    joinedEntryIds: Set<Long>,
): EntrySourceRefreshResult.Refreshed {
    return EntrySourceRefreshResult.Refreshed(
        insertedChildren = insertedChapters,
        insertedChildrenTotal = insertedChaptersTotal,
        updatedChildren = updatedChapters,
        removedChildren = removedChapters,
        metadataChanged = hasMetadataChanges,
        joinedEntryIds = joinedEntryIds,
    )
}
