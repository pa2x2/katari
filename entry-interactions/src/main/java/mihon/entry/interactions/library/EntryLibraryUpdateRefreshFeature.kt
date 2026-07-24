package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.afterFeatureCommitVolatile

internal val ENTRY_LIBRARY_UPDATE_REFRESH_FEATURE_ID = FeatureId("entry.library-update-refresh")
internal val ENTRY_LIBRARY_UPDATE_REFRESH_OWNER = ContributionOwner("entry-library-update")
private val ENTRY_LIBRARY_UPDATE_REFRESH_REFERENCE = entryContentTypeReferenceContribution(
    id = "library-update-refresh",
    owner = ENTRY_LIBRARY_UPDATE_REFRESH_OWNER,
    section = EntryContentTypeReferenceSection.LIBRARY_AND_UPDATES,
    label = "Refresh entries during library updates",
    order = 400,
)
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
                        projectionRequirements = listOf(ENTRY_LIBRARY_UPDATE_REFRESH_REFERENCE.requirement),
                        projections = listOf(ENTRY_LIBRARY_UPDATE_REFRESH_REFERENCE.projection),
                    ),
                ),
            ),
        )
        sink.add(ENTRY_LIBRARY_UPDATE_NEW_CHILDREN_EXECUTION_POINT)
    }
}

internal class DefaultEntryLibraryUpdateRefreshFeature(
    evaluation: FeatureGraphEvaluation,
    private val sourceRefresh: EntrySourceRefreshFeature,
    private val executions: FeatureExecutionRuntime,
) : EntryLibraryUpdateRefreshFeature {
    private val selectedTypes = evaluation.libraryUpdateRefreshContentTypes()

    override fun newSession(): EntryLibraryUpdateRefreshSession {
        return DefaultEntryLibraryUpdateRefreshSession(this)
    }

    private suspend fun refresh(
        request: EntryLibraryUpdateRefreshRequest,
        session: EntryLibraryUpdateExecutionSession,
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
                    executions.afterFeatureCommitVolatile {
                        execute(
                            point = ENTRY_LIBRARY_UPDATE_NEW_CHILDREN_EXECUTION_POINT,
                            contentType = request.entry.type.toContentTypeId(),
                            event = EntryLibraryUpdateNewChildrenEvent(request.entry, newChildren, session),
                        ).throwFirstFailure()
                    }
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
        private val executionSession = EntryLibraryUpdateExecutionSession()
        private var completed = false

        override suspend fun refresh(request: EntryLibraryUpdateRefreshRequest): EntryLibraryUpdateRefreshResult {
            check(!completed) { "Cannot refresh after the Library update session completed" }
            return feature.refresh(request, executionSession)
        }

        override fun complete() {
            if (completed) return
            completed = true
            executionSession.complete()
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
        .mapTo(mutableSetOf()) { it.subject.contentType }
