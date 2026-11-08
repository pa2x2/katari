package app.katari.sourcemigration.session.model

internal data class SourceMigrationRunControl(
    val stage: SourceMigrationSessionStage,
    val cancellationRequested: Boolean,
)
