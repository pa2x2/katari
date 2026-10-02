package mihon.feature.library.update.report

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository

/** The latest library update in numbers, for the places that link to its report. */
data class LibraryUpdateRunSummary(
    val run: LibraryUpdateRun,
    val checked: Int,
    val newChapters: Int,
    val failed: Int,
    val skipped: Int,
    /** Entries the update left out because they are set to never be checked. */
    val neverChecked: Int,
    /** Entries the update left out because their category, source or type is switched off. */
    val switchedOff: Int,
) {
    val isRunning: Boolean
        get() = run.finishedAt == null

    companion object {
        fun of(run: LibraryUpdateRun, statuses: List<EntryUpdateStatus>) = LibraryUpdateRunSummary(
            run = run,
            checked = statuses.count { it.outcome in CHECKED_OUTCOMES },
            newChapters = statuses.sumOf { it.newChapters },
            failed = statuses.count { it.outcome == EntryUpdateOutcome.FAILED },
            skipped = statuses.count { it.outcome == EntryUpdateOutcome.SKIPPED },
            neverChecked = statuses.count { it.reason == EntryUpdateDecisionReason.ENTRY_NEVER },
            switchedOff = statuses.count { it.reason in SWITCH_REASONS },
        )

        private val CHECKED_OUTCOMES = setOf(
            EntryUpdateOutcome.NEW_CHAPTERS,
            EntryUpdateOutcome.NO_CHANGES,
            EntryUpdateOutcome.FAILED,
        )
        private val SWITCH_REASONS = setOf(
            EntryUpdateDecisionReason.CATEGORY_OFF,
            EntryUpdateDecisionReason.SOURCE_OFF,
            EntryUpdateDecisionReason.TYPE_OFF,
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
                LibraryUpdateRunSummary.of(run, statuses.filter { it.decidedAt >= run.startedAt })
            }
        }
    }
}
