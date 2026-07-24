package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned boundary for child-group discovery, filtering, and exclusion persistence. */
interface EntryChildGroupFilterFeature {
    fun isApplicable(type: EntryType): Boolean

    suspend fun state(scope: EntryChildGroupFilterScope): EntryChildGroupFilterStateResult

    fun observe(scope: EntryChildGroupFilterScope): EntryChildGroupFilterObservationResult

    fun filter(
        entry: Entry,
        chapters: List<EntryChapter>,
        excludedGroups: Set<String>,
    ): EntryChildGroupFilterResult

    suspend fun setExcludedGroups(
        scope: EntryChildGroupFilterScope,
        excludedGroups: Set<String>,
    ): EntryChildGroupFilterMutationResult

    suspend fun snapshot(profileId: Long, entry: Entry): EntryChildGroupFilterSnapshotResult

    suspend fun restore(entry: Entry, excludedGroups: Set<String>): EntryChildGroupFilterRestoreResult
}

data class EntryChildGroupFilterScope(
    val entry: Entry,
    val memberIds: List<Long>,
)

data class EntryChildGroupFilterState(
    val availableGroups: Set<String>,
    val excludedGroups: Set<String>,
) {
    val active: Boolean
        get() = availableGroups.any(excludedGroups::contains)
}

sealed interface EntryChildGroupFilterStateResult {
    data class Available(val state: EntryChildGroupFilterState) : EntryChildGroupFilterStateResult
    data class Inapplicable(val type: EntryType) : EntryChildGroupFilterStateResult
}

sealed interface EntryChildGroupFilterObservationResult {
    data class Available(
        val states: Flow<EntryChildGroupFilterState>,
    ) : EntryChildGroupFilterObservationResult

    data class Inapplicable(val type: EntryType) : EntryChildGroupFilterObservationResult
}

sealed interface EntryChildGroupFilterResult {
    data class Available(val chapters: List<EntryChapter>) : EntryChildGroupFilterResult
    data class Inapplicable(val type: EntryType) : EntryChildGroupFilterResult
}

sealed interface EntryChildGroupFilterMutationResult {
    data class Applied(val memberCount: Int) : EntryChildGroupFilterMutationResult
    data object NoChange : EntryChildGroupFilterMutationResult
    data class Inapplicable(val type: EntryType) : EntryChildGroupFilterMutationResult
}

sealed interface EntryChildGroupFilterSnapshotResult {
    data class Available(val excludedGroups: Set<String>) : EntryChildGroupFilterSnapshotResult
    data class Inapplicable(val type: EntryType) : EntryChildGroupFilterSnapshotResult
}

sealed interface EntryChildGroupFilterRestoreResult {
    data class Restored(val memberCount: Int) : EntryChildGroupFilterRestoreResult
    data object NoChange : EntryChildGroupFilterRestoreResult
    data class Inapplicable(val type: EntryType) : EntryChildGroupFilterRestoreResult
}
