package app.katari.sync.client.downloadrequest

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DownloadRequestDeliveryTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a request that comes before its entry waits and is carried out after the sync that brings the entry`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("phone.db")).use { phone ->
                    SyncingTestDevice(directory.resolve("desktop.db")).use { desktop ->
                        phone.join(server, account)
                        desktop.join(server, account)
                        phone.sync() shouldBe SyncOutcome.SYNCED
                        desktop.sync() shouldBe SyncOutcome.SYNCED
                        phone.sql(*showRows("/show", episodes = 3))
                        phone.sync() shouldBe SyncOutcome.SYNCED
                        val show = phone.entryId("/show")

                        phone.downloadRequestSending.requestable(listOf(show)) shouldBe setOf(show)
                        val chapters = listOf(
                            ChapterToRequest(show, "/show/2", "Episode 2"),
                            ChapterToRequest(show, "/show/3", "Episode 3"),
                        )
                        phone.downloadRequestSending.send(desktop.deviceId(), chapters) shouldBe 2
                        phone.runs.awaitIdle()
                        desktop.sync() shouldBe SyncOutcome.SYNCED
                        desktop.downloadRequestInbox.lookAgain().join()

                        desktop.downloadRequests shouldBe listOf(
                            ReceivedDownloadRequest(
                                entryId = desktop.local.entryId("/show"),
                                chapters = listOf(
                                    RequestedChapter("/show/2", "Episode 2"),
                                    RequestedChapter("/show/3", "Episode 3"),
                                ),
                                fromName = "phone",
                            ),
                        )
                        desktop.sync() shouldBe SyncOutcome.SYNCED
                        desktop.downloadRequestInbox.lookAgain().join()
                        desktop.downloadRequests.size shouldBe 1
                    }
                }
            }
        }

    @Test
    fun `an entry that doesn't sync can't be asked about`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("phone.db")).use { phone ->
                phone.join(server, account)
                phone.sync() shouldBe SyncOutcome.SYNCED
                // Browsed, not added to the library, nothing read
                phone.sql(*showRows("/show").map { it.replace("'Show', 1,", "'Show', 0,") }.toTypedArray())
                phone.sync() shouldBe SyncOutcome.SYNCED

                phone.downloadRequestSending.requestable(listOf(phone.entryId("/show"))).shouldBeEmpty()
            }
        }
    }
}
