package app.katari.sync.client.resync

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import app.katari.sync.server.retention.DeletionMarkerRetention
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ExpiredPositionTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `history a device doesn't sync outlasts a resync that left deletions out, and nothing is left to push`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        val history = """["history","/show/1",1000,0]"""
                        a.sql(
                            *showRows("/show", read = 1),
                            "INSERT INTO history(entry_id, chapter_id, last_read, time_read) " +
                                "SELECT entry_id, _id, 1000, 0 FROM chapters",
                            "INSERT INTO categories(name, sort, flags) VALUES ('Gone', 1, 0)",
                            "INSERT INTO categories(name, sort, flags) VALUES ('Gone later', 2, 0)",
                        )
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.choiceChanges.syncData(DataGroup.HISTORY, on = false)

                        a.sql("DELETE FROM categories WHERE name = 'Gone'")
                        a.sync() shouldBe SyncOutcome.SYNCED
                        // B stays away past the time the server keeps deletions for it
                        server.dropPassedDeletionMarkers(
                            account,
                            later = DeletionMarkerRetention.DEVICE_SILENCE_LIMIT * 2,
                        ) shouldBeGreaterThan b.connection.state().position
                        // A deletion the server still keeps, which a pull leaving deletions out doesn't bring
                        a.sql("DELETE FROM categories WHERE name = 'Gone later'")
                        a.sync() shouldBe SyncOutcome.SYNCED

                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.connection.state().stage shouldBe SyncStage.READY
                        b.local.pending.count() shouldBe 0
                        b.choiceChanges.syncData(DataGroup.HISTORY, on = true)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        a.sync() shouldBe SyncOutcome.SYNCED

                        b.library() shouldContain history
                        b.library() shouldNotContain """["category","default","Gone later",2,0]"""
                        b.library() shouldBe a.library()
                        b.local.pending.count() shouldBe 0
                    }
                }
            }
        }

    @Test
    fun `history a device doesn't sync that was deleted while it was away doesn't come back when it syncs it again`():
        Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show", episodes = 2, read = 2))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.choiceChanges.syncData(DataGroup.HISTORY, on = false)

                    // Reaches B only as a copy held for history, which it doesn't sync
                    a.sql(
                        "INSERT INTO history(entry_id, chapter_id, last_read, time_read) " +
                            "SELECT entry_id, _id, 1000, 0 FROM chapters WHERE url = '/show/2'",
                    )
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sql("DELETE FROM history")
                    a.sync() shouldBe SyncOutcome.SYNCED
                    // B stays away past the time the server keeps deletions for it
                    server.dropPassedDeletionMarkers(
                        account,
                        later = DeletionMarkerRetention.DEVICE_SILENCE_LIMIT * 2,
                    ) shouldBeGreaterThan b.connection.state().position

                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.connection.state().stage shouldBe SyncStage.READY
                    b.choiceChanges.syncData(DataGroup.HISTORY, on = true)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED

                    b.library() shouldNotContain """["history","/show/2",1000,0]"""
                    b.library() shouldBe a.library()
                }
            }
        }
    }
}
