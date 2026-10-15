package mihon.entry.interactions.migration

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal val ENTRY_MIGRATION_EXECUTION_CONTEXT_INTEGRATION = FeatureIntegrationId("entry.migration.execution-context")

private object EntryMigrationExecutionBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.migration.execution")
}

internal val ENTRY_MIGRATION_EXECUTION_PAIR_PRESENT_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.migration.execution-pair-present"),
    ContributionOwner("entry-migration-host"),
)
internal val ENTRY_MIGRATION_AUTHORIZATION_STABLE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.migration.execution-authorization-stable"),
    ContributionOwner("entry-migration-host"),
)
private val PAIR_MISSING_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.execution-pair-missing"),
    listOf(ENTRY_MIGRATION_EXECUTION_PAIR_PRESENT_CONTEXT),
)
private val AUTHORIZATION_CHANGED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.migration.execution-authorization-changed"),
    listOf(ENTRY_MIGRATION_AUTHORIZATION_STABLE_CONTEXT),
)

internal fun entryMigrationExecutionContextIntegration(
    owner: ContributionOwner,
    migration: CapabilityExpression,
) = FeatureIntegration(
    id = ENTRY_MIGRATION_EXECUTION_CONTEXT_INTEGRATION,
    prerequisites = migration,
    contextInputs = listOf(
        ENTRY_MIGRATION_EXECUTION_PAIR_PRESENT_CONTEXT,
        ENTRY_MIGRATION_AUTHORIZATION_STABLE_CONTEXT,
    ),
    contextRule = featureContextRule(owner) { evidence ->
        when {
            !evidence.value(ENTRY_MIGRATION_EXECUTION_PAIR_PRESENT_CONTEXT) ->
                FeatureContextDecision.Blocked(listOf(PAIR_MISSING_BLOCKER))
            !evidence.value(ENTRY_MIGRATION_AUTHORIZATION_STABLE_CONTEXT) ->
                FeatureContextDecision.Blocked(listOf(AUTHORIZATION_CHANGED_BLOCKER))
            else -> FeatureContextDecision.Applicable
        }
    },
    contextBlockers = listOf(PAIR_MISSING_BLOCKER, AUTHORIZATION_CHANGED_BLOCKER),
    behaviorProjections = listOf(EntryMigrationExecutionBehavior),
    behavioralContracts = listOf(EntryMigrationBehaviorContract.EXECUTION_CONTEXT),
)
