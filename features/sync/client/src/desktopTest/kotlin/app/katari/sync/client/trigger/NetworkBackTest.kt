package app.katari.sync.client.trigger

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NetworkBackTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `what waited for a network goes when one comes back, messages on mobile data and changes on Wi-Fi`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("phone.db")).use { phone ->
                    SyncingTestDevice(directory.resolve("desktop.db")).use { desktop ->
                        phone.sql(*showRows("/show"))
                        phone.join(server, account)
                        desktop.join(server, account)
                        phone.sync() shouldBe SyncOutcome.SYNCED
                        desktop.sync() shouldBe SyncOutcome.SYNCED
                        phone.preferences.changesOnMobileData.set(false)
                        // No live channel: only the network coming back can start the runs below
                        phone.triggers.start()

                        phone.network.offline()
                        phone.sql("UPDATE chapters SET read = 1 WHERE url = '/show/1'")
                        phone.local.driver.notifyListeners("chapters")
                        phone.sendNote("sent offline", to = desktop)
                        phone.runs.awaitIdle()
                        phone.runs.lastOutcome.value shouldBe SyncOutcome.OFFLINE

                        phone.network.onWifi(false)
                        withTimeout(10.seconds) {
                            while (phone.runs.lastOutcome.value != SyncOutcome.WAITING_FOR_WIFI) delay(50.milliseconds)
                        }
                        desktop.sync() shouldBe SyncOutcome.SYNCED
                        desktop.notes shouldBe listOf("sent offline")
                        phone.local.pending.count() shouldBeGreaterThan 0

                        phone.network.onWifi(true)
                        withTimeout(10.seconds) { while (phone.local.pending.count() > 0) delay(50.milliseconds) }
                    }
                }
            }
        }
}
