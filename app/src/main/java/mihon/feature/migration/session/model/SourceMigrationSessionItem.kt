package mihon.feature.migration.session.model

import mihon.entry.interactions.migration.EntryMigrationOperationKey
import mihon.entry.interactions.migration.EntryMigrationOption

enum class SourceMigrationItemState {
    DISCOVERY_QUEUED,
    DISCOVERING,
    READY,
    NEEDS_REVIEW,
    NO_MATCH,
    DISCOVERY_FAILED,
    CONFLICT,
    EXECUTION_QUEUED,
    EXECUTING,
    APPLIED,
    APPLIED_INCOMPLETE,
    EXECUTION_FAILED,
    CANCELLED,
}

enum class SourceMigrationMatchKind {
    EXACT,
    SIMILAR,
    MANUAL,
}

data class SourceMigrationSessionItem(
    val sessionId: SourceMigrationSessionId,
    val sourceEntryId: Long,
    val position: Long,
    val sourceId: Long,
    val sourceTitle: String,
    val sourceUrl: String,
    val sourceThumbnailUrl: String?,
    val state: SourceMigrationItemState,
    val included: Boolean,
    val selectedTargetEntryId: Long?,
    val targetSourceId: Long?,
    val targetTitle: String?,
    val targetUrl: String?,
    val targetThumbnailUrl: String?,
    val matchKind: SourceMigrationMatchKind?,
    val operationKey: EntryMigrationOperationKey,
    val availableOptions: Set<EntryMigrationOption>,
    val discoveryAttempts: Long,
    val executionAttempts: Long,
    val errorCode: String?,
    val errorMessage: String?,
    val updatedAt: Long,
) {
    val isIncludedAndReady: Boolean
        get() = included && state == SourceMigrationItemState.READY
}
