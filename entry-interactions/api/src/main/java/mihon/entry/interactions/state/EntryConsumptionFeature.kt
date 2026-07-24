package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned consumed-state eligibility, mutation, and lifecycle boundary. */
interface EntryConsumptionFeature {
    fun isApplicable(type: EntryType): Boolean

    fun canSetConsumed(
        type: EntryType,
        status: EntryConsumptionStatus,
        consumed: Boolean,
    ): Boolean

    suspend fun setConsumed(
        entry: Entry,
        children: List<EntryChapter>,
        consumed: Boolean,
    ): EntryConsumptionResult
}

sealed interface EntryConsumptionResult {
    data class Changed(val children: List<EntryChapter>) : EntryConsumptionResult

    data object NoChange : EntryConsumptionResult

    data class Inapplicable(val type: EntryType) : EntryConsumptionResult
}
