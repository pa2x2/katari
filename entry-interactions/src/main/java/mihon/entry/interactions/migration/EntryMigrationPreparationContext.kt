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

internal val ENTRY_MIGRATION_PAIR_CONTEXT_INTEGRATION = FeatureIntegrationId("entry.migration.pair-context")
internal val ENTRY_MIGRATION_INSPECTION_CONTEXT_INTEGRATION = FeatureIntegrationId("entry.migration.inspection-context")

private object EntryMigrationPairBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.migration.target-acceptance")
}

private object EntryMigrationInspectionBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.migration.preparation")
}

internal val ENTRY_MIGRATION_TARGET_PERSISTED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.migration.target-persisted"),
    ContributionOwner("entry-state"),
)
internal val ENTRY_MIGRATION_SAME_PROFILE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.migration.same-profile"),
    ContributionOwner("entry-selection"),
)
internal val ENTRY_MIGRATION_SAME_TYPE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.migration.same-type"),
    ContributionOwner("entry-selection"),
)
internal val ENTRY_MIGRATION_DISTINCT_ENTRY_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.migration.distinct-entry"),
    ContributionOwner("entry-selection"),
)
internal val ENTRY_MIGRATION_INSPECTED_PAIR_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.migration.inspected-pair-present"),
    ContributionOwner("entry-migration-host"),
)
internal val ENTRY_MIGRATION_IDENTITY_STABLE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.migration.identity-stable"),
    ContributionOwner("entry-migration-host"),
)

private val SOURCE_UNPERSISTED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.pair-source-unpersisted"),
    listOf(ENTRY_MIGRATION_PERSISTED_CONTEXT),
)
private val SOURCE_NOT_IN_LIBRARY_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.pair-source-not-in-library"),
    listOf(ENTRY_MIGRATION_LIBRARY_MEMBERSHIP_CONTEXT),
)
private val TARGET_UNPERSISTED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.pair-target-unpersisted"),
    listOf(ENTRY_MIGRATION_TARGET_PERSISTED_CONTEXT),
)
private val PROFILE_MISMATCH_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.pair-profile-mismatch"),
    listOf(ENTRY_MIGRATION_SAME_PROFILE_CONTEXT),
)
private val TYPE_MISMATCH_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.pair-type-mismatch"),
    listOf(ENTRY_MIGRATION_SAME_TYPE_CONTEXT),
)
private val SAME_ENTRY_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.pair-same-entry"),
    listOf(ENTRY_MIGRATION_DISTINCT_ENTRY_CONTEXT),
)
private val PAIR_MISSING_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.inspected-pair-missing"),
    listOf(ENTRY_MIGRATION_INSPECTED_PAIR_CONTEXT),
)
private val IDENTITY_CHANGED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.identity-changed"),
    listOf(ENTRY_MIGRATION_IDENTITY_STABLE_CONTEXT),
)

