package mihon.entry.interactions.download.maintenance

import mihon.entry.interactions.download.ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER
import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.entry.interactions.lifecycle.metadata.ENTRY_METADATA_CHANGED_EXECUTION_POINT
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal object EntryDownloadMetadataChangeBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.maintenance.metadata-change.behavior")
}

internal val ENTRY_DOWNLOAD_METADATA_CHANGE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.maintenance.metadata-change"),
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    point = ENTRY_METADATA_CHANGED_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
    behavioralContracts = listOf(EntryDownloadMetadataChangeBehaviorContract),
)

internal object EntryDownloadMetadataLifecycleContributor : FeatureGraphContributor {
    override val owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_DOWNLOAD_METADATA_CHANGE_PARTICIPANT)
    }
}
