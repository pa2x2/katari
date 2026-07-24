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

internal val ENTRY_MERGE_SELECTION_CONTEXT_INTEGRATION =
    FeatureIntegrationId("entry.merge.preparation-selection-context")
internal val ENTRY_MERGE_AUTHORITY_CONTEXT_INTEGRATION =
    FeatureIntegrationId("entry.merge.preparation-authority-context")
internal val ENTRY_MERGE_MEMBERSHIP_CONTEXT_INTEGRATION =
    FeatureIntegrationId("entry.merge.preparation-membership-context")

private object EntryMergeSelectionBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.merge.preparation-selection")
}

private object EntryMergeAuthorityBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.merge.preparation-authority")
}

private object EntryMergeEditorBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.merge.editor")
}

internal val ENTRY_MERGE_HOMOGENEOUS_TYPE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.homogeneous-selection-type"),
    ContributionOwner("entry-selection"),
)
internal val ENTRY_MERGE_HOMOGENEOUS_PROFILE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.homogeneous-selection-profile"),
    ContributionOwner("entry-selection"),
)
internal val ENTRY_MERGE_AUTHORITATIVE_SELECTION_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.authoritative-selection-present"),
    ContributionOwner("entry-merge-host"),
)
internal val ENTRY_MERGE_AUTHORITATIVE_TYPE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.authoritative-selection-type-stable"),
    ContributionOwner("entry-merge-host"),
)
internal val ENTRY_MERGE_AUTHORITATIVE_PROFILE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.authoritative-selection-profile-stable"),
    ContributionOwner("entry-merge-host"),
)
internal val ENTRY_MERGE_SINGLE_EXISTING_GROUP_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.single-existing-group"),
    ContributionOwner("entry-merge-membership"),
)
internal val ENTRY_MERGE_COMPLETE_EXISTING_GROUP_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.complete-existing-group"),
    ContributionOwner("entry-merge-membership"),
)
internal val ENTRY_MERGE_SUFFICIENT_EDITOR_MEMBERS_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.merge.sufficient-editor-members"),
    ContributionOwner("entry-merge-membership"),
)

private val MIXED_TYPES_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.mixed-selection-types"),
    listOf(ENTRY_MERGE_HOMOGENEOUS_TYPE_CONTEXT),
)
private val MIXED_PROFILES_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.mixed-selection-profiles"),
    listOf(ENTRY_MERGE_HOMOGENEOUS_PROFILE_CONTEXT),
)
private val AUTHORITATIVE_SELECTION_MISSING_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.authoritative-selection-missing"),
    listOf(ENTRY_MERGE_AUTHORITATIVE_SELECTION_CONTEXT),
)
private val AUTHORITATIVE_TYPE_CHANGED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.authoritative-selection-type-changed"),
    listOf(ENTRY_MERGE_AUTHORITATIVE_TYPE_CONTEXT),
)
private val AUTHORITATIVE_PROFILE_CHANGED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.authoritative-selection-profile-changed"),
    listOf(ENTRY_MERGE_AUTHORITATIVE_PROFILE_CONTEXT),
)
private val MULTIPLE_EXISTING_GROUPS_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.multiple-existing-groups"),
    listOf(ENTRY_MERGE_SINGLE_EXISTING_GROUP_CONTEXT),
)
private val INCOMPLETE_EXISTING_GROUP_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.incomplete-existing-group"),
    listOf(ENTRY_MERGE_COMPLETE_EXISTING_GROUP_CONTEXT),
)
private val TOO_FEW_EDITOR_MEMBERS_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.merge.too-few-editor-members"),
    listOf(ENTRY_MERGE_SUFFICIENT_EDITOR_MEMBERS_CONTEXT),
)

