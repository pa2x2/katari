package tachiyomi.data.history

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsOne
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

class HistoryProfileQueriesTest {

    @Test
    fun `read duration only includes active profile history`() = runTest {
        withSeededDatabase { database ->
            database.historyQueries.getReadDuration(profileId = 1L).awaitAsOne() shouldBe 100L
            database.historyQueries.getReadDuration(profileId = 2L).awaitAsOne() shouldBe 200L
        }
    }

    @Test
    fun `clearing history preserves other profiles`() = runTest {
        withSeededDatabase { database ->
            database.historyQueries.removeAllHistory(profileId = 1L)

            database.historyQueries.getReadDuration(profileId = 1L).awaitAsOne() shouldBe 0L
            database.historyQueries.getReadDuration(profileId = 2L).awaitAsOne() shouldBe 200L
        }
    }

    private suspend fun withSeededDatabase(block: suspend (Database) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            seed(driver)
            block(
                Database(
                    driver = driver,
                    entriesAdapter = Entries.Adapter(
                        genreAdapter = StringListColumnAdapter,
                        update_strategyAdapter = UpdateStrategyColumnAdapter,
                        memoAdapter = MemoColumnAdapter,
                    ),
                    chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
                    historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
                ),
            )
        } finally {
            driver.close()
        }
    }

    private suspend fun seed(driver: JdbcSqliteDriver) {
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO profiles(_id, uuid, name, color_seed, position)
                VALUES (2, 'profile-2', 'Profile 2', 2, 2)
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO entries(_id, profile_id, source, url, title, type)
                VALUES
                    (1, 1, 1, '/profile-1', 'Profile 1 entry', 'manga'),
                    (2, 2, 1, '/profile-2', 'Profile 2 entry', 'manga')
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO chapters(_id, entry_id, url, name)
                VALUES
                    (11, 1, '/profile-1/chapter', 'Profile 1 chapter'),
                    (21, 2, '/profile-2/chapter', 'Profile 2 chapter')
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO history(entry_id, chapter_id, last_read, time_read)
                VALUES
                    (1, 11, 1, 100),
                    (2, 21, 1, 200)
            """.trimIndent(),
            parameters = 0,
        )
    }
}
