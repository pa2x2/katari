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
import mihon.feature.graph.transactionalFeatureExecutionPointDefinition

internal val ENTRY_DESTRUCTIVE_REMOVAL_OWNER = ContributionOwner("entry-destructive-removal")
private val ENTRY_DESTRUCTIVE_REMOVAL_REFERENCE = entryContentTypeReferenceContribution(
    id = "destructive-removal",
    owner = ENTRY_DESTRUCTIVE_REMOVAL_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Remove entries and their owned state",
    order = 1800,
)

internal object EntryDestructiveRemovalBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.destructive-removal.behavior")
}

private object EntryDestructiveRemovalBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.destructive-removal.workflow")
}

internal val ENTRY_DESTRUCTIVE_REMOVING_EXECUTION_POINT =
    transactionalFeatureExecutionPointDefinition<EntryDestructiveRemovingEvent>(
        id = FeatureExecutionPointId("entry.destructive-removal.removing"),
        owner = ENTRY_DESTRUCTIVE_REMOVAL_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )

internal val ENTRY_DESTRUCTIVE_REMOVED_EXECUTION_POINT =
    afterCommitVolatileFeatureExecutionPointDefinition<EntryDestructiveRemovedEvent>(
        id = FeatureExecutionPointId("entry.destructive-removal.removed"),
        owner = ENTRY_DESTRUCTIVE_REMOVAL_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.CONTINUE_AND_REPORT,
    )

internal object EntryDestructiveRemovalFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_DESTRUCTIVE_REMOVAL_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = FeatureId("entry.destructive-removal"),
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = FeatureIntegrationId("entry.destructive-removal.workflow"),
                        prerequisites = CapabilityExpression.Always,
                        behaviorProjections = listOf(EntryDestructiveRemovalBehavior),
                        behavioralContracts = listOf(EntryDestructiveRemovalBehaviorContract),
                        projectionRequirements = listOf(ENTRY_DESTRUCTIVE_REMOVAL_REFERENCE.requirement),
                        projections = listOf(ENTRY_DESTRUCTIVE_REMOVAL_REFERENCE.projection),
                    ),
                ),
            ),
        )
        sink.add(ENTRY_DESTRUCTIVE_REMOVING_EXECUTION_POINT)
        sink.add(ENTRY_DESTRUCTIVE_REMOVED_EXECUTION_POINT)
    }
}
