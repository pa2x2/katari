package app.katari.sync.client.account

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import app.katari.sync.protocol.http.SyncHttp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.seconds

class DisconnectTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a device that disconnects while its server can't be reached leaves the account once it can`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                val cutter = CallCutter()
                val tokens = SyncingTestDevice.MemoryTokens()
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db"), cutter.client, tokens).use { b ->
                        a.sql(*showRows("/show", episodes = 2))
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED

                        cutter.unreachable = true
                        b.account.disconnect()
                        b.connection.state().isLinked shouldBe false
                        a.listedDevices().size shouldBe 2

                        cutter.unreachable = false
                        b.triggers.appCameToFront()
                        withTimeout(10.seconds) { b.connection.states().first { it.leftDevice == null } }
                        a.listedDevices().size shouldBe 1
                        tokens.read() shouldBe null
                    }
                }
            }
        }

    @Test
    fun `disconnecting during a first download leaves at once and ends the run for whoever waits on it`(): Unit =
        runBlocking {
            TestSyncServer(directory, pageSize = 3).use { server ->
                val account = server.account("reader")
                val cutter = CallCutter()
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db"), cutter.client).use { b ->
                        a.sql(*showRows("/show", episodes = 12, read = 12))
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED

                        // A server that took the download's first page and never answers, as a long download is
                        val pull = cutter.holdNext(SyncHttp.PULL)
                        try {
                            b.join(server, account)
                            withTimeout(10.seconds) {
                                b.connection.states().first { it.stage == SyncStage.DOWNLOADING }
                            }
                            val waiting = async { b.runs.runAndWait() }

                            withTimeout(10.seconds) { b.account.disconnect() }
                            b.connection.state().isLinked shouldBe false
                            withTimeout(10.seconds) { waiting.await() } shouldBe SyncOutcome.IDLE
                            a.listedDevices().size shouldBe 1
                        } finally {
                            pull.countDown()
                        }
                    }
                }
            }
        }

    private suspend fun SyncingTestDevice.listedDevices(): List<AccountDevice> =
        account.devices().shouldBeInstanceOf<AccountCall.Done<List<AccountDevice>>>().value
}
