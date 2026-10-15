package mihon.entry.interactions.library

import mihon.entry.interactions.download.EntryAutomaticDownloadBatch
import mihon.entry.interactions.download.EntryAutomaticDownloadCoordinator
import mihon.entry.interactions.entryContentType
import mihon.entry.interactions.runtime.toContentTypeId
import mihon.entry.interactions.source.EntrySourceRefreshFailure
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.source.EntrySourceRefreshRequest
import mihon.entry.interactions.source.EntrySourceRefreshResult
import mihon.entry.interactions.source.EntrySourceRefreshWindow
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId

internal val ENTRY_LIBRARY_UPDATE_REFRESH_FEATURE_ID = FeatureId("entry.library-update-refresh")
internal val ENTRY_LIBRARY_UPDATE_REFRESH_OWNER = ContributionOwner("entry-library-update")
internal val ENTRY_LIBRARY_UPDATE_REFRESH_INTEGRATION =
    FeatureIntegrationId("entry.library-update-refresh.source-refresh")

private enum class EntryLibraryUpdateRefreshBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    SOURCE_REFRESH(FeatureArtifactId("entry.library-update-refresh.source-refresh")),
    NEW_CHILD_HANDOFF(FeatureArtifactId("entry.library-update-refresh.new-child-handoff")),
}

internal object EntryLibraryUpdateRefreshBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-update-refresh.behavior")
}

internal object EntryLibraryUpdateRefreshFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_LIBRARY_UPDATE_REFRESH_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_LIBRARY_UPDATE_REFRESH_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_UPDATE_REFRESH_INTEGRATION,
                        prerequisites = CapabilityExpression.Always,
                        behaviorProjections = EntryLibraryUpdateRefreshBehavior.entries,
                        behavioralContracts = listOf(EntryLibraryUpdateRefreshBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryLibraryUpdateRefreshFeature(
    evaluation: FeatureGraphEvaluation,
    private val sourceRefresh: EntrySourceRefreshFeature,
    private val automaticDownloads: () -> EntryAutomaticDownloadCoordinator,
) : EntryLibraryUpdateRefreshFeature {
    private val selectedTypes = evaluation.libraryUpdateRefreshContentTypes()

    override fun newSession(): EntryLibraryUpdateRefreshSession {
        return DefaultEntryLibraryUpdateRefreshSession(this)
    }

    private suspend fun refresh(
        request: EntryLibraryUpdateRefreshRequest,
        automaticDownloadBatch: EntryAutomaticDownloadBatch,
    ): EntryLibraryUpdateRefreshResult {
        check(request.entry.type.toContentTypeId() in selectedTypes) {
            "Entry type ${request.entry.type} was not contributed to Library Update Refresh"
        }
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
                EntryLibraryUpdateRefreshResult.Updated(newChildren)
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

private fun FeatureGraphEvaluation.libraryUpdateRefreshContentTypes(): Set<ContentTypeId> =
    behaviorProjections
        .asSequence()
        .filter { applicability ->
            applicability.subject.feature == ENTRY_LIBRARY_UPDATE_REFRESH_FEATURE_ID &&
                applicability.subject.integration == ENTRY_LIBRARY_UPDATE_REFRESH_INTEGRATION &&
                applicability.projection.id == EntryLibraryUpdateRefreshBehavior.SOURCE_REFRESH.id
        }
        .mapTo(mutableSetOf()) { it.subject.entryContentType }
