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
            database.recordActivitySession(
                sessionId = "manga-session",
                entryId = 1L,
                startedAt = 1_000L,
                durationMillis = 12_000L,
            )
            database.recordActivitySession(
                sessionId = "anime-session",
                entryId = 2L,
                startedAt = 1_000L,
                durationMillis = 30_000L,
            )
            database.recordActivitySession(
                sessionId = "other-profile",
                entryId = 3L,
                startedAt = 1_000L,
                durationMillis = 80_000L,
            )

            val snapshot = repository.subscribeActivity(1L, "2026-08-23").first()

            snapshot.activity.associate { it.type to it.durationMillis } shouldBe mapOf(
                EntryType.ANIME to 30_000L,
                EntryType.MANGA to 12_000L,
            )
            snapshot.topEntries.map { it.title } shouldBe listOf("Anime", "Manga")
            snapshot.sessions.associateBy { it.type }.let { sessions ->
                sessions.getValue(EntryType.MANGA).sessionCount shouldBe 1L
                sessions.getValue(EntryType.MANGA).averageDurationMillis shouldBe 12_000L
                sessions.getValue(EntryType.ANIME).longestDurationMillis shouldBe 30_000L
            }
            snapshot.earlierActivity.associate { it.type to it.durationMillis } shouldBe mapOf(
                EntryType.MANGA to 5_000L,
            )

            val earlierDetails = repository.getEarlierActivityDetails(
                profileId = 1L,
                type = null,
                limit = 20L,
            )
            earlierDetails.totals.associate { it.type to it.durationMillis } shouldBe mapOf(
                EntryType.MANGA to 5_000L,
            )
            earlierDetails.topEntries.map { it.title to it.durationMillis } shouldBe listOf(
                "Manga" to 5_000L,
            )
        }
    }

    @Test
    fun `statistics ignores short sessions without trimming qualifying sessions or completions`() = runTest {
        withDatabase { database, repository ->
            database.activityQueries.insertActivitySession("qualifying-session", 1L, 1_000L, 12_000L)
            database.activityQueries.upsertActivitySegment(
                "qualifying-session",
                null,
                "2026-08-22",
                "UTC",
                1_000L,
                7_000L,
                6_000L,
            )
            database.activityQueries.upsertActivitySegment(
                "qualifying-session",
                null,
                "2026-08-23",
                "UTC",
                7_000L,
                12_000L,
                5_000L,
            )
            database.activityQueries.updateActivitySession(1_000L, 12_000L, 11_000L, 1L, "qualifying-session")
            database.recordActivitySession(
                sessionId = "short-session",
                entryId = 2L,
                startedAt = 20_000L,
                durationMillis = 9_999L,
            )
            database.activityQueries.insertCompletionEvent(
                "short-session-completion",
                2L,
                12L,
                "short-session",
                29_999L,
                "2026-08-23",
                "UTC",
                "consumption",
            )

            val snapshot = repository.subscribeActivity(1L, "2026-08-22").first()

            snapshot.activity.map { it.localDate to it.durationMillis } shouldBe listOf(
                "2026-08-22" to 6_000L,
                "2026-08-23" to 5_000L,
            )
            snapshot.sessions.single().let { session ->
                session.type shouldBe EntryType.MANGA
                session.sessionCount shouldBe 1L
                session.averageDurationMillis shouldBe 11_000L
                session.longestDurationMillis shouldBe 11_000L
            }
            snapshot.topEntries.single().let { entry ->
                entry.title shouldBe "Manga"
                entry.durationMillis shouldBe 11_000L
            }
            snapshot.completions.single().let { completion ->
                completion.type shouldBe EntryType.ANIME
                completion.count shouldBe 1L
            }
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
                        (1, 11, 1000, 17000),
                        (2, 12, 1000, 30000)
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

private suspend fun Database.recordActivitySession(
    sessionId: String,
    entryId: Long,
    startedAt: Long,
    durationMillis: Long,
) {
    val endedAt = startedAt + durationMillis
    activityQueries.insertActivitySession(sessionId, entryId, startedAt, endedAt)
    activityQueries.upsertActivitySegment(
        sessionId,
        null,
        "2026-08-23",
        "UTC",
        startedAt,
        endedAt,
        durationMillis,
    )
    activityQueries.updateActivitySession(startedAt, endedAt, durationMillis, 0L, sessionId)
}