internal fun entryMigrationPreparationContextIntegrations(
    owner: ContributionOwner,
    migration: CapabilityExpression,
): List<FeatureIntegration> = listOf(
    FeatureIntegration(
        id = ENTRY_MIGRATION_PAIR_CONTEXT_INTEGRATION,
        prerequisites = migration,
        contextInputs = listOf(
            ENTRY_MIGRATION_PERSISTED_CONTEXT,
            ENTRY_MIGRATION_LIBRARY_MEMBERSHIP_CONTEXT,
            ENTRY_MIGRATION_TARGET_PERSISTED_CONTEXT,
            ENTRY_MIGRATION_SAME_PROFILE_CONTEXT,
            ENTRY_MIGRATION_SAME_TYPE_CONTEXT,
            ENTRY_MIGRATION_DISTINCT_ENTRY_CONTEXT,
        ),
        contextRule = featureContextRule(owner) { evidence ->
            when {
                !evidence.value(ENTRY_MIGRATION_PERSISTED_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(SOURCE_UNPERSISTED_BLOCKER))
                !evidence.value(ENTRY_MIGRATION_LIBRARY_MEMBERSHIP_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(SOURCE_NOT_IN_LIBRARY_BLOCKER))
                !evidence.value(ENTRY_MIGRATION_TARGET_PERSISTED_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(TARGET_UNPERSISTED_BLOCKER))
                !evidence.value(ENTRY_MIGRATION_SAME_PROFILE_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(PROFILE_MISMATCH_BLOCKER))
                !evidence.value(ENTRY_MIGRATION_SAME_TYPE_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(TYPE_MISMATCH_BLOCKER))
                !evidence.value(ENTRY_MIGRATION_DISTINCT_ENTRY_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(SAME_ENTRY_BLOCKER))
                else -> FeatureContextDecision.Applicable
            }
        },
        contextBlockers = listOf(
            SOURCE_UNPERSISTED_BLOCKER,
            SOURCE_NOT_IN_LIBRARY_BLOCKER,
            TARGET_UNPERSISTED_BLOCKER,
            PROFILE_MISMATCH_BLOCKER,
            TYPE_MISMATCH_BLOCKER,
            SAME_ENTRY_BLOCKER,
        ),
        behaviorProjections = listOf(EntryMigrationPairBehavior),
        behavioralContracts = listOf(EntryMigrationBehaviorContract.PAIR_CONTEXT),
    ),
    FeatureIntegration(
        id = ENTRY_MIGRATION_INSPECTION_CONTEXT_INTEGRATION,
        prerequisites = migration,
        contextInputs = listOf(ENTRY_MIGRATION_INSPECTED_PAIR_CONTEXT, ENTRY_MIGRATION_IDENTITY_STABLE_CONTEXT),
        contextRule = featureContextRule(owner) { evidence ->
            when {
                !evidence.value(ENTRY_MIGRATION_INSPECTED_PAIR_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(PAIR_MISSING_BLOCKER))
                !evidence.value(ENTRY_MIGRATION_IDENTITY_STABLE_CONTEXT) ->
                    FeatureContextDecision.Blocked(listOf(IDENTITY_CHANGED_BLOCKER))
                else -> FeatureContextDecision.Applicable
            }
        },
        contextBlockers = listOf(PAIR_MISSING_BLOCKER, IDENTITY_CHANGED_BLOCKER),
        behaviorProjections = listOf(EntryMigrationInspectionBehavior),
        behavioralContracts = listOf(EntryMigrationBehaviorContract.INSPECTION_CONTEXT),
    ),
)

internal fun FeatureGraphEvaluation.requireMigrationPairContext(
    sourceType: EntryType,
    sourcePersisted: Boolean,
    sourceInLibrary: Boolean,
    targetPersisted: Boolean,
    sameProfile: Boolean,
    sameType: Boolean,
    distinctEntry: Boolean,
) {
    requireEntryContextState(
        type = sourceType,
        feature = ENTRY_MIGRATION_FEATURE_ID,
        integration = ENTRY_MIGRATION_PAIR_CONTEXT_INTEGRATION,
        behaviorProjections = listOf(EntryMigrationPairBehavior.id),
        evidence = listOf(
            contextEvidence(ENTRY_MIGRATION_PERSISTED_CONTEXT, sourcePersisted),
            contextEvidence(ENTRY_MIGRATION_LIBRARY_MEMBERSHIP_CONTEXT, sourceInLibrary),
            contextEvidence(ENTRY_MIGRATION_TARGET_PERSISTED_CONTEXT, targetPersisted),
            contextEvidence(ENTRY_MIGRATION_SAME_PROFILE_CONTEXT, sameProfile),
            contextEvidence(ENTRY_MIGRATION_SAME_TYPE_CONTEXT, sameType),
            contextEvidence(ENTRY_MIGRATION_DISTINCT_ENTRY_CONTEXT, distinctEntry),
        ),
        applicable = sourcePersisted && sourceInLibrary && targetPersisted && sameProfile && sameType && distinctEntry,
    )
}

internal fun FeatureGraphEvaluation.requireMigrationInspectionContext(
    sourceType: EntryType,
    pairPresent: Boolean,
    identityStable: Boolean,
) {
    requireEntryContextState(
        type = sourceType,
        feature = ENTRY_MIGRATION_FEATURE_ID,
        integration = ENTRY_MIGRATION_INSPECTION_CONTEXT_INTEGRATION,
        behaviorProjections = listOf(EntryMigrationInspectionBehavior.id),
        evidence = listOf(
            contextEvidence(ENTRY_MIGRATION_INSPECTED_PAIR_CONTEXT, pairPresent),
            contextEvidence(ENTRY_MIGRATION_IDENTITY_STABLE_CONTEXT, identityStable),
        ),
        applicable = pairPresent && identityStable,
    )
}
