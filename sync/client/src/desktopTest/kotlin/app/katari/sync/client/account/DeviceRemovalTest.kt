package app.katari.sync.client.account

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DeviceRemovalTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a device removed from its account stops syncing and keeps its data`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show", episodes = 2))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    val library = a.library()

                    val devices = b.account.devices().shouldBeInstanceOf<AccountCall.Done<List<AccountDevice>>>().value
                    b.account.removeDevice(devices.single { !it.isThisDevice }.device.id) shouldBe
                        AccountCall.Done(true)

                    a.sync() shouldBe SyncOutcome.IDLE
                    a.connection.state().let { state ->
                        state.isLinked shouldBe false
                        state.stopped shouldBe SyncStop.REMOVED
                    }
                    a.library() shouldBe library
                    a.sql("UPDATE chapters SET read = 1")
                    a.local.outboxKinds().shouldBeEmpty()
                    a.sync() shouldBe SyncOutcome.IDLE
                }
            }
        }
    }
}
