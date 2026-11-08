package app.katari.sourcemigration.work

import app.katari.sourcemigration.execution.model.SourceMigrationExecutionConflict

sealed interface SourceMigrationExecutionStartResult {
    data object Started : SourceMigrationExecutionStartResult

    data class Conflicted(
        val conflicts: List<SourceMigrationExecutionConflict>,
    ) : SourceMigrationExecutionStartResult

    data object NoItems : SourceMigrationExecutionStartResult

    data object Unavailable : SourceMigrationExecutionStartResult
}
