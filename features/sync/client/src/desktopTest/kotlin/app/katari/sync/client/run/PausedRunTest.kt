package app.katari.sync.client.run

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.showRows
import app.katari.sync.client.status.SyncStatus
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PausedRunTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a change made while syncing is paused counts as waiting to be sent, and goes once syncing resumes`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                val cutter = CallCutter()
                SyncingTestDevice(directory.resolve("a.db"), cutter.client).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.sql(*showRows("/show", episodes = 2))
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED

                        a.preferences.paused.set(true)
                        val pushes = cutter.pushes.get()
                        a.sql("UPDATE chapters SET read = 1 WHERE url = '/show/1'")
                        a.sync() shouldBe SyncOutcome.IDLE
                        a.status.status().first().let { status ->
                            status.unsent shouldBeGreaterThan 0
                            status.unsentBecause shouldBe SyncStatus.UnsentReason.PAUSED
                        }
                        cutter.pushes.get() shouldBe pushes

                        a.preferences.paused.set(false)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.library() shouldBe a.library()
                    }
                }
            }
        }
}
