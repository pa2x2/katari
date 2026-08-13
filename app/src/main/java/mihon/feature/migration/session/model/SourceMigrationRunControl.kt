package mihon.feature.migration.session.model

internal data class SourceMigrationRunControl(
    val stage: SourceMigrationSessionStage,
    val cancellationRequested: Boolean,
)
