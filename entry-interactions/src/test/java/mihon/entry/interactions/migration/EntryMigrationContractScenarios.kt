package mihon.entry.interactions

import mihon.feature.graph.contextEvidence

internal data class EntryMigrationContract(
    val integration: mihon.feature.graph.FeatureIntegrationId,
    val contract: EntryMigrationBehaviorContract,
    val scenario: (() -> List<mihon.feature.graph.ContextEvidence<*>>)? = null,
)

internal val EntryMigrationBehaviorContract.requiresExecution: Boolean
    get() = this in setOf(
        EntryMigrationBehaviorContract.CONSUMPTION,
        EntryMigrationBehaviorContract.BOOKMARK,
        EntryMigrationBehaviorContract.EXECUTION_CONTEXT,
    )

internal val EntryMigrationBehaviorContract.transfersChildState: Boolean
    get() = this == EntryMigrationBehaviorContract.CONSUMPTION || this == EntryMigrationBehaviorContract.BOOKMARK

internal fun migrationContracts() = buildList {
    add(EntryMigrationContract(ENTRY_MIGRATION_BASE_INTEGRATION_ID, EntryMigrationBehaviorContract.PROVIDER))
    add(
        EntryMigrationContract(
            ENTRY_MIGRATION_SOURCE_CONTEXT_INTEGRATION,
            EntryMigrationBehaviorContract.SOURCE_CONTEXT,
        ) { migrationSourceEvidence() },
    )
    add(
        EntryMigrationContract(
            ENTRY_MIGRATION_SELECTION_CONTEXT_INTEGRATION,
            EntryMigrationBehaviorContract.SELECTION_CONTEXT,
        ) {
            migrationSourceEvidence() + contextEvidence(ENTRY_MIGRATION_SINGLE_PROFILE_CONTEXT, true)
        },
    )
    add(EntryMigrationContract(ENTRY_MIGRATION_CONSUMPTION_INTEGRATION_ID, EntryMigrationBehaviorContract.CONSUMPTION))
    add(EntryMigrationContract(ENTRY_MIGRATION_BOOKMARK_INTEGRATION_ID, EntryMigrationBehaviorContract.BOOKMARK))
    add(
        EntryMigrationContract(
            ENTRY_MIGRATION_CHILD_STATE_OPTION_INTEGRATION_ID,
            EntryMigrationBehaviorContract.CHILD_STATE_OPTION,
        ),
    )
    add(
        EntryMigrationContract(ENTRY_MIGRATION_PAIR_CONTEXT_INTEGRATION, EntryMigrationBehaviorContract.PAIR_CONTEXT) {
            migrationSourceEvidence() + listOf(
                contextEvidence(ENTRY_MIGRATION_TARGET_PERSISTED_CONTEXT, true),
                contextEvidence(ENTRY_MIGRATION_SAME_PROFILE_CONTEXT, true),
                contextEvidence(ENTRY_MIGRATION_SAME_TYPE_CONTEXT, true),
                contextEvidence(ENTRY_MIGRATION_DISTINCT_ENTRY_CONTEXT, true),
            )
        },
    )
    add(
        EntryMigrationContract(
            ENTRY_MIGRATION_INSPECTION_CONTEXT_INTEGRATION,
            EntryMigrationBehaviorContract.INSPECTION_CONTEXT,
        ) {
            listOf(
                contextEvidence(ENTRY_MIGRATION_INSPECTED_PAIR_CONTEXT, true),
                contextEvidence(ENTRY_MIGRATION_IDENTITY_STABLE_CONTEXT, true),
            )
        },
    )
    EntryMigrationContextualOption.entries.forEach { option ->
        add(EntryMigrationContract(option.integration, option.contract) { listOf(contextEvidence(option.input, true)) })
    }
    add(
        EntryMigrationContract(
            ENTRY_MIGRATION_EXECUTION_CONTEXT_INTEGRATION,
            EntryMigrationBehaviorContract.EXECUTION_CONTEXT,
        ) {
            listOf(
                contextEvidence(ENTRY_MIGRATION_EXECUTION_PAIR_PRESENT_CONTEXT, true),
                contextEvidence(ENTRY_MIGRATION_AUTHORIZATION_STABLE_CONTEXT, true),
            )
        },
    )
}

private fun migrationSourceEvidence() = listOf(
    contextEvidence(ENTRY_MIGRATION_PERSISTED_CONTEXT, true),
    contextEvidence(ENTRY_MIGRATION_LIBRARY_MEMBERSHIP_CONTEXT, true),
)
