package app.katari.sync.client.run

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.showRows
import app.katari.sync.client.status.SyncStatus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FailedRunTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a run that fails on a change it can't apply says so, not that the server can't be reached`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                val cutter = CallCutter()
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db"), cutter.client).use { b ->
                        a.sql(*showRows("/show", episodes = 3, read = 1))
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        val lastSynced = b.connection.state().lastSyncedAt

                        cutter.pullsBeforeCut = 0
                        b.sync() shouldBe SyncOutcome.UNREACHABLE
                        // B can no longer store how far it pulled, as a bug there would leave it; a record that fails
                        // to apply would only be held aside
                        b.sql(
                            "CREATE TRIGGER broken BEFORE UPDATE OF cursor ON sync_state BEGIN SELECT RAISE(ABORT, 'broken'); END",
                        )
                        a.sql("UPDATE chapters SET read = 1 WHERE url = '/show/2'")
                        a.sync() shouldBe SyncOutcome.SYNCED

                        b.sync() shouldBe SyncOutcome.FAILED
                        b.runs.runAndWait(SyncRequest(onlyIfChanged = true)) shouldBe SyncOutcome.IDLE

                        val status = b.status.status().first()
                        status.activity shouldBe SyncStatus.Activity.Failed(lastSynced)
                        b.connection.state().unreachableSince shouldBe null
                    }
                }
            }
        }
}
