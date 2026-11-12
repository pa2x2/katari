package tachiyomi.domain.library.update.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository

/**
 * What a running update found so far. An update of part of the library records its results into the latest report
 * without a run of its own, so only the statuses recorded since the update started are its own.
 */
data class LibraryUpdateResultsSoFar(val newChapters: Int, val failed: Int) {
    companion object {
        fun of(startedAt: Long, statuses: List<EntryUpdateStatus>): LibraryUpdateResultsSoFar {
            val own = statuses.filter { it.decidedAt >= startedAt }
            return LibraryUpdateResultsSoFar(newChapters = own.newChapterCount(), failed = own.failedCount())
        }
    }
}

fun LibraryUpdateReportRepository.subscribeResultsSince(startedAt: Long): Flow<LibraryUpdateResultsSoFar> {
    return subscribeLatestRunStatuses().map { LibraryUpdateResultsSoFar.of(startedAt, it) }
}

internal fun List<EntryUpdateStatus>.newChapterCount(): Int = sumOf { it.newChapters }

internal fun List<EntryUpdateStatus>.failedCount(): Int = count { it.outcome == EntryUpdateOutcome.FAILED }
