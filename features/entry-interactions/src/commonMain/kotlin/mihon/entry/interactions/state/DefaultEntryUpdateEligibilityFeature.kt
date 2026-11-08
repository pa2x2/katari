package mihon.entry.interactions.state

import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules

internal class DefaultEntryUpdateEligibilityFeature : EntryUpdateEligibilityFeature {
    override fun evaluate(request: EntryUpdateEligibilityRequest): EntryUpdateEligibility {
        val reason = request.toContext().skipReason(request.skipRules)
        return reason?.let(EntryUpdateEligibility::Skipped) ?: EntryUpdateEligibility.Eligible
    }
}

private fun EntryUpdateEligibilityRequest.toContext(): EntryUpdateEligibilityContext {
    return EntryUpdateEligibilityContext(
        oneShotAlreadyFetched = entry.updateStrategy == EntryUpdateStrategy.ONLY_FETCH_ONCE &&
            totalCount?.let { it > 0L } == true,
        completed = entry.status == EntryStatus.COMPLETED,
        hasUnconsumed = unconsumedCount?.let { it != 0L } == true,
        notStartedWithChildren = totalCount?.let { it > 0L } == true && hasStarted == false,
        outsideReleasePeriod = fetchWindowUpperBound?.let { entry.nextUpdate > it } == true,
    )
}

private fun EntryUpdateEligibilityContext.skipReason(
    skipRules: LibraryUpdateSkipRules,
): EntryUpdateSkipReason? {
    return when {
        oneShotAlreadyFetched -> EntryUpdateSkipReason.NOT_ALWAYS_UPDATE
        skipRules.skipCompleted && completed -> EntryUpdateSkipReason.COMPLETED
        skipRules.skipUnseen && hasUnconsumed -> EntryUpdateSkipReason.NOT_CAUGHT_UP
        skipRules.skipNotStarted && notStartedWithChildren -> EntryUpdateSkipReason.NOT_STARTED
        skipRules.skipOutsideReleasePeriod && outsideReleasePeriod -> EntryUpdateSkipReason.OUTSIDE_RELEASE_PERIOD
        else -> null
    }
}

private data class EntryUpdateEligibilityContext(
    val oneShotAlreadyFetched: Boolean,
    val completed: Boolean,
    val hasUnconsumed: Boolean,
    val notStartedWithChildren: Boolean,
    val outsideReleasePeriod: Boolean,
)
