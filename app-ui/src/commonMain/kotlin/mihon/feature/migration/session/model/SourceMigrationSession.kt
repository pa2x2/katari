package mihon.feature.migration.session.model

import mihon.entry.interactions.migration.EntryMigrationOption

@JvmInline
value class SourceMigrationSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Source Migration session ID must not be blank" }
    }
}

enum class SourceMigrationSessionStage {
    DRAFT,
    DISCOVERY_QUEUED,
    DISCOVERING,
    DISCOVERY_PAUSED,
    REVIEW_REQUIRED,
    EXECUTION_QUEUED,
    EXECUTING,
    EXECUTION_PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED,
    ;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED || this == FAILED
}

data class SourceMigrationSession(
    val id: SourceMigrationSessionId,
    val profileId: Long,
    val originSourceId: Long,
    val stage: SourceMigrationSessionStage,
    val targetSourceIds: List<Long>,
    val selectedOptions: Set<EntryMigrationOption>,
    val cancellationRequested: Boolean,
    val errorCode: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val groups: List<SourceMigrationSessionGroup>,
    val items: List<SourceMigrationSessionItem>,
) {
    val includedReadyCount: Int
        get() = items.count(SourceMigrationSessionItem::isIncludedAndReady)
}
