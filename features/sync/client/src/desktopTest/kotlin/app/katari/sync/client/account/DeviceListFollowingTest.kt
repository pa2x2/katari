package app.katari.sync.client.account

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.protocol.device.LinkedDevice
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.seconds

class DeviceListFollowingTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a device listening live keeps the devices that join or rename themselves without listing them itself`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("Phone.db")).use { phone ->
                    SyncingTestDevice(directory.resolve("Laptop.db")).use { laptop ->
                        phone.join(server, account)
                        phone.sync() shouldBe SyncOutcome.SYNCED
                        phone.triggers.start()
                        phone.triggers.appOpened()
                        phone.awaitListed { names -> names == setOf("Phone") }

                        laptop.join(server, account)
                        phone.awaitListed { names -> names == setOf("Phone", "Laptop") }

                        laptop.account.renameThisDevice("Work laptop")
                        phone.awaitListed { names -> names == setOf("Phone", "Work laptop") }
                    }
                }
            }
        }

    private suspend fun SyncingTestDevice.awaitListed(until: (Set<String>) -> Boolean) {
        withTimeout(10.seconds) {
            directory.devices().first { devices -> until(devices.names()) }
        }
    }

    private fun List<LinkedDevice>.names() = mapTo(mutableSetOf()) { it.description.name }
}
