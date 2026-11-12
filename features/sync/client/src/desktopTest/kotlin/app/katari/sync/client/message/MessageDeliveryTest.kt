package app.katari.sync.client.message

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.protocol.message.FetchMessagesRequest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MessageDeliveryTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a message sent while its device is away is handled there once, as soon as its live channel opens`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("phone.db")).use { phone ->
                    SyncingTestDevice(directory.resolve("desktop.db")).use { desktop ->
                        phone.join(server, account)
                        desktop.join(server, account)
                        phone.sync() shouldBe SyncOutcome.SYNCED
                        desktop.sync() shouldBe SyncOutcome.SYNCED

                        phone.sendNote("continue on desktop", to = desktop)
                        phone.runs.awaitIdle()
                        desktop.triggers.start()
                        desktop.triggers.appOpened()
                        withTimeout(10.seconds) { while (desktop.notes.isEmpty()) delay(50.milliseconds) }
                        // Came along with the announcement; the run's fetch after it finds it handled
                        desktop.sync() shouldBe SyncOutcome.SYNCED

                        desktop.notes shouldContainExactly listOf("continue on desktop")
                        desktop.client().fetchMessages(FetchMessagesRequest(after = 0)).messages shouldBe emptyList()
                    }
                }
            }
        }
}
