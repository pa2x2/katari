package app.katari.sync.client.part

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.kind.devicechoice.DeviceChoicesKind
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.sync.protocol.clock.HybridTimestamp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DeviceChoicesTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a device's choices reach the others, and a device that never told them shows none`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    val older = server.linkIdleDevice(account, "Old phone")
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.choiceChanges.syncArea(SettingsArea.READERS, on = false)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED

                    val others = a.deviceChoices.others().first().associate { it.device.id to it.choices }
                    others shouldBe mapOf(
                        b.deviceId() to ChoiceSet(areasOff = setOf(SettingsArea.READERS)),
                        older to null,
                    )
                }
            }
        }
    }

    @Test
    fun `choices are told again only once they change`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                a.join(server, server.account("reader"))
                a.sync() shouldBe SyncOutcome.SYNCED
                val told = a.toldAt()
                a.sync() shouldBe SyncOutcome.SYNCED
                a.toldAt() shouldBe told

                a.choiceChanges.syncData(DataGroup.HISTORY, on = false)
                a.sync() shouldBe SyncOutcome.SYNCED
                a.toldAt() shouldNotBe told
            }
        }
    }

    /** When this device's choices were last told, as its record holds them. */
    private suspend fun SyncingTestDevice.toldAt(): HybridTimestamp {
        val key = DeviceChoicesKind.key(deviceId())
        val record = local.handler.await { sync_recordsQueries.getByKey(key).awaitAsOne() }
        return DeviceChoicesKind.decode(record.payload).choices.at
    }
}
