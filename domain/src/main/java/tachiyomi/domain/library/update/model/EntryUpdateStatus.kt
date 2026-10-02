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

/**
 * The step that kept an entry out of an update, in the order the steps are evaluated.
 *
 * Settings that turn checking off for an entry leave it [EntryUpdateOutcome.NOT_CHECKED]. Skip rules, which a user may
 * want to override for one run, leave it [EntryUpdateOutcome.SKIPPED].
 */
enum class EntryUpdateDecisionReason(val outcome: EntryUpdateOutcome) {
    ENTRY_NEVER(EntryUpdateOutcome.NOT_CHECKED),
    CATEGORY_OFF(EntryUpdateOutcome.NOT_CHECKED),
    SOURCE_OFF(EntryUpdateOutcome.NOT_CHECKED),
    TYPE_OFF(EntryUpdateOutcome.NOT_CHECKED),
    FETCH_ONCE(EntryUpdateOutcome.SKIPPED),
    COMPLETED(EntryUpdateOutcome.SKIPPED),
    HAS_UNSEEN(EntryUpdateOutcome.SKIPPED),
    NOT_STARTED(EntryUpdateOutcome.SKIPPED),
    OUTSIDE_RELEASE_PERIOD(EntryUpdateOutcome.SKIPPED),

    /** An automatic update ran sooner than the entry's categories ask to be checked. */
    NOT_DUE(EntryUpdateOutcome.NOT_CHECKED),
}
