package tachiyomi.data.history.activity

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.domain.history.model.activity.HistoryCompletionCause
import tachiyomi.domain.history.model.activity.HistoryCompletionUpdate

class HistoryCompletionRecorderTest {

    @Test
    fun `stable completion identity makes retries idempotent`() = runTest {
        withDatabase { database, recorder ->
            val update = HistoryCompletionUpdate(
                eventId = "completion",
                entryId = 1L,
                chapterId = 11L,
                sessionId = null,
                occurredAtEpochMillis = 1_000L,
                localDate = "2026-08-23",
                timeZoneId = "UTC",
                cause = HistoryCompletionCause.CONSUMPTION,
            )

            recorder.record(update)
            recorder.record(update)

            val events = database.activityQueries.getCompletionEventsByEntryId(1L).awaitAsList()
            events.size shouldBe 1
            events.single().cause shouldBe "consumption"
            database.activityQueries.getStatisticsEpoch(1L).awaitAsOne() shouldBe 1_000L
        }
    }

    private suspend fun withDatabase(block: suspend (Database, HistoryCompletionRecorder) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO entries(_id, profile_id, source, url, title, type)
                    VALUES (1, 1, 1, '/entry', 'Entry', 'manga')
                """.trimIndent(),
                parameters = 0,
            )
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO chapters(_id, entry_id, url, name)
                    VALUES (11, 1, '/chapter', 'Chapter')
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
            block(database, HistoryCompletionRecorder(AndroidDatabaseHandler(database, driver)))
        } finally {
            driver.close()
        }
    }
}
