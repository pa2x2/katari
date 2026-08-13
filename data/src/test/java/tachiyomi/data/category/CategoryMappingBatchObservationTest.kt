package tachiyomi.data.category

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

class CategoryMappingBatchObservationTest {

    @Test
    fun `empty batch observation emits once and completes`() {
        runBlocking {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            try {
                val repository = CategoryRepositoryImpl(
                    handler = AndroidDatabaseHandler(database(driver), driver),
                    profileProvider = FixedProfileProvider(PROFILE_ID),
                )

                repository.observeCategoryIdsByEntryIds(PROFILE_ID, emptyList()).toList() shouldBe
                    listOf(emptyMap())
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun `large batch observation emits one coherent snapshot per mapping invalidation`() {
        runBlocking {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            try {
                Database.Schema.awaitCreate(driver)
                seed(driver)
                val database = database(driver)
                val handler = AndroidDatabaseHandler(database, driver)
                val repository = CategoryRepositoryImpl(handler, FixedProfileProvider(PROFILE_ID))
                val emissions = mutableListOf<Map<Long, List<Long>>>()

                val observation = launch {
                    repository
                        .observeCategoryIdsByEntryIds(PROFILE_ID, listOf(502L, 1L, 1L) + (2L..501L))
                        .toList(emissions)
                }
                try {
                    awaitEmissionCount(emissions, 1)
                    emissions.single().run {
                        size shouldBe 502
                        keys shouldBe (1L..502L).toSet()
                        values.all { it == listOf(FIRST_CATEGORY_ID) } shouldBe true
                    }

                    handler.await(inTransaction = true) {
                        replaceMapping(entryId = 1L, categoryId = SECOND_CATEGORY_ID)
                        replaceMapping(entryId = 501L, categoryId = SECOND_CATEGORY_ID)
                    }
                    awaitEmissionCount(emissions, 2)
                    delay(100)

                    emissions.size shouldBe 2
                    emissions.last().run {
                        getValue(1L) shouldBe listOf(SECOND_CATEGORY_ID)
                        getValue(501L) shouldBe listOf(SECOND_CATEGORY_ID)
                    }

                    handler.await(inTransaction = true) {
                        replaceMapping(entryId = 503L, categoryId = SECOND_CATEGORY_ID)
                    }
                    awaitEmissionCount(emissions, 3)
                    delay(100)
                    emissions.size shouldBe 3
                    emissions.last() shouldBe emissions[1]

                    handler.await {
                        entries_categoriesQueries.deleteByEntryId(PROFILE_ID, 1L)
                    }
                    awaitEmissionCount(emissions, 4)
                    emissions.last().containsKey(1L) shouldBe false
                    emissions.last().size shouldBe 501
                } finally {
                    observation.cancelAndJoin()
                }
            } finally {
                driver.close()
            }
        }
    }

    private suspend fun Database.replaceMapping(entryId: Long, categoryId: Long) {
        entries_categoriesQueries.deleteByEntryId(PROFILE_ID, entryId)
        entries_categoriesQueries.insert(PROFILE_ID, entryId, categoryId)
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
                INSERT INTO categories(_id, profile_id, name, sort, flags)
                VALUES
                    ($FIRST_CATEGORY_ID, $PROFILE_ID, 'First', 1, 0),
                    ($SECOND_CATEGORY_ID, $PROFILE_ID, 'Second', 2, 0)
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
                INSERT INTO entries_categories(profile_id, entry_id, category_id)
                SELECT $PROFILE_ID, _id, $FIRST_CATEGORY_ID FROM entries
                WHERE profile_id = $PROFILE_ID
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
        const val FIRST_CATEGORY_ID = 10L
        const val SECOND_CATEGORY_ID = 11L
    }
}
