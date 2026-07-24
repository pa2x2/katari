package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned gate for finding and opening the next child of an Entry. */
interface EntryContinueFeature {
    fun isApplicable(type: EntryType): Boolean

    suspend fun nextTarget(entry: Entry): EntryContinueTargetResult

    suspend fun continueEntry(context: Context, entry: Entry): EntryContinueResult
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
