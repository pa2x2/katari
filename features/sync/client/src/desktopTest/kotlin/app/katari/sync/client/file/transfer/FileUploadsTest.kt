package app.katari.sync.client.file.transfer

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileUploadsTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a cover waiting for Wi-Fi holds back only the record naming it`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/one"), *showRows("/two"))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED

                    a.network.onWifi(false)
                    val cover = "cover".toByteArray()
                    a.local.setCustomCover("/one", cover)
                    a.sql("UPDATE chapters SET read = 1 WHERE url = '/two/1'")
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.local.pending.count() shouldBe 1
                    a.status.status().first().files.waitingForWifi shouldBe true

                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.library() shouldContain """["chapter","/two","/two/1",1,0]"""
                    b.local.customCover("/one").shouldBeNull()

                    a.network.onWifi(true)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.local.pending.count() shouldBe 0
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.local.customCover("/one") shouldBe cover
                }
            }
        }
    }

    @Test
    fun `a cover the server refuses as too large holds back no record and isn't uploaded again on every sync`(): Unit =
        runBlocking {
            TestSyncServer(directory, maxFileBytes = 1024).use { server ->
                val account = server.account("reader")
                val cutter = CallCutter()
                SyncingTestDevice(directory.resolve("a.db"), cutter.client).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.sql(*showRows("/show"))
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED

                        a.local.setCustomCover("/show", ByteArray(4096) { it.toByte() })
                        a.sync() shouldBe SyncOutcome.SYNCED
                        a.local.pending.count() shouldBe 0
                        a.status.status().first().files.refusedAsTooLarge shouldBe 1
                        a.sync() shouldBe SyncOutcome.SYNCED
                        cutter.uploads.get() shouldBe 1

                        // The record naming the cover came, without it
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.status.status().first().files.missingOnServer shouldBe 1
                    }
                }
            }
        }

    @Test
    fun `an upload cut off midway is sent again whole and stored once`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            val cutter = CallCutter()
            SyncingTestDevice(directory.resolve("a.db"), cutter.client).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show"))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED

                    val cover = ByteArray(256 * 1024) { (it * 31).toByte() }
                    a.local.setCustomCover("/show", cover)
                    cutter.cutNextUpload = true
                    a.sync() shouldBe SyncOutcome.UNREACHABLE
                    server.storedFiles() shouldBe emptyList()

                    a.sync() shouldBe SyncOutcome.SYNCED
                    server.storedFiles() shouldHaveSize 1
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.local.customCover("/show") shouldBe cover
                }
            }
        }
    }
}
