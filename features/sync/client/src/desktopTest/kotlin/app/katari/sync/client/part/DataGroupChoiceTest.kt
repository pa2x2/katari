package app.katari.sync.client.part

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.rows
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataGroupChoiceTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `history a device stops syncing neither leaves nor arrives, and once synced again the newest change wins`():
        Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(
                        *showRows("/show", read = 1),
                        "INSERT INTO history(entry_id, chapter_id, last_read, time_read) " +
                            "SELECT entry_id, _id, 1000, 0 FROM chapters",
                    )
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.choiceChanges.syncData(DataGroup.HISTORY, on = false)

                    a.sql("UPDATE history SET last_read = 2000")
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.lastRead() shouldBe 1000

                    // Made after A's, so it wins once B syncs history again
                    b.sql("UPDATE history SET last_read = 3000")
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.lastRead() shouldBe 2000

                    b.choiceChanges.syncData(DataGroup.HISTORY, on = true)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.lastRead() shouldBe 3000
                    b.lastRead() shouldBe 3000
                }
            }
        }
    }

    private suspend fun SyncingTestDevice.lastRead(): Long =
        local.rows("SELECT CAST(last_read AS TEXT) FROM history").single().toLong()
}
