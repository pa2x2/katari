package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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

class EntryBatchReadTest {

    @Test
    fun `batch read supports large duplicate input and active profile scope`() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            seed(driver)
            val repository = EntryRepositoryImpl(
                handler = AndroidDatabaseHandler(database(driver), driver),
                profileProvider = FixedProfileProvider(PROFILE_ID),
            )

            val entries = repository.getEntriesByIds(listOf(502L, 1L, 1L) + (2L..501L) + 503L)

            entries.map { it.id } shouldContainExactly (1L..502L).toList()
        } finally {
            driver.close()
        }
    }

    private suspend fun seed(driver: JdbcSqliteDriver) {
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO profiles(_id, uuid, name, color_seed, position)
                VALUES ($PROFILE_ID, 'profile', 'Profile', 1, 1), ($OTHER_PROFILE_ID, 'other', 'Other', 2, 2)
            """.trimIndent(),
            parameters = 0,
        )
        for (id in 1L..502L) {
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO entries(_id, profile_id, source, url, title)
                    VALUES ($id, $PROFILE_ID, 10, '/$id', 'Entry $id')
                """.trimIndent(),
                parameters = 0,
            )
        }
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO entries(_id, profile_id, source, url, title)
                VALUES (503, $OTHER_PROFILE_ID, 10, '/503', 'Other profile')
            """.trimIndent(),
            parameters = 0,
        )
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

    private companion object {
        const val PROFILE_ID = 2L
        const val OTHER_PROFILE_ID = 3L
    }
}
