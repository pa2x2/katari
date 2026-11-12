package app.katari.data.database

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDatabaseType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TemporarySchemaTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `the writer and every reader run the statements, once the schema they name exists`() = runTest {
        // A new file: the statements' table exists only once the driver has created the schema
        open(directory.resolve("test.db"), TemporarySchema(listOf(CREATE_MARKER))).use { driver ->
            driver.await(
                null,
                "INSERT INTO entries(_id, profile_id, source, url, title) VALUES (1, 1, 1, '/entry', 'Entry')",
                0,
            )
            driver.await(null, "UPDATE entries SET title = 'By the writer'", 0)
            // Outside a transaction a query runs on a reader connection, writes included.
            driver.writeThroughReader("UPDATE entries SET title = 'By a reader' RETURNING _id")
            driver.markers() shouldBe 2
        }
    }

    private fun open(file: File, temporarySchema: TemporarySchema): SqlDriver {
        lateinit var driver: SqlDriver
        driver = configuredSqlDriver(
            databaseType = AndroidxSqliteDatabaseType.File(file.path),
            readerCount = 2,
            databaseRepository = DatabaseRepositoryImpl(lazy { driver }),
            temporarySchema = temporarySchema,
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
        const val CREATE_MARKER = """
            CREATE TEMP TRIGGER marker AFTER UPDATE OF title ON main.entries
            BEGIN
                INSERT INTO sources(_id, lang, name) VALUES ((SELECT count(*) + 1 FROM sources), '', 'marker');
            END
        """
    }
}
