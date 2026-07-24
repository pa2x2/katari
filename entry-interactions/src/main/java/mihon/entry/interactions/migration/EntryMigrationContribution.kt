package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf
import mihon.feature.graph.anyOf

internal val ENTRY_MIGRATION_FEATURE_ID = FeatureId("entry.migration")
internal val ENTRY_MIGRATION_BASE_INTEGRATION_ID = FeatureIntegrationId("entry.migration.provider")
internal val ENTRY_MIGRATION_CONSUMPTION_INTEGRATION_ID = FeatureIntegrationId("entry.migration.consumption")
internal val ENTRY_MIGRATION_BOOKMARK_INTEGRATION_ID = FeatureIntegrationId("entry.migration.bookmarking")
internal val ENTRY_MIGRATION_CHILD_STATE_OPTION_INTEGRATION_ID =
    FeatureIntegrationId("entry.migration.child-state-option")

internal val ENTRY_MIGRATION_FEATURE_OWNER = ContributionOwner("entry-migration")
private val ENTRY_MIGRATION_REFERENCE = entryContentTypeReferenceContribution(
    id = "migration",
    owner = ENTRY_MIGRATION_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Migrate an entry to another source",
    order = 900,
)

internal enum class EntryMigrationBaseBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    PROVIDER_DISPATCH(FeatureArtifactId("entry.migration.provider-dispatch")),
    TARGET_SEARCH(FeatureArtifactId("entry.migration.target-search")),
    CONFIGURATION(FeatureArtifactId("entry.migration.configuration")),
    EXECUTION_COORDINATION(FeatureArtifactId("entry.migration.execution-coordination")),
    TARGET_SYNCHRONIZATION(FeatureArtifactId("entry.migration.target-synchronization")),
    ENTRY_STATE_TRANSFER(FeatureArtifactId("entry.migration.entry-state-transfer")),
    CATEGORY_TRANSFER(FeatureArtifactId("entry.migration.category-transfer")),
    MERGE_REPLACEMENT(FeatureArtifactId("entry.migration.merge-replacement")),
}

internal enum class EntryMigrationConsumptionBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    TRANSFER(FeatureArtifactId("entry.migration.consumption-transfer")),
}

internal enum class EntryMigrationBookmarkBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    TRANSFER(FeatureArtifactId("entry.migration.bookmark-transfer")),
}

internal enum class EntryMigrationBehaviorContract(
    override val id: FeatureArtifactId,
) : FeatureBehaviorContract {
    PROVIDER(FeatureArtifactId("entry.migration.behavior")),
    SOURCE_CONTEXT(FeatureArtifactId("entry.migration.source-context.behavior")),
    SELECTION_CONTEXT(FeatureArtifactId("entry.migration.selection-context.behavior")),
    CONSUMPTION(FeatureArtifactId("entry.migration.consumption-cooperation")),
    BOOKMARK(FeatureArtifactId("entry.migration.bookmark-cooperation")),
    CHILD_STATE_OPTION(FeatureArtifactId("entry.migration.child-state-option.behavior")),
    PAIR_CONTEXT(FeatureArtifactId("entry.migration.pair-context.behavior")),
    INSPECTION_CONTEXT(FeatureArtifactId("entry.migration.inspection-context.behavior")),
    CATEGORIES_OPTION(FeatureArtifactId("entry.migration.categories-option.behavior")),
    NOTES_OPTION(FeatureArtifactId("entry.migration.notes-option.behavior")),
    CUSTOM_COVER_OPTION(FeatureArtifactId("entry.migration.custom-cover-option.behavior")),
    EXECUTION_CONTEXT(FeatureArtifactId("entry.migration.execution-context.behavior")),
}

internal object EntryMigrationFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_MIGRATION_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        val migration = CapabilityExpression.Provided(EntryMigrationCapability.definition)
        val consumption = CapabilityExpression.Provided(EntryConsumptionCapability.definition)
        val bookmark = CapabilityExpression.Provided(EntryBookmarkCapability.definition)
        sink.add(
            FeatureContribution(
                feature = ENTRY_MIGRATION_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_MIGRATION_BASE_INTEGRATION_ID,
                        prerequisites = migration,
                        behaviorProjections = EntryMigrationBaseBehavior.entries,
                        behavioralContracts = listOf(EntryMigrationBehaviorContract.PROVIDER),
                        projectionRequirements = listOf(ENTRY_MIGRATION_REFERENCE.requirement),
                        projections = listOf(ENTRY_MIGRATION_REFERENCE.projection),
                    ),
                    entryMigrationSourceContextIntegration(owner, migration),
                    entryMigrationSelectionContextIntegration(owner, migration),
                    FeatureIntegration(
                        id = ENTRY_MIGRATION_CONSUMPTION_INTEGRATION_ID,
                        prerequisites = allOf(migration, consumption),
                        behaviorProjections = EntryMigrationConsumptionBehavior.entries,
                        behavioralContracts = listOf(EntryMigrationBehaviorContract.CONSUMPTION),
                    ),
                    FeatureIntegration(
                        id = ENTRY_MIGRATION_BOOKMARK_INTEGRATION_ID,
                        prerequisites = allOf(migration, bookmark),
                        behaviorProjections = EntryMigrationBookmarkBehavior.entries,
                        behavioralContracts = listOf(EntryMigrationBehaviorContract.BOOKMARK),
                    ),
                    FeatureIntegration(
                        id = ENTRY_MIGRATION_CHILD_STATE_OPTION_INTEGRATION_ID,
                        prerequisites = allOf(migration, anyOf(consumption, bookmark)),
                        behaviorProjections = listOf(EntryMigrationChildStateOptionBehavior),
                        behavioralContracts = listOf(EntryMigrationBehaviorContract.CHILD_STATE_OPTION),
                    ),
                    entryMigrationCategoriesOptionIntegration(migration),
                ) +
                    entryMigrationPreparationContextIntegrations(owner, migration) +
                    entryMigrationOptionContextIntegrations(owner, migration) +
                    listOf(entryMigrationExecutionContextIntegration(owner, migration)),
            ),
        )
        sink.add(ENTRY_MIGRATION_DURABLE_EXECUTION_POINT)
        sink.add(ENTRY_MIGRATION_OPTION_DISCOVERY_POINT)
        sink.add(ENTRY_MIGRATION_TRANSITION_PREPARING_POINT)
    }
}
