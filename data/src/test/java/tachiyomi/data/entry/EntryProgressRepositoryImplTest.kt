package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
    fun `repository preserves common locator fields and extensions`() = runTest {
        withDatabase { _, repository ->
            val state = state(
                locator = EntryProgressLocator(
                    kind = "reader.example",
                    position = 4,
                    extent = 10,
                    progression = 0.4,
                    totalProgression = 0.25,
                    extensions = buildJsonObject {
                        put("reader.example.precise", JsonPrimitive("opaque"))
                    },
                ),
            )

            repository.upsert(state)

            repository.get(1, "", "/chapter") shouldBe state
        }
    }

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

    @Test
    fun `rekey moves progress identity when a source changes its child url`() = runTest {
        withDatabase { _, repository ->
            val state = state()
            repository.upsert(state)

            repository.rekey(
                entryId = 1,
                chapterId = 2,
                oldContentKey = "",
                oldResourceKey = "/chapter",
                newContentKey = "",
                newResourceKey = "/chapter-new",
            )

            repository.get(1, "", "/chapter") shouldBe null
            repository.get(1, "", "/chapter-new") shouldBe state.copy(resourceKey = "/chapter-new")
        }
    }

    @Test
    fun `bulk lookup returns every state across SQL parameter chunks in stable order`() = runTest {
        withDatabaseDriver { driver, _, repository ->
            driver.await(
                identifier = null,
                sql = """
                    WITH RECURSIVE ids(id) AS (
                        SELECT 1000
                        UNION ALL
                        SELECT id + 1 FROM ids WHERE id < 1500
                    )
                    INSERT INTO entries(_id, profile_id, source, url, title, favorite, date_added, type)
                    SELECT id, 1, 1, '/entry-' || id, 'Entry ' || id, 1, 0, 'manga'
                    FROM ids
                """.trimIndent(),
                parameters = 0,
            )
            driver.await(
                identifier = null,
                sql = """
                    WITH RECURSIVE ids(id) AS (
                        SELECT 1000
                        UNION ALL
                        SELECT id + 1 FROM ids WHERE id < 1500
                    )
                    INSERT INTO entry_progress_state(
                        entry_id,
                        resource_key,
                        locator_kind
                    )
                    SELECT id, '/resource-' || id, 'page'
                    FROM ids
                """.trimIndent(),
                parameters = 0,
            )
            val entryIds = (1000L..1500L).toSet()

            repository.getByEntryIds(entryIds).map(EntryProgressState::entryId) shouldBe entryIds.toList()
        }
    }

    private suspend fun withDatabase(block: suspend (Database, EntryProgressRepositoryImpl) -> Unit) {
        withDatabaseDriver { _, database, repository -> block(database, repository) }
    }

    private suspend fun withDatabaseDriver(
        block: suspend (JdbcSqliteDriver, Database, EntryProgressRepositoryImpl) -> Unit,
    ) {
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
            block(driver, database, repository)
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

    private fun state(
        locator: EntryProgressLocator = EntryProgressLocator(kind = "page", position = 4),
        completed: Boolean = false,
    ): EntryProgressState {
        return EntryProgressState(
            entryId = 1,
            chapterId = 2,
            resourceKey = "/chapter",
            locator = locator,
            completed = completed,
            locatorUpdatedAt = 10,
            completionUpdatedAt = 10,
        )
    }
}
