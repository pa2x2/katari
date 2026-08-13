package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter

class EntryNotesQueriesTest {
    @Test
    fun `notes update is profile scoped and leaves other entry fields unchanged`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO profiles(_id, uuid, name, color_seed, position)
                    VALUES
                        (2, 'second', 'Second', 1, 1),
                        (3, 'third', 'Third', 2, 2)
                """.trimIndent(),
                parameters = 0,
            )
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO entries(_id, profile_id, source, url, title, notes)
                    VALUES
                        (10, 2, 1, '/target', 'Original title', 'Old target notes'),
                        (11, 3, 1, '/other', 'Other title', 'Other notes')
                """.trimIndent(),
                parameters = 0,
            )
            val database = database(driver)

            database.entriesQueries.updateNotes("New target notes", entryId = 10, profileId = 2)
            database.entriesQueries.updateNotes("Wrong profile", entryId = 11, profileId = 2)

            database.entriesQueries.getEntryById(10, 2).awaitAsOne().run {
                title shouldBe "Original title"
                notes shouldBe "New target notes"
            }
            database.entriesQueries.getEntryById(11, 3).awaitAsOne().notes shouldBe "Other notes"
        } finally {
            driver.close()
        }
    }

    private fun database(driver: JdbcSqliteDriver): Database {
        return Database(
            driver = driver,
            entriesAdapter = Entries.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = UpdateStrategyColumnAdapter,
                memoAdapter = MemoColumnAdapter,
            ),
            chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
            historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
        )
    }
}
