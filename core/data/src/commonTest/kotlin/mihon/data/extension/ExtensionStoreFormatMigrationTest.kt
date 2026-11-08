package mihon.data.extension

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitMigrate
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.Database

class ExtensionStoreFormatMigrationTest {

    // A store read in the wrong format after the update lists nothing, and an unsigned one couldn't be saved at all
    @Test
    fun `migration 63 keeps each store's format and key and accepts a store without a key`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.executeSql(
                """
                CREATE TABLE extension_store(
                    index_url TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    badge_label TEXT NOT NULL,
                    signing_key TEXT NOT NULL,
                    contact_website TEXT NOT NULL,
                    contact_discord TEXT,
                    is_legacy INTEGER NOT NULL,
                    extension_list_url TEXT
                )
                """,
            )
            driver.executeSql(
                """
                INSERT INTO extension_store VALUES
                    ('https://a.invalid/repo.json', 'A', 'A', 'key-a', 'https://a.invalid', NULL, 1, NULL),
                    ('https://b.invalid/index.pb', 'B', 'B', 'key-b', 'https://b.invalid', 'discord', 0, 'https://b.invalid/list')
                """,
            )

            Database.Schema.awaitMigrate(driver, oldVersion = 63, newVersion = 64)
            driver.executeSql(
                """
                INSERT INTO extension_store VALUES
                    ('https://c.invalid/plugins.json', 'C', 'C', NULL, 'https://c.invalid', NULL, 'v2', NULL)
                """,
            )

            driver.stores().shouldContainExactly(
                "https://a.invalid/repo.json|key-a|legacy|null",
                "https://b.invalid/index.pb|key-b|v2|https://b.invalid/list",
                "https://c.invalid/plugins.json|null|v2|null",
            )
        } finally {
            driver.close()
        }
    }

    private suspend fun JdbcSqliteDriver.stores(): List<String> {
        return executeQuery(
            identifier = null,
            sql = "SELECT index_url, signing_key, format, extension_list_url FROM extension_store ORDER BY index_url",
            mapper = { cursor ->
                QueryResult.Value(
                    buildList {
                        while (cursor.next().value) {
                            add((0..3).joinToString("|") { cursor.getString(it).toString() })
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
