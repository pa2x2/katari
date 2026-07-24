package mihon.entry.interactions

sealed interface EntryMigrationAvailability {
    data object Available : EntryMigrationAvailability

    data class Unavailable(
        val reason: EntryMigrationRejection,
    ) : EntryMigrationAvailability
}

sealed interface EntryMigrationSelectionResult {
    data class Ready(
        val subjects: List<EntryMigrationSubject>,
    ) : EntryMigrationSelectionResult

    data class Rejected(
        val reason: EntryMigrationRejection,
    ) : EntryMigrationSelectionResult
}

sealed interface EntryMigrationTargetRefreshResult {
    data object Refreshed : EntryMigrationTargetRefreshResult

    data class Rejected(val reason: EntryMigrationRejection) : EntryMigrationTargetRefreshResult

    data object SourceUnavailable : EntryMigrationTargetRefreshResult

    data object NoChildren : EntryMigrationTargetRefreshResult

    data class OperationalFailure(val error: Throwable) : EntryMigrationTargetRefreshResult
}

sealed interface EntryMigrationPreparationResult {
    data class Ready(
        val reference: EntryMigrationReference,
        val source: EntryMigrationSubject,
        val target: EntryMigrationSubject,
        val availableOptions: Set<EntryMigrationOption>,
    ) : EntryMigrationPreparationResult

    data class Rejected(
        val reason: EntryMigrationRejection,
    ) : EntryMigrationPreparationResult

    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMigrationPreparationResult
}

sealed interface EntryMigrationExecutionResult {
    data class Applied(
        val outcome: EntryMigrationOutcome,
    ) : EntryMigrationExecutionResult

    data class Rejected(
        val reason: EntryMigrationRejection,
    ) : EntryMigrationExecutionResult

    /** A Feature-issued preparation no longer describes authoritative Entry state. */
    data object Conflict : EntryMigrationExecutionResult

    /** The primary transition did not commit. */
    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMigrationExecutionResult
}

data class EntryMigrationOutcome(
    val source: EntryMigrationSubject,
    val target: EntryMigrationSubject,
    val mode: EntryMigrationMode,
    val followUp: EntryMigrationFollowUp,
)

/** Reports whether non-transactional effects all completed without exposing their internal pipeline to callers. */
enum class EntryMigrationFollowUp {
    COMPLETE,
    INCOMPLETE,
}

enum class EntryMigrationRejection {
    EMPTY_SELECTION,
    UNPERSISTED_ENTRY,
    SOURCE_NOT_IN_LIBRARY,
    UNSUPPORTED_SOURCE_TYPE,
    UNSUPPORTED_TARGET_TYPE,
    MIXED_SELECTION_PROFILES,
    SOURCE_TARGET_TYPE_MISMATCH,
    SOURCE_TARGET_PROFILE_MISMATCH,
    SAME_ENTRY,
    UNRECOGNIZED_REFERENCE,
    INVALID_OPTIONS,
    ENTRY_IDENTITY_CHANGED,
}
