package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import tachiyomi.domain.entry.model.Entry

internal val ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_ID = FeatureId("entry.download.maintenance")
internal val ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER = ContributionOwner("entry-download-maintenance")
private val ENTRY_DOWNLOAD_MAINTENANCE_REFERENCE = entryContentTypeReferenceContribution(
    id = "download-maintenance",
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DOWNLOADS,
    label = "Maintain downloads when entries or sources change",
    order = 700,
)
internal val ENTRY_DOWNLOAD_MAINTENANCE_INTEGRATION_ID =
    FeatureIntegrationId("entry.download.maintenance.provider")

internal object EntryDownloadMaintenanceBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.maintenance.provider-behavior")
}

internal object EntryDownloadLibraryRemovalBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.maintenance.library-removal.behavior")
}

internal val ENTRY_DOWNLOAD_LIBRARY_REMOVAL_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.maintenance.library-removal"),
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    point = ENTRY_LIBRARY_REMOVED_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
    behavioralContracts = listOf(EntryDownloadLibraryRemovalBehaviorContract),
)

private val ENTRY_DOWNLOAD_CACHE_MAINTENANCE_BEHAVIOR_ID =
    FeatureArtifactId("entry.download.maintenance.cache")
private val ENTRY_DOWNLOAD_SOURCE_MAINTENANCE_BEHAVIOR_ID =
    FeatureArtifactId("entry.download.maintenance.source")
private val ENTRY_DOWNLOAD_TITLE_MAINTENANCE_BEHAVIOR_ID =
    FeatureArtifactId("entry.download.maintenance.title")
private val ENTRY_DOWNLOAD_REMOVAL_MAINTENANCE_BEHAVIOR_ID =
    FeatureArtifactId("entry.download.maintenance.removal")

private object EntryDownloadCacheMaintenanceBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_DOWNLOAD_CACHE_MAINTENANCE_BEHAVIOR_ID
}

private object EntryDownloadSourceMaintenanceBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_DOWNLOAD_SOURCE_MAINTENANCE_BEHAVIOR_ID
}

private object EntryDownloadTitleMaintenanceBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_DOWNLOAD_TITLE_MAINTENANCE_BEHAVIOR_ID
}

private object EntryDownloadRemovalMaintenanceBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_DOWNLOAD_REMOVAL_MAINTENANCE_BEHAVIOR_ID
}

internal object EntryDownloadMaintenanceFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_MAINTENANCE_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
                        behaviorProjections = listOf(
                            EntryDownloadCacheMaintenanceBehavior,
                            EntryDownloadSourceMaintenanceBehavior,
                            EntryDownloadTitleMaintenanceBehavior,
                            EntryDownloadRemovalMaintenanceBehavior,
                        ),
                        behavioralContracts = listOf(EntryDownloadMaintenanceBehaviorContract),
                        projectionRequirements = listOf(ENTRY_DOWNLOAD_MAINTENANCE_REFERENCE.requirement),
                        projections = listOf(ENTRY_DOWNLOAD_MAINTENANCE_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal object EntryDownloadLibraryMembershipContributor : FeatureGraphContributor {
    override val owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_DOWNLOAD_LIBRARY_REMOVAL_PARTICIPANT)
    }
}

