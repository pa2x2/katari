package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.booleans.shouldBeTrue
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
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState

class EntryProgressRepositoryImplTest {
    @Test
    fun `synchronized completion projects to child and progress survives child deletion`() = runTest {
        withDatabase { database, repository ->
            repository.upsertAndSyncChild(state(completed = true))

            database.chaptersQueries.getChapterById(2).awaitAsOne().read.shouldBeTrue()

            database.chaptersQueries.removeChaptersWithIds(listOf(2))

            val retained = repository.get(1, "", "/chapter")!!
            retained.chapterId shouldBe null
            retained.completed.shouldBeTrue()
        }
    }

    private suspend fun withDatabase(block: suspend (Database, EntryProgressRepositoryImpl) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(null, "PRAGMA foreign_keys = ON", 0)
            seed(driver)
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
            val repository = EntryProgressRepositoryImpl(
                AndroidDatabaseHandler(
                    db = database,
                    driver = driver,
                ),
            )
            block(database, repository)
        } finally {
            driver.close()
        }
    }

    private suspend fun seed(driver: JdbcSqliteDriver) {
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO entries(_id, profile_id, source, url, title, favorite, date_added, type)
                VALUES
                    (1, 1, 1, '/entry', 'Entry', 1, 0, 'manga')
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO chapters(_id, entry_id, url, name)
                VALUES
                    (2, 1, '/chapter', 'Chapter')
            """.trimIndent(),
            parameters = 0,
        )
    }

    private fun state(completed: Boolean): EntryProgressState {
        return EntryProgressState(
            entryId = 1,
            chapterId = 2,
            resourceKey = "/chapter",
            locator = EntryProgressLocator(kind = "page", position = 4),
            completed = completed,
            locatorUpdatedAt = 10,
            completionUpdatedAt = 10,
        )
    }
}
