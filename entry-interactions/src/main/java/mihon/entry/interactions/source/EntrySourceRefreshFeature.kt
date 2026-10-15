package mihon.entry.interactions.source

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.download.EntryAutomaticDownloadCoordinator
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized

internal val ENTRY_SOURCE_REFRESH_FEATURE_ID = FeatureId("entry.source-refresh")
internal val ENTRY_SOURCE_REFRESH_OWNER = ContributionOwner("entry-source-refresh")
internal val ENTRY_SOURCE_REFRESH_INTEGRATION_ID = FeatureIntegrationId("entry.source-refresh.execution")

internal val ENTRY_SOURCE_REFRESH_SOURCE_CONTEXT = contextInputDefinition<Boolean>(
    id = ContextInputId("entry.source-refresh.source-state"),
    owner = ENTRY_SOURCE_CONTEXT_OWNER,
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
                    ),
                ),
            ),
        )
    }
}

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
