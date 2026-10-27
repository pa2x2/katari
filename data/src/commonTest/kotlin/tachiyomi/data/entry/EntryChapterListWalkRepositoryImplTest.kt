package tachiyomi.data.entry

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
import tachiyomi.domain.entry.model.ChapterListWalk

class EntryChapterListWalkRepositoryImplTest {

    // Writing the walk as a replace deleted its row, and the delete cascaded to the chapters it listed, so the last
    // part's Complete removed every chapter the earlier parts had saved
    @Test
    fun `moving a walk on keeps the chapters it listed, and a finished walk stays finished`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO entries(_id, profile_id, source, url, title, favorite, date_added, type)
                    VALUES (1, 1, 1, '/entry', 'Entry', 1, 0, 'book');
                """.trimIndent(),
                parameters = 0,
            )
            driver.await(
                identifier = null,
                sql = "INSERT INTO chapters(_id, entry_id, url, name) VALUES (2, 1, '/a', 'A'), (3, 1, '/b', 'B')",
                parameters = 0,
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
            val walks = EntryChapterListWalkRepositoryImpl(AndroidDatabaseHandler(db = database, driver = driver))
            val walk = ChapterListWalk(1, 1, "page-2", nextSourceOrder = 2, loaded = 1, total = 3)

            walks.start(walk)
            walks.markListed(1, listOf(2L))
            walks.advance(walk.copy(token = "page-3", loaded = 2))
            walks.markListed(1, listOf(3L))
            val listed = walks.getListedChapterIds(1)
            walks.finish(1)
            walks.advance(walk.copy(token = "page-4"))

            listed shouldBe setOf(2L, 3L)
            walks.get(1) shouldBe null
        } finally {
            driver.close()
        }
    }
}
