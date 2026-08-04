package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.EntriesQueries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter

class UpcomingEntriesQueriesTest {

    @Test
    fun `included categories support every entry type and category zero is uncategorized`() = runTest {
        withSeededDatabase { database ->
            database.entriesQueries.filteredEntryIds(includedCategories = listOf(10))
                .filter { it <= 3 } shouldContainExactlyInAnyOrder listOf(1L, 2L, 3L)

            database.entriesQueries.filteredEntryIds(includedCategories = listOf(0))
                .filter { it <= 5 } shouldContainExactlyInAnyOrder listOf(5L)

            database.entriesQueries.filteredEntryIds(excludedCategories = listOf(0))
                .filter { it <= 5 } shouldContainExactlyInAnyOrder listOf(1L, 2L, 3L, 4L)
        }
    }

    @Test
    fun `excluded category wins over included category`() = runTest {
        withSeededDatabase { database ->
            database.entriesQueries.filteredEntryIds(
                includedCategories = listOf(10),
                excludedCategories = listOf(20),
            ).filter { it <= 3 } shouldContainExactlyInAnyOrder listOf(1L, 3L)
        }
    }

    @Test
    fun `category membership from another profile is ignored`() = runTest {
        withSeededDatabase { database ->
            database.entriesQueries.filteredEntryIds(includedCategories = listOf(10))
                .filter { it == 4L } shouldContainExactlyInAnyOrder emptyList()

            database.entriesQueries.filteredEntryIds(excludedCategories = listOf(10))
                .filter { it == 4L } shouldContainExactlyInAnyOrder listOf(4L)
        }
    }

    private suspend fun withSeededDatabase(block: suspend (Database) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            seed(driver)
            block(
                Database(
                    driver = driver,
                    entriesAdapter = Entries.Adapter(
                        genreAdapter = StringListColumnAdapter,
                        update_strategyAdapter = UpdateStrategyColumnAdapter,
                        memoAdapter = MemoColumnAdapter,
                    ),
                    chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
                    historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
                ),
            )
        } finally {
            driver.close()
        }
    }

    private suspend fun seed(driver: JdbcSqliteDriver) {
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO profiles(_id, uuid, name, color_seed, position)
                VALUES (2, 'secondary', 'Secondary', 0, 1)
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO entries(_id, profile_id, source, url, title, status, favorite, next_update, type)
                VALUES
                    (1, 1, 1, '/manga', 'Manga', 1, 1, 100, 'manga'),
                    (2, 1, 2, '/anime', 'Anime', 1, 1, 100, 'anime'),
                    (3, 1, 3, '/book', 'Book', 1, 1, 100, 'book'),
                    (4, 1, 4, '/other-profile-category', 'Other profile category', 1, 1, 100, 'manga'),
                    (5, 1, 5, '/uncategorized', 'Uncategorized', 1, 1, 100, 'book')
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO categories(_id, profile_id, name, sort, flags)
                VALUES
                    (10, 1, 'Included', 0, 0),
                    (20, 1, 'Excluded', 1, 0),
                    (30, 1, 'Other', 2, 0)
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO entries_categories(profile_id, entry_id, category_id)
                VALUES
                    (1, 1, 10),
                    (1, 2, 10),
                    (1, 2, 20),
                    (1, 3, 10),
                    (1, 4, 30),
                    (2, 4, 10)
            """.trimIndent(),
            parameters = 0,
        )
    }

    private suspend fun EntriesQueries.filteredEntryIds(
        includedCategories: List<Long> = emptyList(),
        excludedCategories: List<Long> = emptyList(),
    ): List<Long> {
        return getUpcomingEntries(
            profileId = 1,
            startOfDay = 0,
            statuses = listOf(1),
            types = listOf("manga", "anime", "book"),
            includedEmpty = includedCategories.isEmpty(),
            includedCategories = includedCategories,
            excludedEmpty = excludedCategories.isEmpty(),
            excludedCategories = excludedCategories,
        ).awaitAsList().map { it._id }
    }
}
