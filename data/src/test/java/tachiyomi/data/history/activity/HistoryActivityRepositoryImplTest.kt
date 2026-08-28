package tachiyomi.data.history.activity

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.kanade.tachiyomi.source.entry.EntryType
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

class HistoryActivityRepositoryImplTest {

    @Test
    fun `details preserve typed date membership and page complete sessions`() = runTest {
        withRepository { database, repository ->
            database.activityQueries.insertActivitySession("older", 1L, 1_000L, 2_000L)
            database.activityQueries.upsertActivitySegment(
                "older",
                11L,
                "2026-08-22",
                "UTC",
                1_000L,
                11_000L,
                10_000L,
            )
            database.activityQueries.updateActivitySession(1_000L, 11_000L, 10_000L, 0L, "older")
            database.activityQueries.insertActivitySession("newer", 1L, 3_000L, 5_000L)
            database.activityQueries.upsertActivitySegment(
                "newer",
                12L,
                "2026-08-23",
                "UTC",
                3_000L,
                23_000L,
                20_000L,
            )
            database.activityQueries.updateActivitySession(3_000L, 23_000L, 20_000L, 0L, "newer")
            database.activityQueries.insertCompletionEvent(
                "completion",
                1L,
                12L,
                "newer",
                5_000L,
                "2026-08-23",
                "UTC",
                "consumption",
            )
            database.activityQueries.insertActivitySession("anime", 2L, 3_000L, 6_000L)
            database.activityQueries.upsertActivitySegment(
                "anime",
                null,
                "2026-08-23",
                "UTC",
                3_000L,
                33_000L,
                30_000L,
            )
            database.activityQueries.updateActivitySession(3_000L, 33_000L, 30_000L, 0L, "anime")

            val firstPage = repository.getActivityPage(
                profileId = 1L,
                startLocalDate = "2026-08-22",
                endLocalDate = "2026-08-23",
                type = EntryType.MANGA,
                offset = 0L,
                limit = 1L,
            )

            firstPage.hasMore shouldBe true
            firstPage.sessions.single().sessionId shouldBe "newer"
            firstPage.sessions.single().completionCount shouldBe 1L
            firstPage.sessions.single().segments.single().chapterTitle shouldBe "Chapter two"

            val secondPage = repository.getActivityPage(
                profileId = 1L,
                startLocalDate = "2026-08-22",
                endLocalDate = "2026-08-23",
                type = EntryType.MANGA,
                offset = 1L,
                limit = 1L,
            )

            secondPage.hasMore shouldBe false
            secondPage.sessions.single().sessionId shouldBe "older"
        }
    }

    @Test
    fun `details omit short sessions unless they contain a completion`() = runTest {
        withRepository { database, repository ->
            database.recordSession("short", durationMillis = 9_999L)
            database.recordSession("qualifying", durationMillis = 10_000L)
            database.recordSession("completed-short", durationMillis = 1_000L)
            database.activityQueries.insertCompletionEvent(
                "completed-short-event",
                1L,
                11L,
                "completed-short",
                2_000L,
                "2026-08-23",
                "UTC",
                "consumption",
            )

            val page = repository.getActivityPage(
                profileId = 1L,
                startLocalDate = "2026-08-23",
                endLocalDate = "2026-08-23",
                type = null,
                offset = 0L,
                limit = 20L,
            )

            page.sessions.map { it.sessionId } shouldBe listOf("qualifying", "completed-short")
        }
    }

    private suspend fun withRepository(
        block: suspend (Database, HistoryActivityRepositoryImpl) -> Unit,
    ) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            driver.await(
                null,
                """
                    INSERT INTO entries(_id, profile_id, source, url, title, type)
                    VALUES
                        (1, 1, 1, '/manga', 'Manga', 'manga'),
                        (2, 1, 1, '/anime', 'Anime', 'anime')
                """.trimIndent(),
                0,
            )
            driver.await(
                null,
                """
                    INSERT INTO chapters(_id, entry_id, url, name)
                    VALUES
                        (11, 1, '/one', 'Chapter one'),
                        (12, 1, '/two', 'Chapter two')
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
            block(database, HistoryActivityRepositoryImpl(AndroidDatabaseHandler(database, driver)))
        } finally {
            driver.close()
        }
    }
}

private suspend fun Database.recordSession(sessionId: String, durationMillis: Long) {
    val startedAt = 1_000L
    val endedAt = startedAt + durationMillis
    activityQueries.insertActivitySession(sessionId, 1L, startedAt, endedAt)
    activityQueries.upsertActivitySegment(
        sessionId,
        11L,
        "2026-08-23",
        "UTC",
        startedAt,
        endedAt,
        durationMillis,
    )
    activityQueries.updateActivitySession(startedAt, endedAt, durationMillis, 0L, sessionId)
}
