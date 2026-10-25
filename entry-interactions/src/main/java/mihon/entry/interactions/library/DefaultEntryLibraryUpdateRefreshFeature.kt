package mihon.entry.interactions.library

import mihon.entry.interactions.download.EntryAutomaticDownloadBatch
import mihon.entry.interactions.download.EntryAutomaticDownloadCoordinator
import mihon.entry.interactions.source.EntrySourceRefreshFailure
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.source.EntrySourceRefreshRequest
import mihon.entry.interactions.source.EntrySourceRefreshResult
import mihon.entry.interactions.source.EntrySourceRefreshWindow

internal class DefaultEntryLibraryUpdateRefreshFeature(
    private val sourceRefresh: EntrySourceRefreshFeature,
    private val automaticDownloads: () -> EntryAutomaticDownloadCoordinator,
) : EntryLibraryUpdateRefreshFeature {
    override fun newSession(): EntryLibraryUpdateRefreshSession {
        return DefaultEntryLibraryUpdateRefreshSession(this)
    }

    private suspend fun refresh(
        request: EntryLibraryUpdateRefreshRequest,
        automaticDownloadBatch: EntryAutomaticDownloadBatch,
    ): EntryLibraryUpdateRefreshResult {
        return when (
            val result = sourceRefresh.refresh(
                EntrySourceRefreshRequest(
                    entry = request.entry,
                    fetchDetails = request.fetchMetadata,
                    fetchChildren = true,
                    manual = false,
                    fetchWindow = EntrySourceRefreshWindow(
                        lowerBound = request.fetchWindowLowerBound,
                        upperBound = request.fetchWindowUpperBound,
                    ),
                ),
            )
        ) {
            is EntrySourceRefreshResult.Refreshed -> {
                val newChildren = result.insertedChildren.sortedByDescending { it.sourceOrder }
                if (newChildren.isNotEmpty()) {
                    automaticDownloadBatch.enqueue(request.entry, newChildren)
                }
                EntryLibraryUpdateRefreshResult.Updated(newChildren, result.joinedEntryIds)
            }
            is EntrySourceRefreshResult.SourceUnavailable -> EntryLibraryUpdateRefreshResult.SourceUnavailable
            is EntrySourceRefreshResult.Failed -> when (val reason = result.reason) {
                EntrySourceRefreshFailure.NoChildren -> EntryLibraryUpdateRefreshResult.NoChildren
                is EntrySourceRefreshFailure.Operation -> {
                    EntryLibraryUpdateRefreshResult.OperationalFailure(reason.error)
                }
            }
        }
    }

    private class DefaultEntryLibraryUpdateRefreshSession(
        private val feature: DefaultEntryLibraryUpdateRefreshFeature,
    ) : EntryLibraryUpdateRefreshSession {
        private val automaticDownloadBatch = feature.automaticDownloads().newLibraryUpdateBatch()
        private var completed = false

        override suspend fun refresh(request: EntryLibraryUpdateRefreshRequest): EntryLibraryUpdateRefreshResult {
            check(!completed) { "Cannot refresh after the Library update session completed" }
            return feature.refresh(request, automaticDownloadBatch)
        }

        override fun complete() {
            if (completed) return
            completed = true
            automaticDownloadBatch.complete()
        }
    }
}
