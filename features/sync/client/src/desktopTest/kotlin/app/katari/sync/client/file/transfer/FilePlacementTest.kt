package app.katari.sync.client.file.transfer

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.file.local.contentOf
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import app.katari.sync.client.status.SyncStatus
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FilePlacementTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a custom cover follows its record byte for byte, the newest replacement wins, and only it stays stored`():
        Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show"))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED

                    // B is on mobile data, which files don't use: the record arrives, the file waits
                    b.network.onWifi(false)
                    val first = "first cover".toByteArray()
                    a.local.setCustomCover("/show", first)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.local.customCover("/show").shouldBeNull()
                    b.status.status().first().files shouldBe
                        SyncStatus.Files(
                            toUpload = 0,
                            refusedAsTooLarge = 0,
                            toDownload = 1,
                            missingOnServer = 0,
                            waitingForWifi = true,
                        )

                    b.network.onWifi(true)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.local.customCover("/show") shouldBe first
                    b.status.status().first().files shouldBe SyncStatus.Files.NONE

                    // Replaced on B, then on A before A heard of B's
                    b.local.setCustomCover("/show", "second cover".toByteArray())
                    b.sync() shouldBe SyncOutcome.SYNCED
                    val third = "third cover".toByteArray()
                    a.local.setCustomCover("/show", third)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED

                    a.local.customCover("/show") shouldBe third
                    b.local.customCover("/show") shouldBe third
                    // Applying the other device's cover was no edit of the device applying it
                    a.local.pending.count() shouldBe 0
                    b.local.pending.count() shouldBe 0

                    server.removeUnreferencedFilesAfterGrace()
                    server.storedFiles() shouldContainExactly listOf(sha256Of(third))
                }
            }
        }
    }

    private fun sha256Of(bytes: ByteArray): String =
        File.createTempFile("content", null, directory).apply { writeBytes(bytes) }.let(::contentOf).sha256
}
