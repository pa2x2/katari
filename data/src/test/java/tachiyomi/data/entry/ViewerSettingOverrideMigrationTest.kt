package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitMigrate
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.Database

class ViewerSettingOverrideMigrationTest {

    @Test
    fun `migration 36 moves manga viewer flags into generic overrides`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.executeSql("PRAGMA foreign_keys = ON")
            driver.executeSql(
                """
                CREATE TABLE entries(
                    _id INTEGER PRIMARY KEY,
                    profile_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    viewer INTEGER NOT NULL DEFAULT 0
                )
                """,
            )
            driver.executeSql(
                """
                INSERT INTO entries(_id, profile_id, type, viewer)
                VALUES
                    (1, 1, 'manga', 0),
                    (2, 1, 'manga', 9),
                    (3, 1, 'manga', 18),
                    (4, 1, 'manga', 27),
                    (5, 1, 'manga', 36),
                    (6, 1, 'manga', 45),
                    (7, 1, 'manga', 304),
                    (8, 1, 'anime', 27)
                """,
            )

            Database.Schema.awaitMigrate(driver, oldVersion = 36, newVersion = 37)

            driver.viewerOverrides().shouldContainExactly(
                MigratedOverride(2, "orientation", "8"),
                MigratedOverride(2, "reading_mode", "1"),
                MigratedOverride(3, "orientation", "16"),
                MigratedOverride(3, "reading_mode", "2"),
                MigratedOverride(4, "orientation", "24"),
                MigratedOverride(4, "reading_mode", "3"),
                MigratedOverride(5, "orientation", "32"),
                MigratedOverride(5, "reading_mode", "4"),
                MigratedOverride(6, "orientation", "40"),
                MigratedOverride(6, "reading_mode", "5"),
                MigratedOverride(7, "orientation", "48"),
            )
            driver.entryViewers().shouldContainExactly(
                1L to 0L,
                2L to 0L,
                3L to 0L,
                4L to 0L,
                5L to 0L,
                6L to 0L,
                7L to 256L,
                8L to 27L,
            )

            driver.executeSql("DELETE FROM entries WHERE _id = 2")
            driver.scalarLong("SELECT count(*) FROM viewer_setting_overrides WHERE entry_id = 2") shouldBe 0L
        } finally {
            driver.close()
        }
    }

    private suspend fun JdbcSqliteDriver.viewerOverrides(): List<MigratedOverride> {
        return executeQuery(
            identifier = null,
            sql = """
                SELECT entry_id, setting_key, encoded_value
                FROM viewer_setting_overrides
                ORDER BY entry_id, setting_key
            """.trimIndent(),
            mapper = { cursor ->
                QueryResult.Value(
                    buildList {
                        while (cursor.next().value) {
                            add(MigratedOverride(cursor.getLong(0)!!, cursor.getString(1)!!, cursor.getString(2)!!))
                        }
                    },
                )
            },
            parameters = 0,
        ).await()
    }

    private suspend fun JdbcSqliteDriver.entryViewers(): List<Pair<Long, Long>> {
        return executeQuery(
            identifier = null,
            sql = "SELECT _id, viewer FROM entries ORDER BY _id",
            mapper = { cursor ->
                QueryResult.Value(
                    buildList {
                        while (cursor.next().value) {
                            add(cursor.getLong(0)!! to cursor.getLong(1)!!)
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

    private suspend fun JdbcSqliteDriver.scalarLong(sql: String): Long {
        return executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                check(cursor.next().value)
                QueryResult.Value(cursor.getLong(0)!!)
            },
            parameters = 0,
        ).await()
    }

    private data class MigratedOverride(
        val entryId: Long,
        val settingKey: String,
        val encodedValue: String,
    )
}
