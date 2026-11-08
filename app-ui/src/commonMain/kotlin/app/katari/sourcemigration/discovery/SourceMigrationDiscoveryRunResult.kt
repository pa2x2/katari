package app.katari.sourcemigration.discovery

sealed interface SourceMigrationDiscoveryRunResult {
    data class ReviewRequired(
        val completedItems: Int,
        val totalItems: Int,
    ) : SourceMigrationDiscoveryRunResult

    data object Paused : SourceMigrationDiscoveryRunResult

    data object SessionMissing : SourceMigrationDiscoveryRunResult

    data object NoWork : SourceMigrationDiscoveryRunResult
}