internal fun entryMergePreparationContextIntegrations(owner: ContributionOwner): List<FeatureIntegration> = listOf(
    FeatureIntegration(
        id = ENTRY_MERGE_SELECTION_CONTEXT_INTEGRATION,
        prerequisites = CapabilityExpression.Always,
        contextInputs = listOf(ENTRY_MERGE_HOMOGENEOUS_TYPE_CONTEXT, ENTRY_MERGE_HOMOGENEOUS_PROFILE_CONTEXT),
        contextRule = featureContextRule(owner) { evidence ->
            when {
                !evidence.value(ENTRY_MERGE_HOMOGENEOUS_TYPE_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(MIXED_TYPES_BLOCKER))
                !evidence.value(ENTRY_MERGE_HOMOGENEOUS_PROFILE_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(MIXED_PROFILES_BLOCKER))
                else -> FeatureContextDecision.Applicable
            }
        },
        contextBlockers = listOf(MIXED_TYPES_BLOCKER, MIXED_PROFILES_BLOCKER),
        behaviorProjections = listOf(EntryMergeSelectionBehavior),
        behavioralContracts = listOf(EntryMergeBehaviorContract.PREPARATION_SELECTION),
    ),
    FeatureIntegration(
        id = ENTRY_MERGE_AUTHORITY_CONTEXT_INTEGRATION,
        prerequisites = CapabilityExpression.Always,
        contextInputs = listOf(
            ENTRY_MERGE_AUTHORITATIVE_SELECTION_CONTEXT,
            ENTRY_MERGE_AUTHORITATIVE_TYPE_CONTEXT,
            ENTRY_MERGE_AUTHORITATIVE_PROFILE_CONTEXT,
        ),
        contextRule = featureContextRule(owner) { evidence ->
            when {
                !evidence.value(ENTRY_MERGE_AUTHORITATIVE_SELECTION_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(AUTHORITATIVE_SELECTION_MISSING_BLOCKER))
                !evidence.value(ENTRY_MERGE_AUTHORITATIVE_TYPE_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(AUTHORITATIVE_TYPE_CHANGED_BLOCKER))
                !evidence.value(ENTRY_MERGE_AUTHORITATIVE_PROFILE_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(AUTHORITATIVE_PROFILE_CHANGED_BLOCKER))
                else -> FeatureContextDecision.Applicable
            }
        },
        contextBlockers = listOf(
            AUTHORITATIVE_SELECTION_MISSING_BLOCKER,
            AUTHORITATIVE_TYPE_CHANGED_BLOCKER,
            AUTHORITATIVE_PROFILE_CHANGED_BLOCKER,
        ),
        behaviorProjections = listOf(EntryMergeAuthorityBehavior),
        behavioralContracts = listOf(EntryMergeBehaviorContract.PREPARATION_AUTHORITY),
    ),
    FeatureIntegration(
        id = ENTRY_MERGE_MEMBERSHIP_CONTEXT_INTEGRATION,
        prerequisites = CapabilityExpression.Always,
        contextInputs = listOf(
            ENTRY_MERGE_SINGLE_EXISTING_GROUP_CONTEXT,
            ENTRY_MERGE_COMPLETE_EXISTING_GROUP_CONTEXT,
            ENTRY_MERGE_SUFFICIENT_EDITOR_MEMBERS_CONTEXT,
        ),
        contextRule = featureContextRule(owner) { evidence ->
            when {
                !evidence.value(ENTRY_MERGE_SINGLE_EXISTING_GROUP_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(MULTIPLE_EXISTING_GROUPS_BLOCKER))
                !evidence.value(ENTRY_MERGE_COMPLETE_EXISTING_GROUP_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(INCOMPLETE_EXISTING_GROUP_BLOCKER))
                !evidence.value(ENTRY_MERGE_SUFFICIENT_EDITOR_MEMBERS_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(TOO_FEW_EDITOR_MEMBERS_BLOCKER))
                else -> FeatureContextDecision.Applicable
            }
        },
        contextBlockers = listOf(
            MULTIPLE_EXISTING_GROUPS_BLOCKER,
            INCOMPLETE_EXISTING_GROUP_BLOCKER,
            TOO_FEW_EDITOR_MEMBERS_BLOCKER,
        ),
        behaviorProjections = listOf(EntryMergeEditorBehavior),
        behavioralContracts = listOf(EntryMergeBehaviorContract.PREPARATION_MEMBERSHIP),
    ),
)

internal fun FeatureGraphEvaluation.requireMergeSelectionContext(
    types: Set<EntryType>,
    homogeneousType: Boolean,
    homogeneousProfile: Boolean,
) {
    types.forEach { type ->
        requireEntryContextState(
            type = type,
            feature = ENTRY_MERGE_FEATURE_ID,
            integration = ENTRY_MERGE_SELECTION_CONTEXT_INTEGRATION,
            behaviorProjections = listOf(EntryMergeSelectionBehavior.id),
            evidence = listOf(
                contextEvidence(ENTRY_MERGE_HOMOGENEOUS_TYPE_CONTEXT, homogeneousType),
                contextEvidence(ENTRY_MERGE_HOMOGENEOUS_PROFILE_CONTEXT, homogeneousProfile),
            ),
            applicable = homogeneousType && homogeneousProfile,
        )
    }
}

internal fun FeatureGraphEvaluation.requireMergeAuthorityContext(
    type: EntryType,
    authoritativeSelectionPresent: Boolean,
    typeStable: Boolean,
    profileStable: Boolean,
) {
    requireEntryContextState(
        type = type,
        feature = ENTRY_MERGE_FEATURE_ID,
        integration = ENTRY_MERGE_AUTHORITY_CONTEXT_INTEGRATION,
        behaviorProjections = listOf(EntryMergeAuthorityBehavior.id),
        evidence = listOf(
            contextEvidence(ENTRY_MERGE_AUTHORITATIVE_SELECTION_CONTEXT, authoritativeSelectionPresent),
            contextEvidence(ENTRY_MERGE_AUTHORITATIVE_TYPE_CONTEXT, typeStable),
            contextEvidence(ENTRY_MERGE_AUTHORITATIVE_PROFILE_CONTEXT, profileStable),
        ),
        applicable = authoritativeSelectionPresent && typeStable && profileStable,
    )
}

internal fun FeatureGraphEvaluation.requireMergeMembershipContext(
    type: EntryType,
    singleExistingGroup: Boolean,
    completeExistingGroup: Boolean,
    sufficientEditorMembers: Boolean,
) {
    requireEntryContextState(
        type = type,
        feature = ENTRY_MERGE_FEATURE_ID,
        integration = ENTRY_MERGE_MEMBERSHIP_CONTEXT_INTEGRATION,
        behaviorProjections = listOf(EntryMergeEditorBehavior.id),
        evidence = listOf(
            contextEvidence(ENTRY_MERGE_SINGLE_EXISTING_GROUP_CONTEXT, singleExistingGroup),
            contextEvidence(ENTRY_MERGE_COMPLETE_EXISTING_GROUP_CONTEXT, completeExistingGroup),
            contextEvidence(ENTRY_MERGE_SUFFICIENT_EDITOR_MEMBERS_CONTEXT, sufficientEditorMembers),
        ),
        applicable = singleExistingGroup && completeExistingGroup && sufficientEditorMembers,
    )
}
