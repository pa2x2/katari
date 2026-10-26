package tachiyomi.data.library.update

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus

class LibraryUpdateReportRepositoryImplTest {

    // Only library updates wrote the status, so an entry that kept failing in them still showed Failing after a refresh
    // from its screen loaded it. Recording that refresh as a whole check instead would take the new chapters an update
    // found out of its report.
    @Test
    fun `a refresh outside library updates ends a failure but keeps what an update found`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO entries(_id, profile_id, source, url, title, favorite, date_added, type)
                    VALUES (1, 1, 1, '/failing', 'Failing', 1, 0, 'book'),
                        (2, 1, 1, '/updated', 'Updated', 1, 0, 'book'),
                        (3, 1, 1, '/removed', 'Removed', 0, 0, 'book');
                """.trimIndent(),
                parameters = 0,
            )
            val database = Database(
                driver = driver,
                entriesAdapter = Entries.Adapter(
                    genreAdapter = StringListColumnAdapter,
                    update_strategyAdapter = UpdateStrategyColumnAdapter,
                    memoAdapter = MemoColumnAdapter,
                ),
                chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
                historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
            )
            val report = LibraryUpdateReportRepositoryImpl(
                handler = AndroidDatabaseHandler(db = database, driver = driver),
                profileProvider = object : ActiveProfileProvider {
                    override val activeProfileId = 1L
                    override val activeProfileIdFlow = flowOf(1L)
                },
            )

            listOf(100L, 200L, 300L).forEach { report.recordFailed(1, decidedAt = it, error = "Untrusted") }
            report.recordChecked(2, decidedAt = 300, checkedAt = 310, newChapters = 4)
            report.recordFailed(3, decidedAt = 300, error = "Untrusted")

            listOf(1L, 2L, 3L).forEach { report.recordRefreshed(it, checkedAt = 500, newChapters = 2) }

            report.getStatuses(1) shouldBe mapOf(
                1L to EntryUpdateStatus(
                    entryId = 1,
                    decidedAt = 500,
                    outcome = EntryUpdateOutcome.NEW_CHAPTERS,
                    reason = null,
                    reasonCategoryId = null,
                    error = null,
                    newChapters = 2,
                    lastCheckedAt = 500,
                    consecutiveFailures = 0,
                ),
                2L to EntryUpdateStatus(
                    entryId = 2,
                    decidedAt = 300,
                    outcome = EntryUpdateOutcome.NEW_CHAPTERS,
                    reason = null,
                    reasonCategoryId = null,
                    error = null,
                    newChapters = 4,
                    lastCheckedAt = 500,
                    consecutiveFailures = 0,
                ),
                3L to EntryUpdateStatus(
                    entryId = 3,
                    decidedAt = 300,
                    outcome = EntryUpdateOutcome.FAILED,
                    reason = null,
                    reasonCategoryId = null,
                    error = "Untrusted",
                    newChapters = 0,
                    lastCheckedAt = null,
                    consecutiveFailures = 1,
                ),
            )
        } finally {
            driver.close()
        }
    }
}
