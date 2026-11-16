package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.shouldBe
import io.mockk.mockk
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
import tachiyomi.domain.entry.model.RenamedChapter

class RenamedChapterRecordTest {

    @Test
    fun `a source sync and another device renaming chapters leave their former names, and other changes none`() =
        runTest {
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
                    sql = """
                        INSERT INTO chapters(_id, entry_id, url, name, scanlator)
                        VALUES (2, 1, '/a', 'Chapter 1', 'Group'), (3, 1, '/b', 'Chapter 2', NULL),
                            (4, 1, '/c', 'Chapter 3', NULL);
                    """.trimIndent(),
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

                val titled = chapters.getChapterById(2)!!.copy(name = "Chapter 1 - The Start", scanlator = null)
                val read = chapters.getChapterById(4)!!.copy(read = true, chapterNumber = 3.0)
                chapters.applySourceSync(
                    removedIds = emptyList(),
                    updated = listOf(titled, read),
                    inserted = emptyList(),
                    listedIds = listOf(2L, 4L),
                )
                // As sync applies a chapter another device's refresh found at a new URL
                database.sync_applyQueries.renameChapter(url = "/b2", chapterId = 3)

                chapters.subscribeRenamedChapters().first().map { it.copy(id = 0) } shouldBe listOf(
                    RenamedChapter(0, chapterId = 2, entryId = 1, profileId = 1, "Chapter 1", "Group", "/a"),
                    RenamedChapter(0, chapterId = 3, entryId = 1, profileId = 1, "Chapter 2", null, "/b"),
                )
            } finally {
                driver.close()
            }
        }
}
