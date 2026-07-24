package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal val ENTRY_MERGE_EXISTING_GROUP_CONTEXT_INTEGRATION = FeatureIntegrationId("entry.merge.existing-group-context")
private object EntryMergeExistingGroupBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.merge.existing-group-mutation")
}

internal val ENTRY_MERGE_COMPLETE_ORDERED_MEMBERSHIP_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.complete-ordered-membership"),
    ContributionOwner("entry-merge-membership"),
)
internal val ENTRY_MERGE_HOMOGENEOUS_MEMBERSHIP_TYPE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.homogeneous-membership-type"),
    ContributionOwner("entry-merge-membership"),
)

private val INCOMPLETE_ORDERED_MEMBERSHIP_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.execution-membership-incomplete"),
    listOf(ENTRY_MERGE_COMPLETE_ORDERED_MEMBERSHIP_CONTEXT),
)
private val MIXED_MEMBERSHIP_TYPES_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.execution-membership-types-mixed"),
    listOf(ENTRY_MERGE_HOMOGENEOUS_MEMBERSHIP_TYPE_CONTEXT),
)
internal fun entryMergeExecutionContextIntegrations(owner: ContributionOwner): List<FeatureIntegration> = listOf(
    FeatureIntegration(
        id = ENTRY_MERGE_EXISTING_GROUP_CONTEXT_INTEGRATION,
        prerequisites = CapabilityExpression.Always,
        contextInputs = listOf(
            ENTRY_MERGE_COMPLETE_ORDERED_MEMBERSHIP_CONTEXT,
            ENTRY_MERGE_HOMOGENEOUS_MEMBERSHIP_TYPE_CONTEXT,
        ),
        contextRule = featureContextRule(owner) { evidence ->
            when {
                !evidence.value(ENTRY_MERGE_COMPLETE_ORDERED_MEMBERSHIP_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(INCOMPLETE_ORDERED_MEMBERSHIP_BLOCKER))
                !evidence.value(ENTRY_MERGE_HOMOGENEOUS_MEMBERSHIP_TYPE_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(MIXED_MEMBERSHIP_TYPES_BLOCKER))
                else -> FeatureContextDecision.Applicable
            }
        },
        contextBlockers = listOf(INCOMPLETE_ORDERED_MEMBERSHIP_BLOCKER, MIXED_MEMBERSHIP_TYPES_BLOCKER),
        behaviorProjections = listOf(EntryMergeExistingGroupBehavior),
        behavioralContracts = listOf(EntryMergeBehaviorContract.EXISTING_GROUP),
    ),
)

internal fun FeatureGraphEvaluation.requireMergeExistingGroupContext(
    types: Set<EntryType>,
    completeOrderedMembership: Boolean,
    homogeneousMembershipType: Boolean,
) {
    types.forEach { type ->
        requireEntryContextState(
            type = type,
            feature = ENTRY_MERGE_FEATURE_ID,
            integration = ENTRY_MERGE_EXISTING_GROUP_CONTEXT_INTEGRATION,
            behaviorProjections = listOf(EntryMergeExistingGroupBehavior.id),
            evidence = listOf(
                contextEvidence(ENTRY_MERGE_COMPLETE_ORDERED_MEMBERSHIP_CONTEXT, completeOrderedMembership),
                contextEvidence(ENTRY_MERGE_HOMOGENEOUS_MEMBERSHIP_TYPE_CONTEXT, homogeneousMembershipType),
            ),
            applicable = completeOrderedMembership && homogeneousMembershipType,
        )
    }
}
