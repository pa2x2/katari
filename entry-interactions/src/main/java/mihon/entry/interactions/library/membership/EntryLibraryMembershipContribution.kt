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

internal val ENTRY_LIBRARY_MEMBERSHIP_FEATURE_ID = FeatureId("entry.library-membership")
internal val ENTRY_LIBRARY_MEMBERSHIP_OWNER = ContributionOwner("entry-library-membership")
private val ENTRY_LIBRARY_MEMBERSHIP_INTEGRATION_ID = FeatureIntegrationId("entry.library-membership.workflow")

private val ENTRY_LIBRARY_MEMBERSHIP_REFERENCE = entryContentTypeReferenceContribution(
    id = "library-membership",
    owner = ENTRY_LIBRARY_MEMBERSHIP_OWNER,
    section = EntryContentTypeReferenceSection.LIBRARY_AND_UPDATES,
    label = "Add to and remove from the Library",
    order = 100,
)

internal object EntryLibraryMembershipBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-membership.behavior")
}

private object EntryLibraryMembershipBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.library-membership.workflow")
}

internal val ENTRY_LIBRARY_ADDED_EXECUTION_POINT =
    afterCommitVolatileFeatureExecutionPointDefinition<EntryLibraryAddedEvent>(
        id = FeatureExecutionPointId("entry.library-membership.added"),
        owner = ENTRY_LIBRARY_MEMBERSHIP_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.CONTINUE_AND_REPORT,
    )

internal val ENTRY_LIBRARY_REMOVING_EXECUTION_POINT =
    transactionalFeatureExecutionPointDefinition<EntryLibraryRemovingEvent>(
        id = FeatureExecutionPointId("entry.library-membership.removing"),
        owner = ENTRY_LIBRARY_MEMBERSHIP_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )

internal val ENTRY_LIBRARY_REMOVED_EXECUTION_POINT =
    afterCommitVolatileFeatureExecutionPointDefinition<EntryLibraryRemovedEvent>(
        id = FeatureExecutionPointId("entry.library-membership.removed"),
        owner = ENTRY_LIBRARY_MEMBERSHIP_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.CONTINUE_AND_REPORT,
    )

internal object EntryLibraryMembershipFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_LIBRARY_MEMBERSHIP_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_LIBRARY_MEMBERSHIP_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_MEMBERSHIP_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        behaviorProjections = listOf(EntryLibraryMembershipBehavior),
                        behavioralContracts = listOf(EntryLibraryMembershipBehaviorContract),
                        projectionRequirements = listOf(ENTRY_LIBRARY_MEMBERSHIP_REFERENCE.requirement),
                        projections = listOf(ENTRY_LIBRARY_MEMBERSHIP_REFERENCE.projection),
                    ),
                ),
            ),
        )
        sink.add(ENTRY_LIBRARY_ADDED_EXECUTION_POINT)
        sink.add(ENTRY_LIBRARY_REMOVING_EXECUTION_POINT)
        sink.add(ENTRY_LIBRARY_REMOVED_EXECUTION_POINT)
    }
}
