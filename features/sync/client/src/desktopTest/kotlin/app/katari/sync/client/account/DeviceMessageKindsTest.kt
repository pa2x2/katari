package app.katari.sync.client.account

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.SyncingTestDevice.Companion.NOTE
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.protocol.device.DeviceDescription
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DeviceMessageKindsTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `the account's device list, as listed and as kept here, says which messages each device takes`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("Phone.db")).use { phone ->
                    SyncingTestDevice(directory.resolve("Laptop.db")).use { laptop ->
                        phone.join(server, account)
                        laptop.join(server, account)
                        laptop.sync() shouldBe SyncOutcome.SYNCED

                        val both = mapOf("Phone" to listOf(NOTE), "Laptop" to listOf(NOTE))
                        val listed = phone.account.devices().shouldBeInstanceOf<AccountCall.Done<List<AccountDevice>>>()
                        listed.value.map { it.device.description }.kinds() shouldBe both
                        phone.directory.devices().first().map { it.description }.kinds() shouldBe both
                    }
                }
            }
        }

    private fun List<DeviceDescription>.kinds() = associate { it.name to it.messageKinds }
}
