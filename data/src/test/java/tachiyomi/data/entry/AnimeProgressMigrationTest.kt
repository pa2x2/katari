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

class AnimeProgressMigrationTest {

    @Test
    fun `migration 34 backfills meaningful anime progress and removes playback state`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            createVersion34Schema(driver)
            seedVersion34Data(driver)
            driver.scalarLong("SELECT count(*) FROM playback_state") shouldBe 4L
            driver.scalarLong("SELECT count(*) FROM chapters WHERE entry_id = 1") shouldBe 5L

            Database.Schema.awaitMigrate(driver, oldVersion = 34, newVersion = 35)

            driver.scalarLong(
                "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'playback_state'",
            ) shouldBe 0L

            val rows = driver.executeQuery(
                identifier = null,
                sql = """
                    SELECT chapter_id, resource_key, position, extent, progression, completed,
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
                                        position = cursor.getLong(2),
                                        extent = cursor.getLong(3),
                                        progression = cursor.getDouble(4),
                                        completed = cursor.getLong(5) != 0L,
                                        locatorUpdatedAt = cursor.getLong(6)!!,
                                        completionUpdatedAt = cursor.getLong(7)!!,
                                    ),
                                )
                            }
                        },
                    )
                },
                parameters = 0,
            ).await()

            rows.shouldContainExactly(
                MigratedProgress(11, "/partial", 45_000, 100_000, 0.45, false, 500, 500),
                MigratedProgress(12, "/playback-complete", 100_000, 100_000, 1.0, true, 600, 600),
                MigratedProgress(13, "/child-complete", null, null, null, true, 0, 700),
                MigratedProgress(14, "/invalid", 0, null, null, true, 0, 0),
            )
            driver.scalarLong("SELECT read FROM chapters WHERE _id = 12") shouldBe 1L
            driver.scalarLong("SELECT count(*) FROM entry_progress_state WHERE chapter_id IN (15, 21)") shouldBe 0L
        } finally {
            driver.close()
        }
    }

    private suspend fun createVersion34Schema(driver: JdbcSqliteDriver) {
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
                _id INTEGER PRIMARY KEY,
                entry_id INTEGER NOT NULL,
                url TEXT NOT NULL,
                name TEXT NOT NULL,
                scanlator TEXT,
                read INTEGER NOT NULL,
                bookmark INTEGER NOT NULL,
                last_page_read INTEGER NOT NULL,
                date_upload INTEGER NOT NULL,
                date_fetch INTEGER NOT NULL
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
            CREATE TABLE playback_state(
                _id INTEGER PRIMARY KEY,
                entry_id INTEGER NOT NULL,
                chapter_id INTEGER NOT NULL,
                position_ms INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL,
                completed INTEGER NOT NULL,
                last_watched_at INTEGER NOT NULL
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

    private suspend fun seedVersion34Data(driver: JdbcSqliteDriver) {
        driver.executeSql(
            """
            INSERT INTO entries(_id, profile_id, type, title, source, favorite, cover_last_modified, date_added)
            VALUES
                (1, 1, 'anime', 'Anime', 1, 1, 0, 0),
                (2, 1, 'manga', 'Manga', 2, 1, 0, 0)
            """,
        )
        driver.executeSql(
            """
            INSERT INTO chapters(
                _id, entry_id, url, name, read, bookmark, last_page_read, date_upload, date_fetch
            )
            VALUES
                (11, 1, '/partial', 'Partial', 0, 0, 0, 0, 1),
                (12, 1, '/playback-complete', 'Playback complete', 0, 0, 0, 0, 1),
                (13, 1, '/child-complete', 'Child complete', 1, 0, 0, 0, 1),
                (14, 1, '/invalid', 'Invalid', 0, 0, 0, 0, 1),
                (15, 1, '/untouched', 'Untouched', 0, 0, 0, 0, 1),
                (21, 2, '/manga', 'Manga', 1, 0, 3, 0, 1)
            """,
        )
        driver.executeSql(
            """
            INSERT INTO playback_state(
                _id, entry_id, chapter_id, position_ms, duration_ms, completed, last_watched_at
            )
            VALUES
                (1, 1, 11, 45000, 100000, 0, 500),
                (2, 1, 12, 100000, 100000, 1, 600),
                (3, 1, 14, -5, -1, 1, -20),
                (4, 2, 21, 10, 20, 0, 800)
            """,
        )
        driver.executeSql(
            """
            INSERT INTO history(_id, entry_id, chapter_id, last_read, time_read)
            VALUES (1, 1, 13, 700, 1000)
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
        val position: Long?,
        val extent: Long?,
        val progression: Double?,
        val completed: Boolean,
        val locatorUpdatedAt: Long,
        val completionUpdatedAt: Long,
    )
}
