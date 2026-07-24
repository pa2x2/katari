package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.core.common.preference.TriState

internal class DefaultEntryLibraryFilterFeature(
    private val evaluation: FeatureGraphEvaluation,
) : EntryLibraryFilterFeature {
    private val selection = evaluation.libraryFilterSelection()

    override fun filter(request: EntryLibraryFilterRequest): EntryLibraryFilterResult {
        val currentTypes = request.targets.mapTo(mutableSetOf(), EntryLibraryFilterTarget::type)
        val uncomposedTypes = currentTypes.filterTo(mutableSetOf()) {
            it.toContentTypeId() !in selection.participatingContentTypes
        }
        check(uncomposedTypes.isEmpty()) {
            "Entry types $uncomposedTypes were not contributed to the Library filtering feature graph"
        }

        evaluation.requireLibraryFilterContext(
            types = currentTypes,
            policy = request.policy,
            state = EntryLibraryFilterStateContext(
                targetCount = request.targets.size,
                hasUnknownProgress = request.targets.any { it.hasUnconsumed == null || it.hasStarted == null },
                hasUnknownBookmarks = request.targets.any { it.hasBookmarks == null },
                hasDownloadedOrLocal = request.targets.any { it.isDownloadedOrLocal },
                hasOutsideReleasePeriod = request.targets.any { it.isOutsideReleasePeriod },
            ),
            tracking = EntryLibraryTrackingFilterContext(
                configuredTrackerIds = request.policy.tracking.keys,
                targetTrackerIds = request.targets.flatMapTo(mutableSetOf()) { it.trackerIds },
            ),
        )

        val availability = EntryLibraryFilterAvailability(
            progressSummary = currentTypes.controlAvailability(selection.progressTypes),
            bookmarking = currentTypes.controlAvailability(selection.bookmarkTypes),
            outsideReleasePeriod = currentTypes.controlAvailability(selection.releasePeriodTypes),
        )
        val policy = request.policy
        val effectiveDownloaded = if (policy.downloadedOnly) TriState.ENABLED_IS else policy.downloaded
        val activeTracking = policy.tracking.filterValues { it != TriState.DISABLED }
        val unconsumedFilter = policy.unconsumed.takeIf {
            availability.progressSummary.isAvailable
        } ?: TriState.DISABLED
        val notStartedFilter = policy.notStarted.takeIf {
            availability.progressSummary.isAvailable
        } ?: TriState.DISABLED
        val bookmarkFilter = policy.bookmarked.takeIf { availability.bookmarking.isAvailable } ?: TriState.DISABLED
        val releasePeriodFilter = policy.outsideReleasePeriod.takeIf {
            policy.outsideReleasePeriodEnabled && availability.outsideReleasePeriod.isAvailable
        } ?: TriState.DISABLED

        val included = request.targets.mapIndexedNotNull { index, target ->
            index.takeIf {
                effectiveDownloaded.matches(target.isDownloadedOrLocal) &&
                    unconsumedFilter.matches(target.hasUnconsumed) &&
                    notStartedFilter.matches(target.hasStarted?.not()) &&
                    bookmarkFilter.matches(target.hasBookmarks) &&
                    policy.completed.matches(target.isCompleted) &&
                    releasePeriodFilter.matchesReleasePeriod(target, selection.releasePeriodTypes) &&
                    activeTracking.matches(target.trackerIds)
            }
        }

        return EntryLibraryFilterResult(
            includedTargetIndices = included,
            hasActiveFilters = listOf(
                effectiveDownloaded,
                unconsumedFilter,
                notStartedFilter,
                bookmarkFilter,
                policy.completed,
                releasePeriodFilter,
                *activeTracking.values.toTypedArray(),
            ).any { it != TriState.DISABLED },
            availability = availability,
        )
    }
}

private fun Set<EntryType>.controlAvailability(
    supportedTypes: Set<EntryType>,
): EntryLibraryFilterControlAvailability {
    return EntryLibraryFilterControlAvailability(
        applicableTypes = intersect(supportedTypes),
        inapplicableTypes = subtract(supportedTypes),
    )
}

private fun TriState.matches(value: Boolean?): Boolean {
    return when (this) {
        TriState.DISABLED -> true
        TriState.ENABLED_IS -> value == true
        TriState.ENABLED_NOT -> value == false
    }
}

private fun TriState.matchesReleasePeriod(
    target: EntryLibraryFilterTarget,
    applicableTypes: Set<EntryType>,
): Boolean {
    if (target.type !in applicableTypes) return true
    return matches(target.isOutsideReleasePeriod)
}

private fun Map<Long, TriState>.matches(targetTrackerIds: Set<Long>): Boolean {
    if (isEmpty()) return true
    val excluded = filterValues { it == TriState.ENABLED_NOT }.keys
    val included = filterValues { it == TriState.ENABLED_IS }.keys
    return excluded.intersect(targetTrackerIds).isEmpty() &&
        (included.isEmpty() || included.intersect(targetTrackerIds).isNotEmpty())
}
