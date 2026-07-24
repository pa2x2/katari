package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionFailurePolicy
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.afterCommitVolatileFeatureExecutionPointDefinition

internal val ENTRY_METADATA_LIFECYCLE_OWNER = ContributionOwner("entry-metadata-lifecycle")
private val ENTRY_METADATA_LIFECYCLE_FEATURE_ID = FeatureId("entry.metadata-lifecycle")
private val ENTRY_METADATA_LIFECYCLE_REFERENCE = entryContentTypeReferenceContribution(
    id = "metadata-lifecycle",
    owner = ENTRY_METADATA_LIFECYCLE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Propagate persisted metadata changes",
    order = 1700,
)

internal object EntryMetadataLifecycleBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.metadata-lifecycle.behavior")
}

private object EntryMetadataLifecycleBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.metadata-lifecycle.propagation")
}

internal val ENTRY_METADATA_CHANGED_EXECUTION_POINT =
    afterCommitVolatileFeatureExecutionPointDefinition<EntryMetadataChangedEvent>(
        id = FeatureExecutionPointId("entry.metadata.changed"),
        owner = ENTRY_METADATA_LIFECYCLE_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.CONTINUE_AND_REPORT,
    )

internal object EntryMetadataLifecycleFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_METADATA_LIFECYCLE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_METADATA_LIFECYCLE_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = FeatureIntegrationId("entry.metadata-lifecycle.propagation"),
                        prerequisites = CapabilityExpression.Always,
                        behaviorProjections = listOf(EntryMetadataLifecycleBehavior),
                        behavioralContracts = listOf(EntryMetadataLifecycleBehaviorContract),
                        projectionRequirements = listOf(ENTRY_METADATA_LIFECYCLE_REFERENCE.requirement),
                        projections = listOf(ENTRY_METADATA_LIFECYCLE_REFERENCE.projection),
                    ),
                ),
            ),
        )
        sink.add(ENTRY_METADATA_CHANGED_EXECUTION_POINT)
    }
}
