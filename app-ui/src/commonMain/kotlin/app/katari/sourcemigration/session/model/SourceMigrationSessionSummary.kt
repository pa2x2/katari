package app.katari.sourcemigration.session.model

data class SourceMigrationSessionSummary(
    val id: SourceMigrationSessionId,
    val profileId: Long,
    val originSourceId: Long,
    val stage: SourceMigrationSessionStage,
    val updatedAt: Long,
)
