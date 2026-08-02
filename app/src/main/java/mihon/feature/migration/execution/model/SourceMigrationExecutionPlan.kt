package mihon.feature.migration.execution.model

import mihon.feature.migration.session.model.SourceMigrationSessionItem

sealed interface SourceMigrationExecutionPlanResult {
    data class Ready(
        val items: List<SourceMigrationSessionItem>,
    ) : SourceMigrationExecutionPlanResult

    data class Conflicted(
        val conflicts: List<SourceMigrationExecutionConflict>,
    ) : SourceMigrationExecutionPlanResult

    data object NoItems : SourceMigrationExecutionPlanResult
}

data class SourceMigrationExecutionConflict(
    val sourceEntryIds: Set<Long>,
    val reason: SourceMigrationExecutionConflictReason,
)

enum class SourceMigrationExecutionConflictReason {
    TARGET_IS_ANOTHER_REPLACED_ENTRY,
    SHARED_TARGET_ACROSS_GROUPS,
    OVERLAPPING_EXTERNAL_GROUP,
    TARGET_MISSING,
}
