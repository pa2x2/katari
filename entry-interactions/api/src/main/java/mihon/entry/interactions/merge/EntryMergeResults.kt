package mihon.entry.interactions

sealed interface EntryMergePreparationResult {
    data class Ready(
        val editor: EntryMergeEditorProjection,
    ) : EntryMergePreparationResult

    data class Rejected(
        val reason: EntryMergeRejection,
    ) : EntryMergePreparationResult
}

sealed interface EntryMergeExecutionResult {
    data class Applied(
        val outcome: EntryMergeWorkflowOutcome,
    ) : EntryMergeExecutionResult

    data class Rejected(
        val reason: EntryMergeRejection,
    ) : EntryMergeExecutionResult

    /** The Merge-feature-issued edit reference no longer describes authoritative state. */
    data object Conflict : EntryMergeExecutionResult

    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMergeExecutionResult
}

/** Optional navigation target produced by the completed workflow; it exposes no group or member list. */
data class EntryMergeWorkflowOutcome(
    val visibleSubject: EntryMergeSubject?,
    val followUp: EntryMergeFollowUp,
)

enum class EntryMergeFollowUp {
    COMPLETE,
    PENDING,
}

enum class EntryMergeRejection {
    EMPTY_SELECTION,
    TOO_FEW_ENTRIES,
    DUPLICATE_ENTRIES,
    MIXED_ENTRY_TYPES,
    MIXED_PROFILES,
    MULTIPLE_EXISTING_GROUPS,
    UNRECOGNIZED_EDIT_REFERENCE,
    ENTRY_NOT_IN_EDITOR,
    TARGET_REMOVED,
    INVALID_ORDER,
    PREPARATION_MISSING,
}
