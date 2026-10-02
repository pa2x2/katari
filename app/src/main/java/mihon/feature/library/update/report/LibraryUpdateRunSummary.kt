package mihon.feature.library.update.report

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository

/** The latest library update in numbers, for the places that link to its report. */
data class LibraryUpdateRunSummary(
    val run: LibraryUpdateRun,
    val newChapters: Int,
    val failed: Int,
    val skipped: Int,
) {
    val isRunning: Boolean
        get() = run.finishedAt == null

    companion object {
        fun of(run: LibraryUpdateRun, statuses: List<EntryUpdateStatus>) = LibraryUpdateRunSummary(
            run = run,
            newChapters = statuses.sumOf { it.newChapters },
            failed = statuses.count { it.outcome == EntryUpdateOutcome.FAILED },
            skipped = statuses.count { it.outcome == EntryUpdateOutcome.SKIPPED },
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun LibraryUpdateReportRepository.subscribeLatestSummary(): Flow<LibraryUpdateRunSummary?> {
    return subscribeLatestRun().flatMapLatest { run ->
        if (run == null) {
            flowOf(null)
        } else {
            subscribeLatestRunStatuses().map { statuses ->
                LibraryUpdateRunSummary.of(run, statuses.filter { it.decidedAt == run.startedAt })
            }
        }
    }
}
