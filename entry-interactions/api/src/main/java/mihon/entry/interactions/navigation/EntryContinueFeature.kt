package mihon.entry.interactions.navigation

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

/** Feature-owned gate for finding and opening the next child of an Entry. */
interface EntryContinueFeature {
    fun isApplicable(type: EntryType): Boolean

    suspend fun nextTarget(entry: Entry): EntryContinueTargetResult

    suspend fun nextTargets(entries: List<Entry>): Map<Long, EntryContinueTargetResult> {
        return entries.associate { entry -> entry.id to nextTarget(entry) }
    }

    suspend fun continueEntry(context: Context, entry: Entry): EntryContinueResult
}

interface SeededEntryContinueBatchFeature {
    suspend fun nextTargets(
        entries: List<Entry>,
        seed: EntryContinueBatchSeed,
    ): Map<Long, EntryContinueTargetResult>
}

data class EntryContinueBatchSeed(
    val completeOwnerIds: Set<Long>,
    val chapters: List<EntryChapter>,
    val progressStates: List<EntryProgressState>,
) {
    init {
        require(chapters.all { it.entryId in completeOwnerIds }) {
            "Seeded Continue chapters must belong to complete owners"
        }
        require(progressStates.all { it.entryId in completeOwnerIds }) {
            "Seeded Continue progress must belong to complete owners"
        }
    }
}

sealed interface EntryContinueTargetResult {
    data object Inapplicable : EntryContinueTargetResult
    data object NoNext : EntryContinueTargetResult
    data class Available(val chapter: EntryChapter) : EntryContinueTargetResult
}

sealed interface EntryContinueResult {
    data object Inapplicable : EntryContinueResult

    data object NoNext : EntryContinueResult

    data class Opened(val chapter: EntryChapter) : EntryContinueResult
}
