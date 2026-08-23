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
import tachiyomi.domain.history.model.activity.HistoryActivityUpdate
import java.time.Instant

class HistoryActivityRecorderTest {

    @Test
    fun `checkpoint atomically updates History and local-day segments once`() = runTest {
        withDatabase { database, recorder ->
            val midnight = Instant.parse("2026-08-23T00:00:00Z").toEpochMilli()
            val update = HistoryActivityUpdate(
                entryId = 1L,
                chapterId = 11L,
                sessionId = "session",
                sequence = 0L,
                startedAtEpochMillis = midnight - 30_000L,
                endedAtEpochMillis = midnight + 30_000L,
                durationMillis = 60_000L,
                timeZoneId = "UTC",
            )

            recorder.record(update) shouldBe true
            recorder.record(update) shouldBe false

            database.historyQueries.getReadDuration(1L).awaitAsOne() shouldBe 60_000L
            database.activityQueries.getActivitySegmentsByEntryId(1L).awaitAsList().map { segment ->
                segment.local_date to segment.duration
            } shouldBe listOf(
                "2026-08-22" to 30_000L,
                "2026-08-23" to 30_000L,
            )
            database.activityQueries.getStatisticsEpoch(1L).awaitAsOne() shouldBe midnight - 30_000L
        }
    }

    @Test
    fun `newer checkpoints merge into the same session child and local day`() = runTest {
        withDatabase { database, recorder ->
            val start = Instant.parse("2026-08-23T10:00:00Z").toEpochMilli()
            recorder.record(activityUpdate(sequence = 0L, start = start, duration = 30_000L)) shouldBe true
            recorder.record(activityUpdate(sequence = 1L, start = start + 30_000L, duration = 30_000L)) shouldBe true

            val segments = database.activityQueries.getActivitySegmentsByEntryId(1L).awaitAsList()
            segments.size shouldBe 1
            segments.single().duration shouldBe 60_000L
            database.historyQueries.getReadDuration(1L).awaitAsOne() shouldBe 60_000L
        }
    }

    private fun activityUpdate(sequence: Long, start: Long, duration: Long) = HistoryActivityUpdate(
        entryId = 1L,
        chapterId = 11L,
        sessionId = "session",
        sequence = sequence,
        startedAtEpochMillis = start,
        endedAtEpochMillis = start + duration,
        durationMillis = duration,
        timeZoneId = "UTC",
    )

    private suspend fun withDatabase(block: suspend (Database, HistoryActivityRecorder) -> Unit) {
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
            block(
                database,
                HistoryActivityRecorder(
                    AndroidDatabaseHandler(
                        db = database,
                        driver = driver,
                    ),
                ),
            )
        } finally {
            driver.close()
        }
    }
}
