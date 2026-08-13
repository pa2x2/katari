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
import tachiyomi.domain.entry.model.EntryChapter

class EntryChapterBatchObservationTest {

    @Test
    fun `empty batch observation emits once and completes`() {
        runBlocking {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            try {
                val repository = EntryChapterRepositoryImpl(
                    handler = AndroidDatabaseHandler(database(driver), driver),
                    profileProvider = FixedProfileProvider(PROFILE_ID),
                )

                repository.getChaptersByEntryIds(emptyList()).toList() shouldBe listOf(emptyList())
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun `large batch observation emits one coherent snapshot per chapter invalidation`() {
        runBlocking {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            try {
                Database.Schema.awaitCreate(driver)
                seed(driver)
                val database = database(driver)
                val handler = AndroidDatabaseHandler(database, driver)
                val repository = EntryChapterRepositoryImpl(handler, FixedProfileProvider(PROFILE_ID))
                val emissions = mutableListOf<List<EntryChapter>>()

                val observation = launch {
                    repository.getChaptersByEntryIds(listOf(502L, 1L, 1L) + (2L..501L)).toList(emissions)
                }
                try {
                    awaitEmissionCount(emissions, 1)
                    emissions.single().size shouldBe 502
                    emissions.single().map(EntryChapter::entryId).toSet() shouldBe (1L..502L).toSet()

                    handler.await(inTransaction = true) {
                        updateRead(chapterId = 1L)
                        updateRead(chapterId = 501L)
                    }
                    awaitEmissionCount(emissions, 2)
                    delay(100)

                    emissions.size shouldBe 2
                    emissions.last().associateBy(EntryChapter::id).run {
                        getValue(1L).read shouldBe true
                        getValue(501L).read shouldBe true
                    }

                    handler.await { updateRead(chapterId = 503L) }
                    awaitEmissionCount(emissions, 3)
                    emissions.last() shouldBe emissions[1]

                    handler.await { chaptersQueries.removeChaptersWithIds(listOf(1L)) }
                    awaitEmissionCount(emissions, 4)
                    emissions.last().any { it.id == 1L } shouldBe false
                    emissions.last().size shouldBe 501
                } finally {
                    observation.cancelAndJoin()
                }
            } finally {
                driver.close()
            }
        }
    }

    private suspend fun Database.updateRead(chapterId: Long) {
        chaptersQueries.update(
            entryId = null,
            url = null,
            name = null,
            scanlator = null,
            read = true,
            bookmark = null,
            chapterNumber = null,
            sourceOrder = null,
            dateFetch = null,
            dateUpload = null,
            version = null,
            isSyncing = null,
            memo = null,
            chapterId = chapterId,
        )
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
        driver.await(
            identifier = null,
            sql = """
                WITH RECURSIVE ids(value) AS (
                    VALUES(1)
                    UNION ALL
                    SELECT value + 1 FROM ids WHERE value < 503
                )
                INSERT INTO entries(_id, profile_id, source, url, title)
                SELECT value, $PROFILE_ID, 10, '/' || value, 'Entry ' || value FROM ids
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                WITH RECURSIVE ids(value) AS (
                    VALUES(1)
                    UNION ALL
                    SELECT value + 1 FROM ids WHERE value < 503
                )
                INSERT INTO chapters(_id, entry_id, url, name)
                SELECT value, value, '/chapter/' || value, 'Chapter ' || value FROM ids
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
    }
}
