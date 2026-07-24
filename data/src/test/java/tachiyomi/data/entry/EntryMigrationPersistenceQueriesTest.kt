package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
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

class EntryMigrationPersistenceQueriesTest {
    @Test
    fun `operation identity owns durable consequences`() = runTest {
        withDatabase { database, _ ->
            database.entry_migration_operationsQueries.insert("operation", "replace:notes", 2, 10, 11, "REPLACE", 1)
            database.entry_migration_consequencesQueries.insert(
                consequenceId = "consequence",
                operationId = "operation",
                profileId = 2,
                participantId = "progress",
                schemaVersion = 1,
                payload = "payload",
                createdAt = 1,
            )

            database.entry_migration_consequencesQueries.countByOperation("operation").awaitAsOne() shouldBe 1
            database.entry_migration_operationsQueries.getById("operation").awaitAsOne().run {
                source_entry_id shouldBe 10
                target_entry_id shouldBe 11
                intent_fingerprint shouldBe "replace:notes"
            }
        }
    }

    @Test
    fun `nested participant work rolls back with the outer migration transaction`() = runTest {
        withDatabase { database, handler ->
            runCatching {
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
                    error("abort outer transaction")
                }
            }

            database.entry_migration_operationsQueries.getById("operation").awaitAsOneOrNull() shouldBe null
            database.entry_migration_consequencesQueries.countByOperation("operation").awaitAsOne() shouldBe 0
        }
    }

    @Test
    fun `consequence failure remains pending until acknowledged`() = runTest {
        withDatabase { database, _ ->
            database.entry_migration_operationsQueries.insert("operation", "copy", 2, 10, 11, "COPY", 1)
            database.entry_migration_consequencesQueries.insert(
                consequenceId = "consequence",
                operationId = "operation",
                profileId = 2,
                participantId = "progress",
                schemaVersion = 1,
                payload = "payload",
                createdAt = 1,
            )

            database.entry_migration_consequencesQueries.pending(0, 10).awaitAsList().size shouldBe 1
            database.entry_migration_consequencesQueries.recordFailure(100, "failed", "consequence")
            database.entry_migration_consequencesQueries.pending(99, 10).awaitAsList() shouldBe emptyList()
            database.entry_migration_consequencesQueries.makeRetryable()
            database.entry_migration_consequencesQueries.pending(0, 10).awaitAsList().single().attempts shouldBe 1
            database.entry_migration_consequencesQueries.acknowledge("consequence")
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
