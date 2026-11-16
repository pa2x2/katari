package app.katari.sync.client.resync

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ResyncOnMobileDataTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a resync cut short on Wi-Fi waits for it where changes stay off mobile data, and messages still come`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                val cutter = CallCutter()
                SyncingTestDevice(directory.resolve("phone.db"), cutter.client).use { phone ->
                    SyncingTestDevice(directory.resolve("desktop.db")).use { desktop ->
                        phone.sql(*showRows("/show", episodes = 3))
                        phone.join(server, account)
                        desktop.join(server, account)
                        phone.sync() shouldBe SyncOutcome.SYNCED
                        desktop.sync() shouldBe SyncOutcome.SYNCED
                        val backup = server.backUp()
                        phone.sql("UPDATE chapters SET read = 1 WHERE url = '/show/1'")
                        phone.sync() shouldBe SyncOutcome.SYNCED
                        server.restoreFrom(backup)

                        // The pull that finds the server restored gets through; the resync's own pull is cut off
                        cutter.pullsBeforeCut = 1
                        phone.sync() shouldBe SyncOutcome.UNREACHABLE
                        phone.connection.state().stage shouldBe SyncStage.RESYNC
                        phone.preferences.changesOnMobileData.set(false)
                        phone.network.onWifi(false)
                        desktop.sendNote("on mobile data", to = phone)
                        desktop.runs.awaitIdle()

                        phone.sync() shouldBe SyncOutcome.WAITING_FOR_WIFI
                        phone.connection.state().stage shouldBe SyncStage.RESYNC
                        phone.connection.state().position shouldBe 0
                        phone.notes shouldBe listOf("on mobile data")

                        phone.network.onWifi(true)
                        phone.sync() shouldBe SyncOutcome.SYNCED
                        phone.connection.state().stage shouldBe SyncStage.READY
                    }
                }
            }
        }
}
