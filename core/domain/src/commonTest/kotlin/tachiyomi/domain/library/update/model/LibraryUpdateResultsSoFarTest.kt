package tachiyomi.domain.library.update.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibraryUpdateResultsSoFarTest {

    // An update of one category folds into the latest report, whose earlier results must not count as its own.
    @Test
    fun `only results recorded since the update started count`() {
        val statuses = listOf(
            status(decidedAt = 100, EntryUpdateOutcome.NEW_CHAPTERS, newChapters = 4),
            status(decidedAt = 100, EntryUpdateOutcome.FAILED),
            status(decidedAt = 200, EntryUpdateOutcome.NEW_CHAPTERS, newChapters = 2),
            status(decidedAt = 200, EntryUpdateOutcome.FAILED),
            status(decidedAt = 200, EntryUpdateOutcome.NO_CHANGES),
        )

        LibraryUpdateResultsSoFar.of(startedAt = 200, statuses) shouldBe
            LibraryUpdateResultsSoFar(newChapters = 2, failed = 1)
    }

    private fun status(decidedAt: Long, outcome: EntryUpdateOutcome, newChapters: Int = 0) = EntryUpdateStatus(
        entryId = decidedAt + newChapters + outcome.ordinal,
        decidedAt = decidedAt,
        outcome = outcome,
        reason = null,
        reasonCategoryId = null,
        error = null,
        newChapters = newChapters,
        lastCheckedAt = null,
        consecutiveFailures = 0,
    )
}
