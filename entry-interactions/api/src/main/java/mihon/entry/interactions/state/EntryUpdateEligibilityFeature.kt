package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryUpdateEligibilityFeature {
    fun evaluate(request: EntryUpdateEligibilityRequest): EntryUpdateEligibility
}

data class EntryUpdateEligibilityRequest(
    val entry: Entry,
    val totalCount: Long?,
    val unconsumedCount: Long?,
    val hasStarted: Boolean?,
    val fetchWindowUpperBound: Long? = null,
)

sealed interface EntryUpdateEligibility {
    data object Eligible : EntryUpdateEligibility
    data class Skipped(val reason: EntryUpdateSkipReason) : EntryUpdateEligibility
}

enum class EntryUpdateSkipReason {
    NOT_ALWAYS_UPDATE,
    COMPLETED,
    NOT_CAUGHT_UP,
    NOT_STARTED,
    OUTSIDE_RELEASE_PERIOD,
}
