package app.katari.sync.client.handoff

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.rows
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LastReadDevicesTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a chapter read on another device names it until this device reads it again`(): Unit = runBlocking {
        TestSyncServer(directory, pageSize = 500).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show"))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED

                    a.sql(
                        """
                        INSERT INTO history(entry_id, chapter_id, last_read, time_read)
                        SELECT entry_id, _id, 1000, 60000 FROM chapters WHERE url = '/show/1'
                        """,
                    )
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    val onA = a.chapterId("/show/1")
                    val onB = b.chapterId("/show/1")
                    a.lastReadDevices.otherDeviceName(onA, 1000).shouldBeNull()
                    b.lastReadDevices.otherDeviceName(onB, 1000) shouldBe "a"

                    // Read here and not synced yet: the record still holds a's reading, which is older
                    b.sql("UPDATE history SET last_read = 2000")
                    b.lastReadDevices.otherDeviceName(onB, 2000).shouldBeNull()
                }
            }
        }
    }

    private suspend fun SyncingTestDevice.chapterId(url: String): Long =
        local.rows("SELECT _id FROM chapters WHERE url = '$url'").single().toLong()
}
