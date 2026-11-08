package app.katari.sync.client.run

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InterruptedRunTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a first sync cut off while downloading carries on from the page it reached`(): Unit = runBlocking {
        TestSyncServer(directory, pageSize = 3).use { server ->
            val account = server.account("reader")
            val cutter = CallCutter()
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db"), cutter.client).use { b ->
                    a.sql(*showRows("/show", episodes = 12, read = 12))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED

                    cutter.pullsBeforeCut = 2
                    b.join(server, account)
                    b.runs.lastOutcome.first { it != SyncOutcome.IDLE } shouldBe SyncOutcome.UNREACHABLE
                    val stopped = b.connection.state()
                    stopped.stage shouldBe SyncStage.DOWNLOADING
                    (stopped.position > 0) shouldBe true

                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.connection.state().stage shouldBe SyncStage.READY
                    b.library() shouldBe a.library()
                }
            }
        }
    }

    @Test
    fun `a push whose answer was lost is sent again and stored once`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            val cutter = CallCutter()
            SyncingTestDevice(directory.resolve("a.db"), cutter.client).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show", episodes = 3))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED

                    a.sql("UPDATE chapters SET read = 1 WHERE url = '/show/2'")
                    cutter.loseNextPushAnswer = true
                    a.sync() shouldBe SyncOutcome.UNREACHABLE
                    a.local.pending.count() shouldBe 1
                    val stored = a.client().snapshot()

                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.local.pending.count() shouldBe 0
                    // The server took the change once: the retry got the first push's acknowledgement back.
                    a.client().snapshot() shouldBe stored

                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.library() shouldBe a.library()
                    b.library() shouldContain """["chapter","/show","/show/2",1,0]"""
                }
            }
        }
    }
}
