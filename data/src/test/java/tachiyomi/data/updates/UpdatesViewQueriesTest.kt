package tachiyomi.data.updates

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
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.view.UpdatesViewQueries

class UpdatesViewQueriesTest {

    @Test
    fun `initial chapters fetched at or before library addition are excluded from updates`() = runTest {
        withSeededDatabase { database ->
            database.updatesViewQueries.getRecentUpdates(
                profileId = 1,
                after = -1,
                limit = 100,
            ).awaitAsList()
                .filter { it.entryId == 4L }
                .map { it.chapterName } shouldContainExactlyInAnyOrder listOf("Post-library update")
        }
    }

    @Test
    fun `started filter uses progress locators across entry types`() = runTest {
        withSeededDatabase { database ->
            database.updatesViewQueries.filteredChapterNames(started = true) shouldContainExactlyInAnyOrder listOf(
                "Manga started",
                "Manga consumed",
                "Anime partial",
                "Anime partial unknown duration",
                "Anime playback completed",
                "Anime consumed",
                "Book partial resource",
                "Book partial total",
            )
        }
    }

    @Test
    fun `not started filter excludes anime consumption and playback progress`() = runTest {
        withSeededDatabase { database ->
            database.updatesViewQueries.filteredChapterNames(started = false) shouldContainExactlyInAnyOrder listOf(
                "Manga untouched",
                "Anime untouched",
                "Book untouched",
            )
        }
    }

    @Test
    fun `included categories support every entry type and category zero is uncategorized`() = runTest {
        withSeededDatabase { database ->
            database.updatesViewQueries.filteredEntryIds(includedCategories = listOf(10))
                .filter { it <= 3 } shouldContainExactlyInAnyOrder listOf(1L, 2L, 3L)

            database.updatesViewQueries.filteredEntryIds(includedCategories = listOf(0))
                .filter { it <= 5 } shouldContainExactlyInAnyOrder listOf(5L)

            database.updatesViewQueries.filteredEntryIds(
                includedCategories = listOf(0),
                excludedCategories = listOf(0),
            ).filter { it <= 5 } shouldContainExactlyInAnyOrder emptyList()
        }
    }

    @Test
    fun `excluded category wins over included category`() = runTest {
        withSeededDatabase { database ->
            database.updatesViewQueries.filteredEntryIds(
                includedCategories = listOf(10),
                excludedCategories = listOf(20),
            ).filter { it <= 3 } shouldContainExactlyInAnyOrder listOf(1L, 3L)
        }
    }

    @Test
    fun `category membership from another profile is ignored`() = runTest {
        withSeededDatabase { database ->
            database.updatesViewQueries.filteredEntryIds(includedCategories = listOf(10))
                .filter { it == 4L } shouldContainExactlyInAnyOrder emptyList()
        }
    }

