package mihon.feature.migration.session.model

data class SourceMigrationSessionSummary(
    val id: SourceMigrationSessionId,
    val profileId: Long,
    val originSourceId: Long,
    val stage: SourceMigrationSessionStage,
    val updatedAt: Long,
)
