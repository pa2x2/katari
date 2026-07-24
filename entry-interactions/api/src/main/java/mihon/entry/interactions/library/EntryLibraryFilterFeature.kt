package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.core.common.preference.TriState

/** Feature-owned boundary for Library filter availability, policy, and matching. */
interface EntryLibraryFilterFeature {
    fun filter(request: EntryLibraryFilterRequest): EntryLibraryFilterResult
}

data class EntryLibraryFilterRequest(
    val targets: List<EntryLibraryFilterTarget>,
    val policy: EntryLibraryFilterPolicy,
)

data class EntryLibraryFilterTarget(
    val type: EntryType,
    val isDownloadedOrLocal: Boolean,
    val hasUnconsumed: Boolean?,
    val hasStarted: Boolean?,
    val hasBookmarks: Boolean?,
    val isCompleted: Boolean,
    val isOutsideReleasePeriod: Boolean,
    val trackerIds: Set<Long>,
)

data class EntryLibraryFilterPolicy(
    val downloadedOnly: Boolean,
    val downloaded: TriState,
    val unconsumed: TriState,
    val notStarted: TriState,
    val bookmarked: TriState,
    val completed: TriState,
    val outsideReleasePeriod: TriState,
    val outsideReleasePeriodEnabled: Boolean,
    val tracking: Map<Long, TriState>,
)

data class EntryLibraryFilterResult(
    val includedTargetIndices: List<Int>,
    val hasActiveFilters: Boolean,
    val availability: EntryLibraryFilterAvailability,
)

data class EntryLibraryFilterAvailability(
    val progressSummary: EntryLibraryFilterControlAvailability,
    val bookmarking: EntryLibraryFilterControlAvailability,
    val outsideReleasePeriod: EntryLibraryFilterControlAvailability,
)

data class EntryLibraryFilterControlAvailability(
    val applicableTypes: Set<EntryType>,
    val inapplicableTypes: Set<EntryType>,
) {
    val isAvailable: Boolean
        get() = applicableTypes.isNotEmpty()
}
