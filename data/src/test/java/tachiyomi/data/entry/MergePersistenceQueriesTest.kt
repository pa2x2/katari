package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
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

class MergePersistenceQueriesTest {
    @Test
    fun `deleting a member dissolves a merge group that would become a singleton`() = runTest {
        withDatabase { database ->
            database.merged_entriesQueries.insert(2, 10, 10, 0)
            database.merged_entriesQueries.insert(2, 10, 11, 1)

            database.entriesQueries.deleteById(2, 11)

            database.merged_entriesQueries.getAll(2).awaitAsList() shouldBe emptyList()
        }
    }

    @Test
    fun `clearing profile entries cascades membership and durable consequences`() = runTest {
        withDatabase { database ->
            database.merged_entriesQueries.insert(2, 10, 10, 0)
            database.merged_entriesQueries.insert(2, 10, 11, 1)
            database.merge_consequencesQueries.insert("event", "operation", 2, 10, "cleanup", "", 1)

            database.entriesQueries.deleteByProfile(2)

            database.merged_entriesQueries.getAll(2).awaitAsList() shouldBe emptyList()
            database.merge_consequencesQueries.consequenceStatus().awaitAsOne().pending_count shouldBe 0
        }
    }

    @Test
    fun `failed consequences remain visible and can be made immediately retryable`() = runTest {
        withDatabase { database ->
            database.merge_consequencesQueries.insert("event", "operation", 2, 10, "cleanup", "", 1)
            database.merge_consequencesQueries.recordFailure(10_000, "disk unavailable", "event")

            database.merge_consequencesQueries.consequenceStatus().awaitAsOne().run {
                pending_count shouldBe 1
                failed_count shouldBe 1
                last_failure shouldBe "disk unavailable"
            }
            database.merge_consequencesQueries.makeRetryable()
            database.merge_consequencesQueries.pending(0, 10).awaitAsList().size shouldBe 1
        }
    }

    private suspend fun withDatabase(block: suspend (Database) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO profiles(_id, uuid, name, color_seed, position)
                    VALUES (2, 'profile', 'Profile', 1, 1)
                """.trimIndent(),
                parameters = 0,
            )
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO entries(_id, profile_id, source, url, title, type)
                    VALUES
                        (10, 2, 1, '/target', 'Target', 'book'),
                        (11, 2, 1, '/member', 'Member', 'book')
                """.trimIndent(),
                parameters = 0,
            )
            block(database(driver))
        } finally {
            driver.close()
        }
    }

    private fun database(driver: JdbcSqliteDriver): Database {
        return Database(
            driver = driver,
            entriesAdapter = Entries.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = UpdateStrategyColumnAdapter,
                memoAdapter = MemoColumnAdapter,
            ),
            chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
            historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
        )
    }
}
