package mihon.entry.interactions.state

import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import mihon.entry.interactions.runtime.toContentTypeId
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules

internal class DefaultEntryUpdateEligibilityFeature(
    private val evaluation: FeatureGraphEvaluation,
) : EntryUpdateEligibilityFeature {
    private val selectedTypes = evaluation.updateEligibilityContentTypes()

    override fun evaluate(request: EntryUpdateEligibilityRequest): EntryUpdateEligibility {
        check(request.entry.type.toContentTypeId() in selectedTypes) {
            "Entry type ${request.entry.type} was not contributed to the update eligibility feature graph"
        }

        val skipRules = request.skipRules
        val context = request.toContext()
        val reason = context.skipReason(skipRules)
        evaluation.requireUpdateEligibilityContext(request.entry.type, skipRules, context, applicable = reason == null)
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
