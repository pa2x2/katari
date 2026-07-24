package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSelection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.source.entrySourceContextInputDefinition
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.afterFeatureCommitVolatile
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.source.service.SourceManager

internal val ENTRY_SOURCE_REFRESH_FEATURE_ID = FeatureId("entry.source-refresh")
internal val ENTRY_SOURCE_REFRESH_OWNER = ContributionOwner("entry-source-refresh")
private val ENTRY_SOURCE_REFRESH_REFERENCE = entryContentTypeReferenceContribution(
    id = "source-refresh",
    owner = ENTRY_SOURCE_REFRESH_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Refresh entry details and child items from a source",
    order = 900,
    selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)
internal val ENTRY_SOURCE_REFRESH_INTEGRATION_ID = FeatureIntegrationId("entry.source-refresh.execution")

internal val ENTRY_SOURCE_REFRESH_SOURCE_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.source-refresh.source-state"),
    contracts = setOf(UnifiedSource::class),
)
private val ENTRY_SOURCE_REFRESH_SOURCE_MISSING = FeatureContextBlocker(
    FeatureArtifactId("entry.source-refresh.source-missing"),
    listOf(ENTRY_SOURCE_REFRESH_SOURCE_CONTEXT),
)
private enum class EntrySourceRefreshBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    SOURCE_ACCESS(FeatureArtifactId("entry.source-refresh.source-access")),
    DETAILS(FeatureArtifactId("entry.source-refresh.details")),
    CHILDREN(FeatureArtifactId("entry.source-refresh.children")),
    PERSISTENCE(FeatureArtifactId("entry.source-refresh.persistence")),
    RESULT(FeatureArtifactId("entry.source-refresh.result")),
}

internal object EntrySourceRefreshBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.source-refresh.behavior")
}

internal object EntrySourceRefreshFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_SOURCE_REFRESH_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_SOURCE_REFRESH_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_SOURCE_REFRESH_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(ENTRY_SOURCE_REFRESH_SOURCE_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            if (evidence.value(ENTRY_SOURCE_REFRESH_SOURCE_CONTEXT)) {
                                FeatureContextDecision.Applicable
                            } else {
                                FeatureContextDecision.Blocked(listOf(ENTRY_SOURCE_REFRESH_SOURCE_MISSING))
                            }
                        },
                        contextBlockers = listOf(ENTRY_SOURCE_REFRESH_SOURCE_MISSING),
                        behaviorProjections = EntrySourceRefreshBehavior.entries,
                        behavioralContracts = listOf(EntrySourceRefreshBehaviorContract),
                        projectionRequirements = listOf(ENTRY_SOURCE_REFRESH_REFERENCE.requirement),
                        projections = listOf(ENTRY_SOURCE_REFRESH_REFERENCE.projection),
                    ),
                ),
            ),
        )
        sink.add(ENTRY_SOURCE_REFRESH_NEW_CHILDREN_EXECUTION_POINT)
    }
}

internal class DefaultEntrySourceRefreshFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val executions: FeatureExecutionRuntime,
    private val sourceManager: SourceManager,
    private val syncEntryWithSource: SyncEntryWithSource,
    private val updateLibraryTitles: (profileId: Long) -> Boolean,
) : EntrySourceRefreshFeature {

    override suspend fun refresh(request: EntrySourceRefreshRequest): EntrySourceRefreshResult {
        require(request.fetchDetails || request.fetchChildren) {
            "Source refresh must request details, children, or both"
        }

        val sourceInstalled = sourceManager.get(request.entry.source) != null
        requireState(request, sourceInstalled)
        if (!sourceInstalled) return EntrySourceRefreshResult.SourceUnavailable(request.entry.source)

        return try {
            val result = syncEntryWithSource.syncStrictly(
                entry = request.entry,
                profileId = request.entry.profileId,
                updateLibraryTitles = updateLibraryTitles(request.entry.profileId),
                fetchDetails = request.fetchDetails,
                fetchChapters = request.fetchChildren,
                manualFetch = request.manual,
                fetchWindow = request.fetchWindow.let { it.lowerBound to it.upperBound },
            ).toRefreshResult()
            if (request.manual && result.insertedChildren.isNotEmpty()) {
                executions.afterFeatureCommitVolatile {
                    execute(
                        point = ENTRY_SOURCE_REFRESH_NEW_CHILDREN_EXECUTION_POINT,
                        contentType = request.entry.type.toContentTypeId(),
                        event = EntrySourceRefreshNewChildrenEvent(request.entry, result.insertedChildren),
                    ).throwFirstFailure()
                }
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

    private fun requireState(request: EntrySourceRefreshRequest, sourceInstalled: Boolean) {
        EntrySourceRefreshBehavior.entries.forEach { behavior ->
            evaluation.requireSourceContextState(
                feature = ENTRY_SOURCE_REFRESH_FEATURE_ID,
                integration = ENTRY_SOURCE_REFRESH_INTEGRATION_ID,
                behaviorProjection = behavior.id,
                evidence = listOf(contextEvidence(ENTRY_SOURCE_REFRESH_SOURCE_CONTEXT, sourceInstalled)),
                applicable = sourceInstalled,
                contentType = request.entry.type,
            )
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
