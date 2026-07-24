package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionOrder
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal object EntryDownloadDestructiveRemovalBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.maintenance.destructive-removal.behavior")
}

internal val ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PREPARATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.maintenance.destructive-removal.prepare"),
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    point = ENTRY_DESTRUCTIVE_REMOVING_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
    order = FeatureExecutionOrder(before = setOf(ENTRY_MERGE_DESTRUCTIVE_REMOVAL_PARTICIPANT.id)),
    behavioralContracts = listOf(EntryDownloadDestructiveRemovalBehaviorContract),
)

internal val ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.maintenance.destructive-removal.apply"),
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    point = ENTRY_DESTRUCTIVE_REMOVED_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
    behavioralContracts = listOf(EntryDownloadDestructiveRemovalBehaviorContract),
)

internal object EntryDownloadDestructiveRemovalContributor : FeatureGraphContributor {
    override val owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PREPARATION_PARTICIPANT)
        sink.add(ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PARTICIPANT)
    }
}
