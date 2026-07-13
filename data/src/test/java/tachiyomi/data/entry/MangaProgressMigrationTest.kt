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

class MangaProgressMigrationTest {

    @Test
    fun `migration 35 backfills manga page progress and removes chapter page state`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            createVersion35Schema(driver)
            seedVersion35Data(driver)

            Database.Schema.awaitMigrate(driver, oldVersion = 35, newVersion = 36)

            driver.scalarLong(
                "SELECT count(*) FROM pragma_table_info('chapters') WHERE name = 'last_page_read'",
            ) shouldBe 0L

            val rows = driver.executeQuery(
                identifier = null,
                sql = """
                    SELECT chapter_id, resource_key, locator_kind, position, completed,
                           locator_updated_at, completion_updated_at
                    FROM entry_progress_state
                    ORDER BY chapter_id
                """.trimIndent(),
                mapper = { cursor ->
                    QueryResult.Value(
                        buildList {
                            while (cursor.next().value) {
                                add(
                                    MigratedProgress(
                                        chapterId = cursor.getLong(0)!!,
                                        resourceKey = cursor.getString(1)!!,
                                        locatorKind = cursor.getString(2)!!,
                                        position = cursor.getLong(3),
                                        completed = cursor.getLong(4) != 0L,
                                        locatorUpdatedAt = cursor.getLong(5)!!,
                                        completionUpdatedAt = cursor.getLong(6)!!,
                                    ),
                                )
                            }
                        },
                    )
                },
                parameters = 0,
            ).await()

            rows.shouldContainExactly(
                MigratedProgress(11, "/partial", "page", 4, false, 500, 500),
                MigratedProgress(12, "/completed", "page", null, true, 0, 0),
                MigratedProgress(14, "/generic-wins", "page", 2, false, 900, 900),
                MigratedProgress(21, "/anime", "time", 1_000, false, 800, 800),
            )
            driver.scalarLong("SELECT read FROM chapters WHERE _id = 14") shouldBe 0L
            driver.scalarLong("SELECT count(*) FROM entry_progress_state WHERE chapter_id = 13") shouldBe 0L
        } finally {
            driver.close()
        }
    }

    private suspend fun createVersion35Schema(driver: JdbcSqliteDriver) {
        driver.executeSql(
            """
            CREATE TABLE entries(
                _id INTEGER PRIMARY KEY,
                profile_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                source INTEGER NOT NULL,
                favorite INTEGER NOT NULL,
                thumbnail_url TEXT,
                cover_last_modified INTEGER NOT NULL,
                date_added INTEGER NOT NULL
            )
            """,
        )
        driver.executeSql(
            """
            CREATE TABLE chapters(
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                entry_id INTEGER NOT NULL,
                url TEXT NOT NULL,
                name TEXT NOT NULL,
                read INTEGER NOT NULL DEFAULT 0,
                bookmark INTEGER NOT NULL DEFAULT 0,
                last_page_read INTEGER NOT NULL DEFAULT 0,
                chapter_number REAL NOT NULL DEFAULT 0,
                scanlator TEXT,
                date_upload INTEGER NOT NULL DEFAULT 0,
                date_fetch INTEGER NOT NULL DEFAULT 0,
                source_order INTEGER NOT NULL DEFAULT 0,
                last_modified_at INTEGER NOT NULL DEFAULT 0,
                version INTEGER NOT NULL DEFAULT 0,
                is_syncing INTEGER NOT NULL DEFAULT 0,
                memo BLOB NOT NULL DEFAULT '{}'
            )
            """,
        )
        driver.executeSql(
            """
            CREATE TABLE history(
                _id INTEGER PRIMARY KEY,
                entry_id INTEGER NOT NULL,
                chapter_id INTEGER NOT NULL,
                last_read INTEGER,
                time_read INTEGER NOT NULL
            )
            """,
        )
        driver.executeSql(
            """
            CREATE TABLE entry_progress_state(
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                entry_id INTEGER NOT NULL,
                chapter_id INTEGER,
                content_key TEXT NOT NULL DEFAULT '',
                resource_key TEXT NOT NULL,
                resource_revision TEXT,
                locator_kind TEXT NOT NULL,
                position INTEGER,
                extent INTEGER,
                progression REAL,
                total_progression REAL,
                extensions TEXT NOT NULL DEFAULT '{}',
                completed INTEGER NOT NULL DEFAULT 0,
                locator_updated_at INTEGER NOT NULL DEFAULT 0,
                completion_updated_at INTEGER NOT NULL DEFAULT 0,
                UNIQUE(entry_id, content_key, resource_key)
            )
            """,
        )
        driver.executeSql(
            """
            CREATE TABLE excluded_scanlators(
                profile_id INTEGER NOT NULL,
                entry_id INTEGER NOT NULL,
                scanlator TEXT NOT NULL
            )
            """,
        )
    }

    private suspend fun seedVersion35Data(driver: JdbcSqliteDriver) {
        driver.executeSql(
            """
            INSERT INTO entries(_id, profile_id, type, title, source, favorite, cover_last_modified, date_added)
            VALUES
                (1, 1, 'manga', 'Manga', 1, 1, 0, 0),
                (2, 1, 'anime', 'Anime', 2, 1, 0, 0)
            """,
        )
        driver.executeSql(
            """
            INSERT INTO chapters(
                _id, entry_id, url, name, read, bookmark, last_page_read, chapter_number, date_fetch
            )
            VALUES
                (11, 1, '/partial', 'Partial', 0, 0, 4, 1, 1),
                (12, 1, '/completed', 'Completed', 1, 0, 0, 2, 1),
                (13, 1, '/untouched', 'Untouched', 0, 0, 0, 3, 1),
                (14, 1, '/generic-wins', 'Generic wins', 1, 0, 8, 4, 1),
                (21, 2, '/anime', 'Anime', 0, 0, 0, 1, 1)
            """,
        )
        driver.executeSql(
            """
            INSERT INTO history(_id, entry_id, chapter_id, last_read, time_read)
            VALUES (1, 1, 11, 500, 1000)
            """,
        )
        driver.executeSql(
            """
            INSERT INTO entry_progress_state(
                entry_id, chapter_id, resource_key, locator_kind, position, completed,
                locator_updated_at, completion_updated_at
            )
            VALUES
                (1, 14, '/generic-wins', 'page', 2, 0, 900, 900),
                (2, 21, '/anime', 'time', 1000, 0, 800, 800)
            """,
        )
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

    private data class MigratedProgress(
        val chapterId: Long,
        val resourceKey: String,
        val locatorKind: String,
        val position: Long?,
        val completed: Boolean,
        val locatorUpdatedAt: Long,
        val completionUpdatedAt: Long,
    )
}