    @Test
    fun `merged entry filters use the visible entry category union`() = runTest {
        withSeededDatabase { database ->
            database.updatesViewQueries.filteredEntryIds(includedCategories = listOf(10))
                .filter { it == 7L } shouldContainExactlyInAnyOrder listOf(7L)

            database.updatesViewQueries.filteredEntryIds(
                includedCategories = emptyList(),
                excludedCategories = listOf(10),
            ).filter { it == 7L } shouldContainExactlyInAnyOrder emptyList()
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
                INSERT INTO entries(_id, profile_id, source, url, title, favorite, date_added, type)
                VALUES
                    (1, 1, 1, '/manga', 'Manga', 1, 0, 'manga'),
                    (2, 1, 2, '/anime', 'Anime', 1, 0, 'anime'),
                    (3, 1, 3, '/book', 'Book', 1, 0, 'book'),
                    (4, 1, 4, '/new-library-entry', 'New library entry', 1, 20, 'manga'),
                    (5, 1, 5, '/uncategorized-book', 'Uncategorized book', 1, 0, 'book'),
                    (6, 1, 6, '/merged-target', 'Merged target', 1, 0, 'manga'),
                    (7, 1, 7, '/merged-member', 'Merged member', 1, 0, 'manga')
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO chapters(
                    _id, entry_id, url, name, read, date_upload, date_fetch
                )
                VALUES
                    (11, 1, '/manga/untouched', 'Manga untouched', 0, 10, 10),
                    (12, 1, '/manga/started', 'Manga started', 0, 10, 10),
                    (13, 1, '/manga/consumed', 'Manga consumed', 1, 10, 10),
                    (21, 2, '/anime/untouched', 'Anime untouched', 0, 10, 10),
                    (22, 2, '/anime/partial', 'Anime partial', 0, 10, 10),
                    (23, 2, '/anime/completed', 'Anime playback completed', 0, 10, 10),
                    (24, 2, '/anime/consumed', 'Anime consumed', 1, 10, 10),
                    (25, 2, '/anime/partial-unknown', 'Anime partial unknown duration', 0, 10, 10),
                    (31, 3, '/book/untouched', 'Book untouched', 0, 10, 10),
                    (32, 3, '/book/partial-resource', 'Book partial resource', 0, 10, 10),
                    (33, 3, '/book/partial-total', 'Book partial total', 0, 10, 10),
                    (41, 4, '/initial', 'Initial chapter', 0, 0, 10),
                    (42, 4, '/update', 'Post-library update', 0, 0, 30),
                    (43, 4, '/initial-at-add', 'Initial chapter at add', 0, 0, 20),
                    (51, 5, '/book/uncategorized', 'Book uncategorized', 0, 0, 10),
                    (71, 7, '/merged/update', 'Merged member update', 0, 10, 10)
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO entry_progress_state(
                    entry_id, chapter_id, resource_key, locator_kind, position, extent,
                    progression, total_progression, completed, locator_updated_at
                )
                VALUES
                    (1, 12, '/manga/started', 'page', 4, NULL, NULL, NULL, 0, 10),
                    (2, 22, '/anime/partial', 'time', 100, 1000, NULL, NULL, 0, 10),
                    (2, 23, '/anime/completed', 'time', 1000, 1000, NULL, NULL, 1, 10),
                    (2, 25, '/anime/partial-unknown', 'time', 100, NULL, NULL, NULL, 0, 10),
                    (3, 32, '/book/partial-resource', 'book', NULL, NULL, 0.4, NULL, 0, 10),
                    (3, 33, '/book/partial-total', 'book', NULL, NULL, 0.0, 0.2, 0, 10)
            """.trimIndent(),
            parameters = 0,
        )
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
                    (2, 4, 10),
                    (1, 6, 10),
                    (1, 7, 20)
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO merged_entries(profile_id, target_entry_id, entry_id, position)
                VALUES
                    (1, 6, 6, 0),
                    (1, 6, 7, 1)
            """.trimIndent(),
            parameters = 0,
        )
    }

    private suspend fun UpdatesViewQueries.filteredChapterNames(started: Boolean): List<String> {
        return getRecentUpdatesWithFilters(
            profileId = 1,
            after = 0,
            limit = 100,
            read = null,
            started = if (started) 1 else 0,
            bookmarked = null,
            hideExcludedScanlators = 0,
            includedEmpty = true,
            excludedEmpty = true,
            includedCategories = emptyList(),
            excludedCategories = emptyList(),
        ).awaitAsList()
            .filter { it.entryId <= 5L }
            .map { it.chapterName }
    }

    private suspend fun UpdatesViewQueries.filteredEntryIds(
        includedCategories: List<Long>,
        excludedCategories: List<Long> = emptyList(),
    ): List<Long> {
        return getRecentUpdatesWithFilters(
            profileId = 1,
            after = -1,
            limit = 100,
            read = null,
            started = null,
            bookmarked = null,
            hideExcludedScanlators = 0,
            includedEmpty = includedCategories.isEmpty(),
            excludedEmpty = excludedCategories.isEmpty(),
            includedCategories = includedCategories,
            excludedCategories = excludedCategories,
        ).awaitAsList().map { it.entryId }.distinct()
    }
}
