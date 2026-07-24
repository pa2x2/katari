package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputDefinition
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

internal object EntryMigrationChildStateOptionBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.migration.child-state-option-availability")
}

internal val ENTRY_MIGRATION_CATEGORIES_OPTION_INTEGRATION_ID =
    FeatureIntegrationId("entry.migration.categories-option-context")

internal object EntryMigrationCategoriesOptionBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.migration.categories-option-availability")
}

internal enum class EntryMigrationContextualOption(
    val integration: FeatureIntegrationId,
    val behaviorProjection: FeatureArtifactId,
    val input: ContextInputDefinition<Boolean>,
    val blockerId: FeatureArtifactId,
    val contract: EntryMigrationBehaviorContract,
) {
    NOTES(
        integration = FeatureIntegrationId("entry.migration.notes-option-context"),
        behaviorProjection = FeatureArtifactId("entry.migration.notes-option-availability"),
        input = contextInputDefinition(
            ContextInputId("entry.migration.has-notes"),
            ContributionOwner("entry-state"),
        ),
        blockerId = FeatureArtifactId("entry.migration.no-notes"),
        contract = EntryMigrationBehaviorContract.NOTES_OPTION,
    ),
    CUSTOM_COVER(
        integration = FeatureIntegrationId("entry.migration.custom-cover-option-context"),
        behaviorProjection = FeatureArtifactId("entry.migration.custom-cover-option-availability"),
        input = contextInputDefinition(
            ContextInputId("entry.migration.has-custom-cover"),
            ContributionOwner("entry-cover-state"),
        ),
        blockerId = FeatureArtifactId("entry.migration.no-custom-cover"),
        contract = EntryMigrationBehaviorContract.CUSTOM_COVER_OPTION,
    ),
}

internal fun entryMigrationCategoriesOptionIntegration(
    migration: CapabilityExpression,
) = FeatureIntegration(
    id = ENTRY_MIGRATION_CATEGORIES_OPTION_INTEGRATION_ID,
    prerequisites = migration,
    behaviorProjections = listOf(EntryMigrationCategoriesOptionBehavior),
    behavioralContracts = listOf(EntryMigrationBehaviorContract.CATEGORIES_OPTION),
)

private data class EntryMigrationOptionDefinition(
    val blocker: FeatureContextBlocker,
)

private val OPTION_DEFINITIONS = EntryMigrationContextualOption.entries.associateWith { option ->
    EntryMigrationOptionDefinition(
        blocker = FeatureContextBlocker(
            id = option.blockerId,
            inputs = listOf(option.input),
        ),
    )
}

internal fun entryMigrationOptionContextIntegrations(
    owner: ContributionOwner,
    migration: CapabilityExpression,
): List<FeatureIntegration> = EntryMigrationContextualOption.entries.map { option ->
    val definition = OPTION_DEFINITIONS.getValue(option)
    FeatureIntegration(
        id = option.integration,
        prerequisites = migration,
        contextInputs = listOf(option.input),
        contextRule = featureContextRule(owner) { evidence ->
            if (evidence.value(option.input)) {
                FeatureContextDecision.Applicable
            } else {
                FeatureContextDecision.Blocked(listOf(definition.blocker))
            }
        },
        contextBlockers = listOf(definition.blocker),
        behaviorProjections = listOf(
            object : FeatureBehaviorProjection {
                override val id = option.behaviorProjection
            },
        ),
        behavioralContracts = listOf(option.contract),
    )
}

internal fun FeatureGraphEvaluation.requireMigrationOptionContext(
    type: EntryType,
    option: EntryMigrationContextualOption,
    available: Boolean,
) {
    requireEntryContextState(
        type = type,
        feature = ENTRY_MIGRATION_FEATURE_ID,
        integration = option.integration,
        behaviorProjections = listOf(option.behaviorProjection),
        evidence = listOf(contextEvidence(option.input, available)),
        applicable = available,
    )
}
