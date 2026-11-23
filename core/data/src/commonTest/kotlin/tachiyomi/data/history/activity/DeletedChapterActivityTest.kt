package tachiyomi.data.history.activity

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter

class DeletedChapterActivityTest {

    @Test
    fun `chapters read in one session and deleted one by one leave one segment holding all their time`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            listOf(
                "INSERT INTO entries(_id, profile_id, source, url, title, type) " +
                    "VALUES (1, 1, 1, '/entry', 'Entry', 'manga')",
                "INSERT INTO chapters(_id, entry_id, url, name) VALUES (11, 1, '/1', 'One'), (12, 1, '/2', 'Two'), " +
                    "(13, 1, '/3', 'Three')",
                "INSERT INTO activity_sessions(session_id, entry_id, started_at, ended_at, duration) " +
                    "VALUES ('session', 1, 100, 700, 450)",
                """
                INSERT INTO activity_segments(
                    session_id, chapter_id, local_date, time_zone_id, started_at, ended_at, duration
                ) VALUES
                    ('session', 11, '2026-11-23', 'UTC', 100, 200, 100),
                    ('session', 12, '2026-11-23', 'UTC', 300, 400, 100),
                    ('session', 13, '2026-11-23', 'UTC', 500, 700, 200),
                    ('session', 13, '2026-11-24', 'UTC', 700, 750, 50)
                """,
                // Two chapters on their own, as a refresh removes them, then the third
                "DELETE FROM chapters WHERE _id = 11",
                "DELETE FROM chapters WHERE _id = 12",
                "DELETE FROM chapters WHERE _id = 13",
            ).forEach { driver.await(null, it, 0) }
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

            database.activityQueries.getActivitySegmentsByEntryIds(listOf(1L)).awaitAsList().map {
                listOf(it.chapter_id, it.local_date, it.started_at, it.ended_at, it.duration)
            } shouldBe listOf(
                listOf(null, "2026-11-23", 100L, 700L, 400L),
                listOf(null, "2026-11-24", 700L, 750L, 50L),
            )
        } finally {
            driver.close()
        }
    }
}
