package mihon.entry.interactions.child

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned boundary for discovering, filtering out and remembering excluded scanlators. */
interface EntryScanlatorFilterFeature {
    fun isApplicable(type: EntryType): Boolean

    suspend fun state(scope: EntryScanlatorFilterScope): EntryScanlatorFilterStateResult

    fun observe(scope: EntryScanlatorFilterScope): EntryScanlatorFilterObservationResult

    fun filter(
        entry: Entry,
        chapters: List<EntryChapter>,
        excludedScanlators: Set<String>,
    ): EntryScanlatorFilterResult

    suspend fun setExcludedScanlators(
        scope: EntryScanlatorFilterScope,
        excludedScanlators: Set<String>,
    ): EntryScanlatorFilterMutationResult

    /** The result for each of [entries] by entry id. */
    suspend fun snapshot(profileId: Long, entries: List<Entry>): Map<Long, EntryScanlatorFilterSnapshotResult>

    suspend fun restore(
        profileId: Long,
        entry: Entry,
        excludedScanlators: Set<String>,
    ): EntryScanlatorFilterRestoreResult
}

data class EntryScanlatorFilterScope(
    val entry: Entry,
    val memberIds: List<Long>,
)

data class EntryScanlatorFilterState(
    val availableScanlators: Set<String>,
    val excludedScanlators: Set<String>,
) {
    val active: Boolean
        get() = availableScanlators.any(excludedScanlators::contains)
}

sealed interface EntryScanlatorFilterStateResult {
    data class Available(val state: EntryScanlatorFilterState) : EntryScanlatorFilterStateResult
    data class Inapplicable(val type: EntryType) : EntryScanlatorFilterStateResult
}

sealed interface EntryScanlatorFilterObservationResult {
    data class Available(
        val states: Flow<EntryScanlatorFilterState>,
    ) : EntryScanlatorFilterObservationResult

    data class Inapplicable(val type: EntryType) : EntryScanlatorFilterObservationResult
}

sealed interface EntryScanlatorFilterResult {
    data class Available(val chapters: List<EntryChapter>) : EntryScanlatorFilterResult
    data class Inapplicable(val type: EntryType) : EntryScanlatorFilterResult
}

sealed interface EntryScanlatorFilterMutationResult {
    data class Applied(val memberCount: Int) : EntryScanlatorFilterMutationResult
    data object NoChange : EntryScanlatorFilterMutationResult
    data class Inapplicable(val type: EntryType) : EntryScanlatorFilterMutationResult
}

sealed interface EntryScanlatorFilterSnapshotResult {
    data class Available(val excludedScanlators: Set<String>) : EntryScanlatorFilterSnapshotResult
    data class Inapplicable(val type: EntryType) : EntryScanlatorFilterSnapshotResult
}

sealed interface EntryScanlatorFilterRestoreResult {
    data class Restored(val memberCount: Int) : EntryScanlatorFilterRestoreResult
    data object NoChange : EntryScanlatorFilterRestoreResult
    data class Inapplicable(val type: EntryType) : EntryScanlatorFilterRestoreResult
}
