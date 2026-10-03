package mihon.feature.library.update.report

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger

class LibraryUpdateReportTest {

    @Test
    fun `checking a paused source keeps its entries with it until the source is resumed`() {
        val leftOut = status(1, RUN_START, EntryUpdateOutcome.NOT_CHECKED, EntryUpdateDecisionReason.SOURCE_OFF)
        val checkedWhilePaused = status(2, RECHECK, EntryUpdateOutcome.NO_CHANGES)
        val otherSource = status(3, RECHECK, EntryUpdateOutcome.NO_CHANGES)
        val statuses = listOf(leftOut, checkedWhilePaused, otherSource)

        val paused = LibraryUpdateReport.build(run, statuses, entries, pausedSourceIds = setOf(PAUSED))
        val resumed = LibraryUpdateReport.build(run, statuses, entries, pausedSourceIds = emptySet())

        paused.pausedSources.map { source -> source.sourceId to source.items.map { it.entry.id } } shouldBe
            listOf(PAUSED to listOf(1L, 2L))
        paused.noChanges.map { it.entry.id } shouldBe listOf(3L)
        paused.notChecked shouldBe emptyMap()
        resumed.pausedSources.map { source -> source.sourceId to source.items.map { it.entry.id } } shouldBe
            listOf(PAUSED to listOf(1L))
        resumed.noChanges.map { it.entry.id } shouldBe listOf(2L, 3L)
    }

    private val run = LibraryUpdateRun(
        startedAt = RUN_START,
        finishedAt = RUN_START + 1,
        trigger = LibraryUpdateTrigger.AUTOMATIC,
        librarySize = 3,
    )

    private val entries = mapOf(
        1L to entry(1, PAUSED),
        2L to entry(2, PAUSED),
        3L to entry(3, OTHER),
    )

    private fun entry(id: Long, source: Long) = Entry.create().copy(id = id, source = source, title = "Entry $id")

    private fun status(
        entryId: Long,
        decidedAt: Long,
        outcome: EntryUpdateOutcome,
        reason: EntryUpdateDecisionReason? = null,
    ) = EntryUpdateStatus(
        entryId = entryId,
        decidedAt = decidedAt,
        outcome = outcome,
        reason = reason,
        reasonCategoryId = null,
        error = null,
        newChapters = 0,
        lastCheckedAt = decidedAt.takeIf { reason == null },
        consecutiveFailures = 0,
    )

    private companion object {
        const val PAUSED = 10L
        const val OTHER = 20L
        const val RUN_START = 1_000L
        const val RECHECK = 2_000L
    }
}
