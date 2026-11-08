package app.katari.sync.client.kind.font

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.run.SyncOutcome
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ImportedFontTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a font imported on one device reaches the other, and deleting it there removes it everywhere`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED

                        val font = "font bytes".toByteArray()
                        a.local.files.importFont(profileId = 1, fileName = "Reading.ttf", bytes = font)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.local.files.fonts(profileId = 1) shouldBe listOf("Reading.ttf")
                        b.local.files.file(FileOwner.Font(1, "Reading.ttf"))?.readBytes() shouldBe font

                        b.local.files.remove(FileOwner.Font(1, "Reading.ttf"))
                        b.sync() shouldBe SyncOutcome.SYNCED
                        a.sync() shouldBe SyncOutcome.SYNCED
                        a.local.files.file(FileOwner.Font(1, "Reading.ttf")).shouldBeNull()
                        a.local.pending.count() shouldBe 0

                        server.removeUnreferencedFilesAfterGrace()
                        server.storedFiles() shouldBe emptyList()
                    }
                }
            }
        }
}
