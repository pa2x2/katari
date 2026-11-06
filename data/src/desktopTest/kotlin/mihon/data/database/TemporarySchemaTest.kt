package mihon.data.database

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDatabaseType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.data.Database
import java.io.File

class TemporarySchemaTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `temporary statements never run during a migration and reach writer and readers after it`() = runTest {
        val file = directory.resolve("test.db")
        open(file, TemporarySchema { _ -> emptyList() }).use { driver ->
            driver.await(
                null,
                "INSERT INTO entries(_id, profile_id, source, url, title) VALUES (1, 1, 1, '/entry', 'Entry')",
                0,
            )
        }

        val schema = TemporarySchema { _ -> listOf(DROP_MARKER, CREATE_MARKER) }
        val migrating = object : SqlSchema<QueryResult.AsyncValue<Unit>> by Database.Schema {
            override val version = Database.Schema.version + 1

            override fun migrate(
                driver: SqlDriver,
                oldVersion: Long,
                newVersion: Long,
                vararg callbacks: AfterVersion,
            ) = QueryResult.AsyncValue {
                driver.await(null, "UPDATE entries SET title = 'Migrated'", 0)
                Unit
            }
        }
        open(file, schema, migrating).use { driver ->
            driver.markers() shouldBe 0

            driver.await(null, "UPDATE entries SET title = 'By the writer'", 0)
            // Outside a transaction a query runs on a reader connection, writes included.
            driver.writeThroughReader("UPDATE entries SET title = 'By a reader' RETURNING _id")
            driver.markers() shouldBe 2

            schema.publish(listOf(DROP_MARKER))
            driver.await(null, "UPDATE entries SET title = 'Unmarked by the writer'", 0)
            driver.writeThroughReader("UPDATE entries SET title = 'Unmarked by a reader' RETURNING _id")
            driver.markers() shouldBe 2
        }
    }

    private fun open(
        file: File,
        temporarySchema: TemporarySchema,
        schema: SqlSchema<QueryResult.AsyncValue<Unit>> = Database.Schema,
    ): SqlDriver {
        lateinit var driver: SqlDriver
        driver = configuredSqlDriver(
            databaseType = AndroidxSqliteDatabaseType.File(file.path),
            readerCount = 2,
            databaseRepository = DatabaseRepositoryImpl(lazy { driver }),
            temporarySchema = temporarySchema,
            schema = schema,
        )
        return driver
    }

    private suspend fun SqlDriver.writeThroughReader(sql: String) {
        executeQuery(null, sql, { cursor -> QueryResult.Value(cursor.apply { next() }) }, 0).await()
    }

    private suspend fun SqlDriver.markers(): Long = executeQuery(
        null,
        "SELECT count(*) FROM sources WHERE name = 'marker'",
        { cursor -> QueryResult.Value(cursor.apply { next() }.getLong(0)!!) },
        0,
    ).await()

    private companion object {
        const val DROP_MARKER = "DROP TRIGGER IF EXISTS temp.marker"
        const val CREATE_MARKER = """
            CREATE TEMP TRIGGER marker AFTER UPDATE OF title ON main.entries
            BEGIN
                INSERT INTO sources(_id, lang, name) VALUES ((SELECT count(*) + 1 FROM sources), '', 'marker');
            END
        """
    }
}
