package mihon.feature.migration.execution

sealed interface SourceMigrationExecutionRunResult {
    data class Completed(
        val migratedItems: Int,
        val attentionItems: Int,
        val totalItems: Int,
    ) : SourceMigrationExecutionRunResult

    data object Paused : SourceMigrationExecutionRunResult

    data object SessionMissing : SourceMigrationExecutionRunResult

    data object NoWork : SourceMigrationExecutionRunResult
}
