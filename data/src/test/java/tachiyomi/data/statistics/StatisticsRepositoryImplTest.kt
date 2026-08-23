package tachiyomi.data.statistics

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
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

class StatisticsRepositoryImplTest {

    @Test
    fun `activity read model groups media contributions and isolates profiles`() = runTest {
        withDatabase { database, repository ->
            database.activityQueries.insertActivitySession("manga-session", 1L, 1_000L, 2_000L)
            database.activityQueries.upsertActivitySegment(
                "manga-session",
                null,
                "2026-08-23",
                "UTC",
                1_000L,
                2_000L,
                1_000L,
            )
            database.activityQueries.insertActivitySession("anime-session", 2L, 1_000L, 4_000L)
            database.activityQueries.upsertActivitySegment(
                "anime-session",
                null,
                "2026-08-23",
                "UTC",
                1_000L,
                4_000L,
                3_000L,
            )
            database.activityQueries.insertActivitySession("other-profile", 3L, 1_000L, 9_000L)
            database.activityQueries.upsertActivitySegment(
                "other-profile",
                null,
                "2026-08-23",
                "UTC",
                1_000L,
                9_000L,
                8_000L,
            )

            val snapshot = repository.subscribeActivity(1L, "2026-08-23").first()

            snapshot.activity.associate { it.type to it.durationMillis } shouldBe mapOf(
                EntryType.ANIME to 3_000L,
                EntryType.MANGA to 1_000L,
            )
            snapshot.topEntries.map { it.title } shouldBe listOf("Anime", "Manga")
            snapshot.sessions.associateBy { it.type }.let { sessions ->
                sessions.getValue(EntryType.MANGA).sessionCount shouldBe 1L
                sessions.getValue(EntryType.MANGA).averageDurationMillis shouldBe 1_000L
                sessions.getValue(EntryType.ANIME).longestDurationMillis shouldBe 3_000L
            }
            snapshot.earlierActivity.associate { it.type to it.durationMillis } shouldBe mapOf(
                EntryType.MANGA to 5_000L,
            )
        }
    }

    private suspend fun withDatabase(block: suspend (Database, StatisticsRepositoryImpl) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            driver.await(
                null,
                "INSERT INTO profiles(_id, uuid, name, color_seed, position) " +
                    "VALUES (2, 'other', 'Other', 1, 1)",
                0,
            )
            driver.await(
                null,
                """
                    INSERT INTO entries(_id, profile_id, source, url, title, type)
                    VALUES
                        (1, 1, 1, '/manga', 'Manga', 'manga'),
                        (2, 1, 1, '/anime', 'Anime', 'anime'),
                        (3, 2, 1, '/book', 'Book', 'book')
                """.trimIndent(),
                0,
            )
            driver.await(
                null,
                """
                    INSERT INTO chapters(_id, entry_id, url, name)
                    VALUES
                        (11, 1, '/manga/chapter', 'Manga chapter'),
                        (12, 2, '/anime/episode', 'Anime episode')
                """.trimIndent(),
                0,
            )
            driver.await(
                null,
                """
                    INSERT INTO history(entry_id, chapter_id, last_read, time_read)
                    VALUES
                        (1, 11, 1000, 6000),
                        (2, 12, 1000, 3000)
                """.trimIndent(),
                0,
            )
            val database = Database(
                driver = driver,
                entriesAdapter = Entries.Adapter(
                    genreAdapter = StringListColumnAdapter,
                    update_strategyAdapter = UpdateStrategyColumnAdapter,
                    memoAdapter = MemoColumnAdapter,
                ),
                chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
                historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
            )
            val handler = AndroidDatabaseHandler(database, driver)
            block(database, StatisticsRepositoryImpl(handler))
        } finally {
            driver.close()
        }
    }
}
