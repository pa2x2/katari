package mihon.feature.migration.work

import mihon.feature.migration.execution.model.SourceMigrationExecutionConflict

sealed interface SourceMigrationExecutionStartResult {
    data object Started : SourceMigrationExecutionStartResult

    data class Conflicted(
        val conflicts: List<SourceMigrationExecutionConflict>,
    ) : SourceMigrationExecutionStartResult

    data object NoItems : SourceMigrationExecutionStartResult

    data object Unavailable : SourceMigrationExecutionStartResult
}
