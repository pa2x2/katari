package app.katari.sync.client.account

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DeviceNamesTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `devices of one name join numbered, and a new name reaches the account with the next sync`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                // Every test device is named after its database file, as phones of one model share theirs
                val phones = (1..3).map {
                    SyncingTestDevice(directory.resolve("$it").apply { mkdirs() }.resolve("Phone.db"))
                }
                try {
                    phones.forEach { phone ->
                        phone.join(server, account)
                        phone.sync() shouldBe SyncOutcome.SYNCED
                    }
                    phones[0].names() shouldContainExactlyInAnyOrder listOf("Phone", "Phone 2", "Phone 3")

                    phones[1].naming.rename("  Tablet ")
                    phones[1].sync() shouldBe SyncOutcome.SYNCED
                    phones[0].names() shouldContainExactlyInAnyOrder listOf("Phone", "Tablet", "Phone 3")
                } finally {
                    phones.forEach(SyncingTestDevice::close)
                }
            }
        }

    private suspend fun SyncingTestDevice.names() =
        account.devices().shouldBeInstanceOf<AccountCall.Done<List<AccountDevice>>>().value
            .map { it.device.description.name }
}
