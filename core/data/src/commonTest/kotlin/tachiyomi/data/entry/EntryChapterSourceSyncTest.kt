package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
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
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState

class EntryChapterSourceSyncTest {

    @Test
    fun `a source sync failing part way leaves the chapters and their progress as they were`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO entries(_id, profile_id, source, url, title, favorite, date_added, type)
                    VALUES (1, 1, 1, '/entry', 'Entry', 1, 0, 'manga');
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
            val handler = AndroidDatabaseHandler(db = database, driver = driver)
            val chapters = EntryChapterRepositoryImpl(handler, mockk(relaxed = true))
            val progress = EntryProgressRepositoryImpl(handler)
            progress.upsert(
                EntryProgressState(
                    entryId = 1,
                    chapterId = 3,
                    resourceKey = "/b",
                    locator = EntryProgressLocator(kind = "page", position = 4),
                    locatorUpdatedAt = 10,
                ),
            )
            val renamed = chapters.getChapterById(3)!!.copy(url = "/b2")
            val added = EntryChapter.create().copy(entryId = 1, url = "/c", name = "C")

            runCatching {
                chapters.applySourceSync(listOf(2L), listOf(renamed), listOf(added), afterUpdates = {
                    progress.rekey(1, 3, "", "/b", "", "/b2")
                    error("Sync failed")
                })
            }

            database.chaptersQueries.getChaptersByEntryId(1, 0).awaitAsList().map { it.url }.sorted() shouldBe
                listOf("/a", "/b")
            progress.get(1, "", "/b").shouldNotBeNull()
            progress.get(1, "", "/b2").shouldBeNull()
        } finally {
            driver.close()
        }
    }
}