internal class DefaultEntryDownloadMaintenanceFeature(
    evaluation: FeatureGraphEvaluation,
    private val interaction: EntryDownloadInteraction,
    private val ownership: EntryMergeDownloadOwnershipProjection,
) : EntryDownloadMaintenanceFeature {
    private val cacheTypes = evaluation.applicableProviderTypes<EntryDownloadProcessor>(
        feature = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_ID,
        integration = ENTRY_DOWNLOAD_MAINTENANCE_INTEGRATION_ID,
        behaviorProjection = ENTRY_DOWNLOAD_CACHE_MAINTENANCE_BEHAVIOR_ID,
    )
    private val sourceTypes = evaluation.applicableProviderTypes<EntryDownloadProcessor>(
        feature = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_ID,
        integration = ENTRY_DOWNLOAD_MAINTENANCE_INTEGRATION_ID,
        behaviorProjection = ENTRY_DOWNLOAD_SOURCE_MAINTENANCE_BEHAVIOR_ID,
    )
    private val titleTypes = evaluation.applicableProviderTypes<EntryDownloadProcessor>(
        feature = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_ID,
        integration = ENTRY_DOWNLOAD_MAINTENANCE_INTEGRATION_ID,
        behaviorProjection = ENTRY_DOWNLOAD_TITLE_MAINTENANCE_BEHAVIOR_ID,
    )
    private val removalTypes = evaluation.applicableProviderTypes<EntryDownloadProcessor>(
        feature = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_ID,
        integration = ENTRY_DOWNLOAD_MAINTENANCE_INTEGRATION_ID,
        behaviorProjection = ENTRY_DOWNLOAD_REMOVAL_MAINTENANCE_BEHAVIOR_ID,
    )

    init {
        check(setOf(cacheTypes, sourceTypes, titleTypes, removalTypes).size == 1) {
            "Download maintenance behaviors selected different provider sets"
        }
    }

    override fun invalidateCaches(): EntryDownloadMaintenanceResult {
        if (cacheTypes.isEmpty()) return EntryDownloadMaintenanceResult.NoParticipants
        interaction.invalidateCaches()
        return EntryDownloadMaintenanceResult.Performed
    }

    override fun renameSource(
        oldSource: UnifiedSource,
        newSource: UnifiedSource,
    ): EntryDownloadMaintenanceResult {
        if (sourceTypes.isEmpty()) return EntryDownloadMaintenanceResult.NoParticipants
        interaction.renameSource(oldSource, newSource)
        return EntryDownloadMaintenanceResult.Performed
    }

    override suspend fun renameEntry(entry: Entry, newTitle: String): EntryDownloadMaintenanceResult {
        if (entry.type !in titleTypes) return inapplicable(entry.type)
        interaction.renameEntry(entry, newTitle)
        return EntryDownloadMaintenanceResult.Performed
    }

    override suspend fun inspectEntry(entry: Entry): EntryDownloadMaintenanceInspection {
        if (entry.type !in removalTypes) {
            return EntryDownloadMaintenanceInspection.Inapplicable(entry.type)
        }
        val owners = ownership.resolveDownloadOwners(entry.mergeSubject()).orderedOwners
        return if (owners.any(interaction::hasDownloads)) {
            EntryDownloadMaintenanceInspection.HasDownloads
        } else {
            EntryDownloadMaintenanceInspection.NoDownloads
        }
    }

    override suspend fun removeEntryDownloads(entry: Entry): EntryDownloadMaintenanceResult {
        return when (val preparation = prepareRemoval(entry)) {
            is EntryDownloadRemovalPreparation.Prepared -> applyRemoval(preparation.plan)
            EntryDownloadRemovalPreparation.NothingToRemove -> EntryDownloadMaintenanceResult.Performed
            is EntryDownloadRemovalPreparation.Inapplicable -> inapplicable(preparation.type)
        }
    }

    override suspend fun prepareRemoval(entry: Entry): EntryDownloadRemovalPreparation {
        if (entry.type !in removalTypes) return EntryDownloadRemovalPreparation.Inapplicable(entry.type)
        val owners = ownership.resolveDownloadOwners(entry.mergeSubject()).orderedOwners
            .filter(interaction::hasDownloads)
        return if (owners.isEmpty()) {
            EntryDownloadRemovalPreparation.NothingToRemove
        } else {
            EntryDownloadRemovalPreparation.Prepared(EntryDownloadRemovalPlan(owners))
        }
    }

    override suspend fun applyRemoval(plan: EntryDownloadRemovalPlan): EntryDownloadMaintenanceResult {
        val remaining = plan.owners.filter { owner ->
            !interaction.deleteEntryDownloads(owner) || interaction.hasDownloads(owner)
        }
        return if (remaining.isEmpty()) {
            EntryDownloadMaintenanceResult.Performed
        } else {
            EntryDownloadMaintenanceResult.Incomplete(remaining)
        }
    }

    private fun inapplicable(type: EntryType): EntryDownloadMaintenanceResult {
        return inapplicable(setOf(type))
    }

    private fun inapplicable(types: Set<EntryType>) = EntryDownloadMaintenanceResult.Inapplicable(types)
}

private fun Entry.mergeSubject() = EntryMergeSubject(profileId, id)
