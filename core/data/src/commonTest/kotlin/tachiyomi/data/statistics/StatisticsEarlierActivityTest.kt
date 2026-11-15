package tachiyomi.data.statistics

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.kanade.tachiyomi.source.entry.EntryType
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
import tachiyomi.domain.statistics.model.StatisticsEarlierActivity

class StatisticsEarlierActivityTest {

    @Test
    fun `a title whose sessions recorded more than its history leaves the others' earlier time whole`() = runTest {
        withRepository { driver, repository ->
            // Entry 1 was read for ten minutes before sessions were recorded. Entry 2's sessions recorded five
            // minutes while its history kept only two, so it has no earlier time and takes none from entry 1.
            driver.execute(
                """
                INSERT INTO entries(_id, profile_id, source, url, title, type) VALUES
                    (1, 1, 1, '/one', 'One', 'manga'),
                    (2, 1, 1, '/two', 'Two', 'manga');
                INSERT INTO chapters(_id, entry_id, url, name) VALUES
                    (11, 1, '/one/1', 'One 1'),
                    (21, 2, '/two/1', 'Two 1');
                INSERT INTO history(entry_id, chapter_id, time_read) VALUES
                    (1, 11, 600000),
                    (2, 21, 120000);
                INSERT INTO activity_sessions(session_id, entry_id, started_at, ended_at, duration) VALUES
                    ('two', 2, 0, 300000, 300000);
                INSERT INTO activity_segments(
                    session_id, chapter_id, local_date, time_zone_id, started_at, ended_at, duration
                ) VALUES
                    ('two', 21, '2026-08-23', 'UTC', 0, 300000, 300000);
                """,
            )

            val details = repository.getEarlierActivityDetails(profileId = 1L, type = null, limit = 10L)

            details.totals shouldBe listOf(StatisticsEarlierActivity(EntryType.MANGA, 600_000L))
            details.topEntries.map { it.entryId to it.durationMillis } shouldBe listOf(1L to 600_000L)
        }
    }

    private suspend fun JdbcSqliteDriver.execute(sql: String) {
        sql.trimIndent().split(";").map(String::trim).filter(String::isNotEmpty).forEach { statement ->
            await(identifier = null, sql = statement, parameters = 0)
        }
    }

    private suspend fun withRepository(block: suspend (JdbcSqliteDriver, StatisticsRepositoryImpl) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
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
            block(driver, StatisticsRepositoryImpl(AndroidDatabaseHandler(db = database, driver = driver)))
        } finally {
            driver.close()
        }
    }
}
