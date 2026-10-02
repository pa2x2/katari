package mihon.feature.library.update.report

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateRun

/**
 * What the latest library update did, grouped the way the report shows it.
 *
 * Entries a later update of part of the library decided about again show that update's result.
 *
 * Failures that keep happening are separated from one-off ones because they need a fix rather than a retry. When most
 * of one source's checks failed with the same error, they are one problem with the source and are listed once.
 */
data class LibraryUpdateReport(
    val run: LibraryUpdateRun,
    val failingSources: List<FailingSource>,
    val failingRepeatedly: List<Item>,
    val failed: List<Item>,
    val newChapters: List<Item>,
    val noChanges: List<Item>,
    val skipped: Map<EntryUpdateDecisionReason, List<Item>>,
    val notChecked: Map<EntryUpdateDecisionReason, List<Item>>,
) {
    val skippedCount: Int
        get() = skipped.values.sumOf { it.size }

    /** @param isRechecked whether a partial update made after the report's run decided about the entry again. */
    data class Item(val entry: Entry, val status: EntryUpdateStatus, val isRechecked: Boolean)

    data class FailingSource(val sourceId: Long, val error: String?, val items: List<Item>) {
        val isFailingRepeatedly: Boolean
            get() = items.all { it.status.isFailingRepeatedly }
    }

    companion object {
        private const val SOURCE_COLLAPSE_MINIMUM = 3

        /** @param entries the entries the statuses belong to; statuses of entries no longer around are dropped. */
        fun build(
            run: LibraryUpdateRun,
            statuses: List<EntryUpdateStatus>,
            entries: Map<Long, Entry>,
        ): LibraryUpdateReport {
            val items = statuses
                .filter { it.decidedAt >= run.startedAt }
                .mapNotNull { status ->
                    entries[status.entryId]?.let { Item(it, status, isRechecked = status.decidedAt > run.startedAt) }
                }
                .sortedBy { it.entry.title.lowercase() }
            val byOutcome = items.groupBy { it.status.outcome }

            val checkedPerSource = items
                .filter { it.status.outcome in CHECKED_OUTCOMES }
                .groupingBy { it.entry.source }
                .eachCount()
            val failingSources = byOutcome[EntryUpdateOutcome.FAILED].orEmpty()
                .groupBy { it.entry.source to it.status.error }
                .filter { (key, group) ->
                    group.size >= SOURCE_COLLAPSE_MINIMUM && group.size * 2 > (checkedPerSource[key.first] ?: 0)
                }
                .map { (key, group) -> FailingSource(sourceId = key.first, error = key.second, items = group) }
            val collapsed = failingSources.flatMap { it.items }.mapTo(mutableSetOf()) { it.entry.id }
            val (failingRepeatedly, failed) = byOutcome[EntryUpdateOutcome.FAILED].orEmpty()
                .filter { it.entry.id !in collapsed }
                .partition { it.status.isFailingRepeatedly }

            return LibraryUpdateReport(
                run = run,
                failingSources = failingSources,
                failingRepeatedly = failingRepeatedly,
                failed = failed,
                newChapters = byOutcome[EntryUpdateOutcome.NEW_CHAPTERS].orEmpty(),
                noChanges = byOutcome[EntryUpdateOutcome.NO_CHANGES].orEmpty(),
                skipped = byOutcome[EntryUpdateOutcome.SKIPPED].orEmpty().groupByReason(),
                notChecked = byOutcome[EntryUpdateOutcome.NOT_CHECKED].orEmpty().groupByReason(),
            )
        }

        private val CHECKED_OUTCOMES = setOf(
            EntryUpdateOutcome.NEW_CHAPTERS,
            EntryUpdateOutcome.NO_CHANGES,
            EntryUpdateOutcome.FAILED,
        )

        private fun List<Item>.groupByReason(): Map<EntryUpdateDecisionReason, List<Item>> {
            return mapNotNull { item -> item.status.reason?.let { it to item } }
                .groupBy({ it.first }, { it.second })
                .toSortedMap(compareBy { it.ordinal })
        }
    }
}
