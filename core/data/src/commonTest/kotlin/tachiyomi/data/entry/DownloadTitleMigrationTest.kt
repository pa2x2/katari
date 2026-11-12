package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitMigrate
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.Database

class DownloadTitleMigrationTest {

    @Test
    fun `migration 91 forgets download names that are only the title, and keeps names of their own`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.executeSql(
                """
                CREATE TABLE entries(
                    _id INTEGER PRIMARY KEY,
                    title TEXT NOT NULL,
                    download_title TEXT
                )
                """,
            )
            driver.executeSql(
                """
                INSERT INTO entries(_id, title, download_title)
                VALUES
                    (1, 'Season 1', 'Season 1'),
                    (2, 'Season 1', 'Show - Season 1'),
                    (3, 'Renamed', 'Before the rename'),
                    (4, 'Unnamed', NULL)
                """,
            )

            Database.Schema.awaitMigrate(driver, oldVersion = 91, newVersion = 92)

            driver.downloadTitles().shouldContainExactly(
                1L to null,
                2L to "Show - Season 1",
                3L to "Before the rename",
                4L to null,
            )
        } finally {
            driver.close()
        }
    }

    private suspend fun JdbcSqliteDriver.downloadTitles(): List<Pair<Long, String?>> {
        return executeQuery(
            identifier = null,
            sql = "SELECT _id, download_title FROM entries ORDER BY _id",
            mapper = { cursor ->
                QueryResult.Value(
                    buildList {
                        while (cursor.next().value) {
                            add(cursor.getLong(0)!! to cursor.getString(1))
                        }
                    },
                )
            },
            parameters = 0,
        ).await()
    }

    private suspend fun JdbcSqliteDriver.executeSql(sql: String) {
        await(identifier = null, sql = sql.trimIndent(), parameters = 0)
    }
}
