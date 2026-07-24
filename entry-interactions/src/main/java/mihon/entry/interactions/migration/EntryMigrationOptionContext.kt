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

internal enum class EntryMigrationContextualOption(
    val integration: FeatureIntegrationId,
    val behaviorProjection: FeatureArtifactId,
    val input: ContextInputDefinition<Boolean>,
    val blockerId: FeatureArtifactId,
    val contract: EntryMigrationBehaviorContract,
) {
    CATEGORIES(
        integration = FeatureIntegrationId("entry.migration.categories-option-context"),
        behaviorProjection = FeatureArtifactId("entry.migration.categories-option-availability"),
        input = contextInputDefinition(
            ContextInputId("entry.migration.has-categories"),
            ContributionOwner("entry-category-state"),
        ),
        blockerId = FeatureArtifactId("entry.migration.no-categories"),
        contract = EntryMigrationBehaviorContract.CATEGORIES_OPTION,
    ),
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
