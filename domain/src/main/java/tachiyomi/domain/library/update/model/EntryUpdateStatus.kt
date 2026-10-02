package tachiyomi.domain.library.update.model

data class EntryUpdateStatus(
    val entryId: Long,
    /** Start of the library update that last decided about this entry. */
    val decidedAt: Long,
    val outcome: EntryUpdateOutcome,
    /** Why the entry was skipped or not checked; null for checked entries. */
    val reason: EntryUpdateDecisionReason?,
    /** The category whose switch or rules made the decision, when one did. */
    val reasonCategoryId: Long?,
    val error: String?,
    val newChapters: Int,
    val lastCheckedAt: Long?,
    val consecutiveFailures: Int,
) {
    val isFailingRepeatedly: Boolean
        get() = consecutiveFailures >= REPEATED_FAILURE_THRESHOLD

    companion object {
        const val REPEATED_FAILURE_THRESHOLD = 3
    }
}

enum class EntryUpdateOutcome {
    NEW_CHAPTERS,
    NO_CHANGES,
    FAILED,
    SKIPPED,
    NOT_CHECKED,
}

/** The step that kept an entry out of an update, in the order the steps are evaluated. */
enum class EntryUpdateDecisionReason {
    ENTRY_NEVER,
    CATEGORY_OFF,
    SOURCE_OFF,
    TYPE_OFF,

    /** The entry's category checks less often than the update ran. */
    NOT_DUE,
    FETCH_ONCE,
    COMPLETED,
    HAS_UNSEEN,
    NOT_STARTED,
    OUTSIDE_RELEASE_PERIOD,
}
