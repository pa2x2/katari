package tachiyomi.data.track

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
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
import tachiyomi.data.subscribeToList

@OptIn(ExperimentalCoroutinesApi::class)
class EntrySyncIdentityQueriesTest {

    @Test
    fun `identity projection suppresses unrelated tracking changes`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            seedEntries(driver)
            val database = database(driver)
            database.insertTrack(profileId = 2, entryId = 20, trackerId = 8, remoteId = 80, title = "Second")
            database.insertTrack(profileId = 2, entryId = 10, trackerId = 7, remoteId = 70, title = "First")
            database.insertTrack(profileId = 3, entryId = 30, trackerId = 6, remoteId = 60, title = "Other")

            val emissions = mutableListOf<List<TrackIdentityKey>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                database.entry_syncQueries.getTrackIdentityKeys(profileId = 2, ::TrackIdentityKey)
                    .subscribeToList()
                    .distinctUntilChanged()
                    .toList(emissions)
            }
            runCurrent()

            emissions.single() shouldContainExactly listOf(
                TrackIdentityKey(entryId = 10, trackerId = 7, remoteId = 70),
                TrackIdentityKey(entryId = 20, trackerId = 8, remoteId = 80),
            )

            database.insertTrack(
                profileId = 2,
                entryId = 10,
                trackerId = 7,
                remoteId = 70,
                title = "Updated",
                progress = 13.0,
            )
            database.insertTrack(profileId = 3, entryId = 30, trackerId = 6, remoteId = 61, title = "Other update")
            runCurrent()

            emissions.size shouldBe 1

            database.insertTrack(profileId = 2, entryId = 10, trackerId = 7, remoteId = 71, title = "Updated")
            runCurrent()

            emissions.size shouldBe 2
            emissions.last() shouldContainExactly listOf(
                TrackIdentityKey(entryId = 10, trackerId = 7, remoteId = 71),
                TrackIdentityKey(entryId = 20, trackerId = 8, remoteId = 80),
            )
        } finally {
            driver.close()
        }
    }

    private suspend fun Database.insertTrack(
        profileId: Long,
        entryId: Long,
        trackerId: Long,
        remoteId: Long,
        title: String,
        progress: Double = 12.0,
    ) {
        entry_syncQueries.insert(
            profileId = profileId,
            entryId = entryId,
            syncId = trackerId,
            remoteId = remoteId,
            libraryId = null,
            title = title,
            lastChapterRead = progress,
            totalChapters = 100,
            status = 1,
            score = 8.0,
            remoteUrl = "https://tracker/$remoteId",
            startDate = 1,
            finishDate = 2,
            private = false,
        )
    }

    private suspend fun seedEntries(driver: JdbcSqliteDriver) {
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
                INSERT INTO entries(_id, profile_id, source, url, title)
                VALUES
                    (10, 2, 1, '/first', 'First'),
                    (20, 2, 1, '/second', 'Second'),
                    (30, 3, 1, '/other', 'Other')
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

    private data class TrackIdentityKey(
        val entryId: Long,
        val trackerId: Long,
        val remoteId: Long,
    )
}
