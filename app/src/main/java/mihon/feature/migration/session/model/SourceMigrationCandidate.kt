package mihon.feature.migration.session.model

data class SourceMigrationCandidate(
    val sessionId: SourceMigrationSessionId,
    val sourceEntryId: Long,
    val targetSourceId: Long,
    val targetTitle: String,
    val targetUrl: String,
    val rank: Long,
    val score: Double?,
    val matchKind: SourceMigrationMatchKind,
    val discoveredAt: Long,
)
