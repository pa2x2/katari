package tachiyomi.data.history.activity

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
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
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySessionSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySnapshot
import tachiyomi.domain.history.model.activity.HistoryCompletionCause
import tachiyomi.domain.history.model.activity.HistoryCompletionSnapshot

class HistoryActivityBackupTest {

    @Test
    fun `restoring activity repeatedly preserves one unattributed segment and the earliest epoch`() = runTest {
        withRepository { repository ->
            val snapshot = HistoryActivitySnapshot(
                sessions = listOf(
                    HistoryActivitySessionSnapshot(
                        sessionId = "stable-session",
                        startedAtEpochMillis = 1_000L,
                        endedAtEpochMillis = 2_000L,
                        durationMillis = 1_000L,
                        lastSequence = 3L,
                        segments = listOf(
                            HistoryActivitySegmentSnapshot(
                                chapterId = null,
                                localDate = "2026-08-23",
                                timeZoneId = "UTC",
                                startedAtEpochMillis = 1_000L,
                                endedAtEpochMillis = 2_000L,
                                durationMillis = 1_000L,
                            ),
                        ),
                    ),
                ),
                completions = listOf(
                    HistoryCompletionSnapshot(
                        eventId = "stable-completion",
                        chapterId = null,
                        sessionId = "stable-session",
                        occurredAtEpochMillis = 2_000L,
                        localDate = "2026-08-23",
                        timeZoneId = "UTC",
                        cause = HistoryCompletionCause.CONSUMPTION,
                    ),
                ),
            )

            repository.restoreActivity(1L, snapshot)
            repository.restoreActivity(1L, snapshot)
            repository.restoreStatisticsEpoch(1L, 2_000L)
            repository.restoreStatisticsEpoch(1L, 3_000L)

            val restored = repository.getActivityByEntryId(1L)
            restored.sessions.size shouldBe 1
            restored.sessions.single().durationMillis shouldBe 1_000L
            restored.sessions.single().segments.size shouldBe 1
            restored.sessions.single().segments.single().durationMillis shouldBe 1_000L
            restored.completions.size shouldBe 1
            repository.getStatisticsEpoch(1L) shouldBe 2_000L
        }
    }

    private suspend fun withRepository(block: suspend (HistoryActivityBackupRepositoryImpl) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            driver.await(
                null,
                "INSERT INTO entries(_id, profile_id, source, url, title, type) " +
                    "VALUES (1, 1, 1, '/entry', 'Entry', 'manga')",
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
            block(HistoryActivityBackupRepositoryImpl(handler))
        } finally {
            driver.close()
        }
    }
}
