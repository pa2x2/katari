package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
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
import tachiyomi.domain.entry.model.Entry
import java.sql.SQLException

class EntryBatchPersistenceTest {

    @Test
    fun `large batch preserves input order and keeps same url entries of different types apart`() = runTest {
        withRepository { _, _, repository ->
            val networkEntries = buildList {
                add(entry("/same", "First", EntryType.MANGA))
                add(entry("/same", "Second", EntryType.ANIME))
                repeat(98) { index ->
                    add(entry("/entry-$index", "Entry $index", EntryType.BOOK))
                }
            }

            val persisted = repository.insertOrUpdateBatch(
                networkEntries,
                PROFILE_ID,
            )

            persisted.map(Entry::title) shouldContainExactly networkEntries.map(Entry::title)
            persisted.take(2).map(Entry::type) shouldContainExactly listOf(EntryType.MANGA, EntryType.ANIME)
        }
    }

    @Test
    fun `batch preserves favorites and applies existing nonfavorite update rules`() = runTest {
        withRepository { _, driver, repository ->
            repository.insertOrUpdateBatch(
                listOf(
                    entry("/favorite", "Favorite original", EntryType.MANGA),
                    entry("/nonfavorite", "Nonfavorite original", EntryType.MANGA),
                ),
                PROFILE_ID,
            )
            driver.await(
                identifier = null,
                sql = "UPDATE entries SET favorite = 1 WHERE url = '/favorite'",
                parameters = 0,
            )

            val persisted = repository.insertOrUpdateBatch(
                listOf(
                    entry("/favorite", "Favorite network update", EntryType.MANGA),
                    entry("/nonfavorite", "Nonfavorite network update", EntryType.MANGA),
                ),
                PROFILE_ID,
            )

            persisted.map(Entry::title) shouldContainExactly
                listOf("Favorite original", "Nonfavorite network update")
            persisted.map(Entry::favorite) shouldContainExactly listOf(true, false)
        }
    }

    @Test
    fun `batch commits successful prefix and rethrows first persistence failure`() = runTest {
        withRepository { database, driver, repository ->
            driver.await(
                identifier = null,
                sql = """
                    CREATE TRIGGER reject_entry BEFORE INSERT ON entries
                    WHEN new.url = '/fail'
                    BEGIN
                        SELECT RAISE(FAIL, 'rejected entry');
                    END
                """.trimIndent(),
                parameters = 0,
            )

            shouldThrow<SQLException> {
                repository.insertOrUpdateBatch(
                    listOf(
                        entry("/persisted", "Persisted", EntryType.BOOK),
                        entry("/fail", "Failure", EntryType.BOOK),
                        entry("/not-attempted", "Not attempted", EntryType.BOOK),
                    ),
                    PROFILE_ID,
                )
            }.message shouldContain "rejected entry"

            database.entriesQueries.getAllEntries(PROFILE_ID).awaitAsList()
                .map { it.url } shouldContainExactly listOf("/persisted")
        }
    }

    private suspend fun withRepository(
        block: suspend (Database, JdbcSqliteDriver, EntryRepositoryImpl) -> Unit,
    ) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO profiles(_id, uuid, name, color_seed, position)
                    VALUES ($PROFILE_ID, 'profile', 'Profile', 1, 1)
                """.trimIndent(),
                parameters = 0,
            )
            val database = database(driver)
            val repository = EntryRepositoryImpl(
                handler = AndroidDatabaseHandler(database, driver),
                profileProvider = FixedProfileProvider(PROFILE_ID),
            )
            block(database, driver, repository)
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

    private fun entry(
        url: String,
        title: String,
        type: EntryType,
    ): Entry {
        return Entry.create().copy(
            profileId = PROFILE_ID,
            source = SOURCE_ID,
            url = url,
            title = title,
            type = type,
        )
    }

    private class FixedProfileProvider(
        override val activeProfileId: Long,
    ) : ActiveProfileProvider {
        override val activeProfileIdFlow: Flow<Long> = flowOf(activeProfileId)
    }

    private companion object {
        const val PROFILE_ID = 3L
        const val SOURCE_ID = 9L
    }
}
