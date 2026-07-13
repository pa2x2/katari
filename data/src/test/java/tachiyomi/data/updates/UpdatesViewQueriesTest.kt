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
    fun `started filter uses playback for anime and page progress for manga`() = runTest {
        withSeededDatabase { database ->
            database.updatesViewQueries.filteredChapterNames(started = true) shouldContainExactlyInAnyOrder listOf(
                "Manga started",
                "Anime partial",
                "Anime partial unknown duration",
                "Anime playback completed",
                "Anime consumed",
            )
        }
    }

    @Test
    fun `not started filter excludes anime consumption and playback progress`() = runTest {
        withSeededDatabase { database ->
            database.updatesViewQueries.filteredChapterNames(started = false) shouldContainExactlyInAnyOrder listOf(
                "Manga untouched",
                "Anime untouched",
            )
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
                    (2, 1, 2, '/anime', 'Anime', 1, 0, 'anime')
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO chapters(
                    _id, entry_id, url, name, read, last_page_read, date_upload, date_fetch
                )
                VALUES
                    (11, 1, '/manga/untouched', 'Manga untouched', 0, 0, 10, 10),
                    (12, 1, '/manga/started', 'Manga started', 0, 4, 10, 10),
                    (13, 1, '/manga/consumed', 'Manga consumed', 1, 4, 10, 10),
                    (21, 2, '/anime/untouched', 'Anime untouched', 0, 0, 10, 10),
                    (22, 2, '/anime/partial', 'Anime partial', 0, 0, 10, 10),
                    (23, 2, '/anime/completed', 'Anime playback completed', 0, 0, 10, 10),
                    (24, 2, '/anime/consumed', 'Anime consumed', 1, 0, 10, 10),
                    (25, 2, '/anime/partial-unknown', 'Anime partial unknown duration', 0, 0, 10, 10)
            """.trimIndent(),
            parameters = 0,
        )
        driver.await(
            identifier = null,
            sql = """
                INSERT INTO entry_progress_state(
                    entry_id, chapter_id, resource_key, locator_kind, position, extent, completed, locator_updated_at
                )
                VALUES
                    (2, 22, '/anime/partial', 'time', 100, 1000, 0, 10),
                    (2, 23, '/anime/completed', 'time', 1000, 1000, 1, 10),
                    (2, 25, '/anime/partial-unknown', 'time', 100, NULL, 0, 10)
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
        ).awaitAsList().map { it.chapterName }
    }
}
