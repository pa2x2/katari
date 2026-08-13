package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

class EntryBatchObservationTest {

    @Test
    fun `batch observation supports large id sets and suppresses unrelated writes`() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            seed(driver)
            val database = database(driver)
            val repository = EntryRepositoryImpl(
                handler = AndroidDatabaseHandler(database, driver),
                profileProvider = FixedProfileProvider(PROFILE_ID),
            )
            val emissions = mutableListOf<List<tachiyomi.domain.entry.model.Entry>>()

            val observation = launch {
                repository.getEntriesByIdsAsFlow(listOf(502L, 1L, 1L) + (2L..501L)).toList(emissions)
            }
            awaitEmissionCount(emissions, 1)

            emissions.single().run {
                size shouldBe 502
                first().id shouldBe 1L
                last().id shouldBe 502L
            }

            database.entriesQueries.updateNotes("Unrelated", entryId = 503, profileId = PROFILE_ID)
            delay(100)
            emissions.size shouldBe 1

            database.entriesQueries.updateNotes("Updated", entryId = 1, profileId = PROFILE_ID)
            awaitEmissionCount(emissions, 2)

            emissions.size shouldBe 2
            emissions.last().first().notes shouldBe "Updated"
            observation.cancelAndJoin()
        } finally {
            driver.close()
        }
    }

    private suspend fun awaitEmissionCount(emissions: List<*>, expected: Int) {
        withTimeout(5_000) {
            while (emissions.size < expected) delay(10)
        }
    }

    private suspend fun seed(driver: JdbcSqliteDriver) {
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO profiles(_id, uuid, name, color_seed, position)
                VALUES ($PROFILE_ID, 'profile', 'Profile', 1, 1)
            """.trimIndent(),
            parameters = 0,
        )
        for (id in 1L..503L) {
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO entries(_id, profile_id, source, url, title)
                    VALUES ($id, $PROFILE_ID, 10, '/$id', 'Entry $id')
                """.trimIndent(),
                parameters = 0,
            )
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

    private companion object {
        const val PROFILE_ID = 2L
    }
}
