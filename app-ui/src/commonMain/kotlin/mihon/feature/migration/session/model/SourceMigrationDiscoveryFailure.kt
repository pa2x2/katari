package mihon.feature.migration.session.model

data class SourceMigrationDiscoveryFailure(
    val sessionId: SourceMigrationSessionId,
    val sourceEntryId: Long,
    val targetSourceId: Long,
    val reason: SourceMigrationDiscoveryFailureReason,
    val retryable: Boolean,
    val detail: String?,
    val updatedAt: Long,
)

enum class SourceMigrationDiscoveryFailureReason {
    SOURCE_MISSING,
    CATALOGUE_UNSUPPORTED,
    ENTRY_TYPE_UNSUPPORTED,
    SEARCH_FAILED,
}
