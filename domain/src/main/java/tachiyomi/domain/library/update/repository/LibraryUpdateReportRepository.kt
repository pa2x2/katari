package tachiyomi.domain.library.update.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger

/**
 * The latest library update of the active profile that covered the whole library, and what it decided about each
 * entry. Later updates of part of the library don't start a run of their own; their decisions replace the ones they
 * redo.
 */
interface LibraryUpdateReportRepository {
    fun subscribeLatestRun(): Flow<LibraryUpdateRun?>

    suspend fun getLatestRun(): LibraryUpdateRun?

    /** Statuses decided by the latest run or by a partial update since. */
    fun subscribeLatestRunStatuses(): Flow<List<EntryUpdateStatus>>

    fun subscribeStatus(entryId: Long): Flow<EntryUpdateStatus?>

    suspend fun getStatuses(): Map<Long, EntryUpdateStatus>

    suspend fun startRun(startedAt: Long, trigger: LibraryUpdateTrigger, librarySize: Int)

    suspend fun finishRun(startedAt: Long, finishedAt: Long)

    /** Records the entries an update left out; checked entries are recorded as their checks finish. */
    suspend fun recordDecisions(decidedAt: Long, decisions: List<Decision>)

    suspend fun recordChecked(entryId: Long, decidedAt: Long, checkedAt: Long, newChapters: Int)

    /** Leaves the last check time alone, so a failed entry is retried by the next update that runs. */
    suspend fun recordFailed(entryId: Long, decidedAt: Long, error: String?)

    data class Decision(
        val entryId: Long,
        val reason: EntryUpdateDecisionReason,
        val reasonCategoryId: Long?,
    )
}
