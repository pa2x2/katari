package app.katari.sync.client.resync

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import app.katari.sync.server.retention.DeletionMarkerRetention
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RestartedDownloadTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a first download that starts over for dropped deletions deletes what it took before and was deleted since`():
        Unit = runBlocking {
        TestSyncServer(directory, PAGE_SIZE).use { server ->
            val account = server.account("reader")
            val cutter = CallCutter()
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db"), cutter.client).use { b ->
                    val gone = """["category","default","Gone",1,0]"""
                    a.sql("INSERT INTO categories(name, sort, flags) VALUES ('Gone', 1, 0)")
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    val firstSync = a.connection.state().position
                    a.sql(*showRows("/later", episodes = 30, read = 30))
                    a.sync() shouldBe SyncOutcome.SYNCED

                    // Every page but the last is full, so these pages take everything of the first sync and stop
                    // well before the rest
                    cutter.pullsBeforeCut = ((firstSync + PAGE_SIZE - 1) / PAGE_SIZE).toInt()
                    b.join(server, account)
                    b.runs.lastOutcome.first { it != SyncOutcome.IDLE } shouldBe SyncOutcome.UNREACHABLE
                    b.connection.state().stage shouldBe SyncStage.DOWNLOADING
                    b.library() shouldContain gone

                    a.sql("DELETE FROM categories WHERE name = 'Gone'")
                    a.sync() shouldBe SyncOutcome.SYNCED
                    // The device downloading stays away past the time the server keeps deletions for it
                    server.dropPassedDeletionMarkers(
                        account,
                        later = DeletionMarkerRetention.DEVICE_SILENCE_LIMIT * 2,
                    ) shouldBeGreaterThan b.connection.state().position

                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.connection.state().stage shouldBe SyncStage.READY
                    b.library() shouldNotContain gone
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.library() shouldNotContain gone
                    b.library() shouldBe a.library()
                }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 3
    }
}
