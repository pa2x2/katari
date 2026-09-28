package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.assertions.throwables.shouldThrow
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

class EntryMigrationPersistenceQueriesTest {
    @Test
    fun `nested participant work rolls back with the outer migration transaction`() = runTest {
        withDatabase { database, handler ->
            val abort = IllegalStateException("abort outer transaction")
            val failure = shouldThrow<IllegalStateException> {
                handler.await(inTransaction = true) {
                    entry_migration_operationsQueries.insert("operation", "replace", 2, 10, 11, "REPLACE", 1)
                    handler.await(inTransaction = true) {
                        entry_migration_consequencesQueries.insert(
                            consequenceId = "participant",
                            operationId = "operation",
                            profileId = 2,
                            participantId = "merge-participant-proof",
                            schemaVersion = 1,
                            payload = "",
                            createdAt = 1,
                        )
                    }
                    entry_migration_consequencesQueries.countByOperation("operation").awaitAsOne() shouldBe 1
                    throw abort
                }
            }
            failure shouldBe abort

            database.entry_migration_operationsQueries.getById("operation").awaitAsOneOrNull() shouldBe null
            database.entry_migration_consequencesQueries.countByOperation("operation").awaitAsOne() shouldBe 0
        }
    }

    private suspend fun withDatabase(block: suspend (Database, AndroidDatabaseHandler) -> Unit) {
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
            val database = database(driver)
            block(database, AndroidDatabaseHandler(database, driver))
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
