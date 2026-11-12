package app.katari.library.update.report

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger

class LibraryUpdateReportFailuresTest {

    // An outage hits every source alike; listing it per source would offer a dozen retries for one cause.
    @Test
    fun `one error behind most failures across sources is listed once`() {
        val report = build(
            failure(1, SOURCE_A, OFFLINE),
            failure(2, SOURCE_A, OFFLINE),
            failure(3, SOURCE_A, OFFLINE),
            failure(4, SOURCE_B, OFFLINE),
            failure(5, SOURCE_C, NOT_FOUND),
        )

        val shared = report.sharedFailure!!
        shared.error shouldBe OFFLINE
        shared.sources shouldBe 2
        shared.items.map { it.entry.id } shouldBe listOf(1L, 2L, 3L, 4L)
        report.failingSources shouldBe emptyList()
        report.failed.map { it.entry.id } shouldBe listOf(5L)
    }

    @Test
    fun `an error of one source stays that source's problem`() {
        val report = build(
            failure(1, SOURCE_A, OFFLINE),
            failure(2, SOURCE_A, OFFLINE),
            failure(3, SOURCE_A, OFFLINE),
            failure(4, SOURCE_B, NOT_FOUND),
        )

        report.sharedFailure shouldBe null
        report.failingSources.map { it.sourceId to it.error } shouldBe listOf(SOURCE_A to OFFLINE)
        report.failed.map { it.entry.id } shouldBe listOf(4L)
    }

    private fun build(vararg failures: Pair<Entry, EntryUpdateStatus>) = LibraryUpdateReport.build(
        run = LibraryUpdateRun(
            startedAt = RUN_START,
            finishedAt = RUN_START + 1,
            trigger = LibraryUpdateTrigger.MANUAL,
            librarySize = failures.size,
        ),
        statuses = failures.map { it.second },
        entries = failures.associate { it.first.id to it.first },
        pausedSourceIds = emptySet(),
    )

    private fun failure(id: Long, source: Long, error: String) =
        Entry.create().copy(id = id, source = source, title = "Entry $id") to EntryUpdateStatus(
            entryId = id,
            decidedAt = RUN_START,
            outcome = EntryUpdateOutcome.FAILED,
            reason = null,
            reasonCategoryId = null,
            error = error,
            newChapters = 0,
            lastCheckedAt = null,
            consecutiveFailures = 1,
        )

    private companion object {
        const val SOURCE_A = 10L
        const val SOURCE_B = 20L
        const val SOURCE_C = 30L
        const val RUN_START = 1_000L
        const val OFFLINE = "Connection refused"
        const val NOT_FOUND = "HTTP error 404"
    }
}
