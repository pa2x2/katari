package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryUpdateEligibility
import mihon.entry.interactions.EntryUpdateEligibilityProcessor
import mihon.entry.interactions.EntryUpdateEligibilityRequest
import mihon.entry.interactions.EntryUpdateRestriction
import mihon.entry.interactions.EntryUpdateSkipReason
import tachiyomi.domain.entry.model.EntryStatus

internal class AnimeUpdateEligibilityProcessor : EntryUpdateEligibilityProcessor {
    override val type: EntryType = EntryType.ANIME

    override fun evaluate(request: EntryUpdateEligibilityRequest): EntryUpdateEligibility {
        val fetchWindowUpperBound = request.fetchWindowUpperBound
        return when {
            EntryUpdateRestriction.NON_COMPLETED in request.restrictions &&
                request.entry.status == EntryStatus.COMPLETED -> {
                EntryUpdateEligibility.Skipped(EntryUpdateSkipReason.COMPLETED)
            }

            EntryUpdateRestriction.HAS_UNCONSUMED in request.restrictions &&
                request.unconsumedCount != 0L -> {
                EntryUpdateEligibility.Skipped(EntryUpdateSkipReason.NOT_CAUGHT_UP)
            }

            EntryUpdateRestriction.NON_STARTED in request.restrictions &&
                request.totalCount > 0L &&
                !request.hasStarted -> {
                EntryUpdateEligibility.Skipped(EntryUpdateSkipReason.NOT_STARTED)
            }

            EntryUpdateRestriction.OUTSIDE_RELEASE_PERIOD in request.restrictions &&
                fetchWindowUpperBound != null &&
                request.entry.nextUpdate > fetchWindowUpperBound -> {
                EntryUpdateEligibility.Skipped(EntryUpdateSkipReason.OUTSIDE_RELEASE_PERIOD)
            }

            else -> EntryUpdateEligibility.Eligible
        }
    }
}
