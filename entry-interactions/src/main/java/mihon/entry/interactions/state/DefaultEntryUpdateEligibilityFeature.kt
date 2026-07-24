package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.EntryStatus

internal class DefaultEntryUpdateEligibilityFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val currentPolicy: () -> EntryUpdateEligibilityPolicy,
) : EntryUpdateEligibilityFeature {
    private val selectedTypes = evaluation.updateEligibilityContentTypes()

    override fun evaluate(request: EntryUpdateEligibilityRequest): EntryUpdateEligibility {
        check(request.entry.type.toContentTypeId() in selectedTypes) {
            "Entry type ${request.entry.type} was not contributed to the update eligibility feature graph"
        }

        val policy = currentPolicy()
        val context = request.toContext()
        val reason = context.skipReason(policy)
        evaluation.requireUpdateEligibilityContext(request.entry.type, policy, context, applicable = reason == null)
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
    policy: EntryUpdateEligibilityPolicy,
): EntryUpdateSkipReason? {
    return when {
        oneShotAlreadyFetched -> EntryUpdateSkipReason.NOT_ALWAYS_UPDATE
        policy.skipCompleted && completed -> EntryUpdateSkipReason.COMPLETED
        policy.skipWhenUnconsumed && hasUnconsumed -> EntryUpdateSkipReason.NOT_CAUGHT_UP
        policy.skipWhenNotStarted && notStartedWithChildren -> EntryUpdateSkipReason.NOT_STARTED
        policy.skipOutsideReleasePeriod && outsideReleasePeriod -> EntryUpdateSkipReason.OUTSIDE_RELEASE_PERIOD
        else -> null
    }
}
