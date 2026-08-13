package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter

class EntryNotesRepositoryTest {

    @Test
    fun `update reports whether the profile owns the entry`() = runTest {
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
                    VALUES (10, 2, 1, '/target', 'Target', 'Old notes')
                """.trimIndent(),
                parameters = 0,
            )
            val repository = EntryRepositoryImpl(
                handler = AndroidDatabaseHandler(database(driver), driver),
                profileProvider = FixedProfileProvider(2),
            )

            repository.updateNotes(entryId = 10, profileId = 2, notes = "Updated") shouldBe true
            repository.updateNotes(entryId = 10, profileId = 3, notes = "Wrong profile") shouldBe false
            repository.updateNotes(entryId = 11, profileId = 2, notes = "Missing") shouldBe false
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

    private class FixedProfileProvider(
        override val activeProfileId: Long,
    ) : ActiveProfileProvider {
        override val activeProfileIdFlow: Flow<Long> = flowOf(activeProfileId)
    }
}
